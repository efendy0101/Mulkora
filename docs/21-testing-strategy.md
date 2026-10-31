# Mülkora — Test Stratejisi

**Sürüm:** 1.0  
**Tarih:** 2026-07-19  
**Hazırlayan:** Claude Fable — Analiz Aşaması  
**Durum:** Taslak

---

## 1. Test Piramidi

```
         ┌──────────────┐
         │  E2E Tests   │  ← Az sayıda, yavaş, pahalı
         │  (UI Tests)  │    Firebase Test Lab
         ├──────────────┤
         │ Integration  │  ← Orta sayıda
         │   Tests      │    Emulator Suite
         ├──────────────┤
         │  Unit Tests  │  ← Çok sayıda, hızlı, ucuz
         │              │    JUnit5 + Coroutines Test
         └──────────────┘
```

**Hedef Kapsam (Coverage):**
- Unit Tests: %80+
- Integration Tests: Kritik iş akışları
- E2E Tests: Happy path + temel güvenlik senaryoları

---

## 2. Android Unit Test Yapısı

### Araçlar

| Araç | Amaç |
|---|---|
| JUnit 5 | Test çerçevesi |
| MockK | Mock kütüphanesi (Kotlin için) |
| Turbine | StateFlow/Flow test |
| kotlinx-coroutines-test | Coroutines test araçları |
| Robolectric | Android API'leri gerektiren unit testler |
| Truth | Assertion kütüphanesi |

### ViewModel Testi

```kotlin
// PostDetailViewModelTest.kt
class PostDetailViewModelTest {

    private val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val rule = mainDispatcherRule

    private val fakePostRepository = FakePostRepository()
    private lateinit var viewModel: PostDetailViewModel

    @BeforeEach
    fun setup() {
        viewModel = PostDetailViewModel(
            postRepository = fakePostRepository,
            savedStateHandle = SavedStateHandle(mapOf("postId" to "test_post_id"))
        )
    }

    @Test
    fun `when post is loaded, ui state should be Success`() = runTest {
        fakePostRepository.setPost(TestDataFactory.createPost())

        viewModel.uiState.test {
            val item = awaitItem()
            assertThat(item).isInstanceOf(PostDetailUiState.Success::class.java)
        }
    }

    @Test
    fun `when post not found, ui state should be Error`() = runTest {
        fakePostRepository.setError(NotFoundException())

        viewModel.uiState.test {
            skipItems(1) // Loading state
            val item = awaitItem()
            assertThat(item).isInstanceOf(PostDetailUiState.Error::class.java)
        }
    }
}
```

### UseCase Testi

```kotlin
// ToggleReactionUseCaseTest.kt
class ToggleReactionUseCaseTest {

    private val fakeReactionRepository = FakeReactionRepository()
    private val useCase = ToggleReactionUseCase(fakeReactionRepository)

    @Test
    fun `toggle like on unliked post should add like`() = runTest {
        fakeReactionRepository.setCurrentReaction(null)

        val result = useCase("post_id", ReactionType.LIKE)

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat(fakeReactionRepository.currentReaction).isEqualTo(ReactionType.LIKE)
    }

    @Test
    fun `toggle like on liked post should remove like`() = runTest {
        fakeReactionRepository.setCurrentReaction(ReactionType.LIKE)

        val result = useCase("post_id", ReactionType.LIKE)

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat(fakeReactionRepository.currentReaction).isNull()
    }
}
```

### State Machine Testi

```kotlin
// PostStateMachineTest.kt
class PostStateMachineTest {

    @Test
    fun `draft to pending is valid`() {
        assertThat(PostStateMachine.isValidTransition(PostStatus.DRAFT, PostStatus.PENDING)).isTrue()
    }

    @Test
    fun `approved to draft is invalid`() {
        assertThat(PostStateMachine.isValidTransition(PostStatus.APPROVED, PostStatus.DRAFT)).isFalse()
    }

    @Test
    fun `deleted to any status is invalid`() {
        PostStatus.values().forEach { targetStatus ->
            if (targetStatus != PostStatus.DELETED) {
                assertThat(PostStateMachine.isValidTransition(PostStatus.DELETED, targetStatus)).isFalse()
            }
        }
    }
}
```

---

## 3. Cloud Functions Unit Testleri

### Araçlar

| Araç | Amaç |
|---|---|
| Jest | Test çerçevesi |
| Firebase Emulator Suite | Gerçek Firestore/Auth |
| `@firebase/rules-unit-testing` | Security Rules testi |
| `ts-jest` | TypeScript desteği |

### ContentFilter Testi

```typescript
// contentFilter.test.ts
describe('ContentFilter', () => {
  it('should detect TC kimlik number', async () => {
    const result = await filterContent('TC kimliğim 12345678901', db);
    expect(result.riskLevel).toBe('high');
    expect(result.details.personalDataTypes).toContain('tc_id');
  });

  it('should elevate risk with multiple low-risk terms', async () => {
    // 3+ low risk term should elevate to medium
    const result = await filterContent('test düşük risk kelime1 kelime2 kelime3', db);
    expect(result.riskLevel).toBe('medium');
  });

  it('should not flag clean content', async () => {
    const result = await filterContent('Ev sahibim ile kiracı sorunum hakkında', db);
    expect(result.riskLevel).toBe('none');
  });
});
```

### Cloud Function Entegrasyon Testi

```typescript
// submitPost.test.ts
describe('submitPost', () => {
  let testEnv: RulesTestEnvironment;

  beforeAll(async () => {
    testEnv = await initializeTestEnvironment({
      projectId: 'mulkora-test',
      firestore: { host: 'localhost', port: 8080 }
    });
  });

  it('should reject submission from banned user', async () => {
    const context = createAuthenticatedContext('banned_user_id', { isBanned: true });
    
    await expect(
      wrapped_submitPost({ postType: 'problem', title: 'Test' }, context)
    ).rejects.toThrow('permission-denied');
  });

  it('should create post with pending status for clean content', async () => {
    const context = createAuthenticatedContext('user_id', { role: 'tenant' });
    const result = await wrapped_submitPost({
      postType: 'problem',
      category: 'rent_increase',
      title: 'Kira artış sorunu',
      content: 'Ev sahibim fazla artış talep ediyor.',
      city: 'İstanbul',
      isAnonymous: false,
      commentsAllowed: true
    }, context);
    
    expect(result.success).toBe(true);
    
    const postDoc = await testEnv.firestore()
      .collection('posts')
      .doc(result.postId)
      .get();
    
    expect(postDoc.data()?.status).toBe('pending');
  });
});
```

---

## 4. Firestore Security Rules Testleri

```typescript
// firestore.rules.test.ts
describe('Firestore Security Rules', () => {
  
  describe('posts collection', () => {
    
    it('TEST-RULES-001: unauthenticated user cannot read posts', async () => {
      const unauthedDb = testEnv.unauthenticatedContext().firestore();
      
      await expect(
        unauthedDb.collection('posts').doc('test_post').get()
      ).rejects.toThrow();
    });

    it('TEST-RULES-002: approved post is readable by authenticated user', async () => {
      await testEnv.withSecurityRulesDisabled(async (ctx) => {
        await ctx.firestore().collection('posts').doc('approved_post').set({
          status: 'approved',
          authorId: 'other_user'
        });
      });
      
      const authedDb = testEnv.authenticatedContext('any_user').firestore();
      const doc = await authedDb.collection('posts').doc('approved_post').get();
      expect(doc.exists).toBe(true);
    });

    it('TEST-RULES-005: user cannot update likeCount directly', async () => {
      await testEnv.withSecurityRulesDisabled(async (ctx) => {
        await ctx.firestore().collection('posts').doc('my_post').set({
          status: 'draft',
          authorId: 'user1',
          likeCount: 0
        });
      });
      
      const authedDb = testEnv.authenticatedContext('user1').firestore();
      
      await expect(
        authedDb.collection('posts').doc('my_post').update({ likeCount: 100 })
      ).rejects.toThrow();
    });
  });

  describe('auditLogs collection', () => {
    
    it('TEST-RULES-008: no client can write audit logs', async () => {
      const adminDb = testEnv.authenticatedContext('admin', { role: 'admin' }).firestore();
      
      await expect(
        adminDb.collection('auditLogs').add({ test: 'data' })
      ).rejects.toThrow();
    });

    it('TEST-RULES-009: only admin can read audit logs', async () => {
      const regularDb = testEnv.authenticatedContext('user1', { role: 'tenant' }).firestore();
      
      await expect(
        regularDb.collection('auditLogs').get()
      ).rejects.toThrow();
    });
  });
});
```

---

## 5. E2E (UI) Test Yapısı

### Araçlar

| Araç | Amaç |
|---|---|
| Firebase Test Lab | Fiziksel cihazlarda test |
| Espresso | UI etkileşimleri |
| UIAutomator | Sistem diyalogları |
| Hilt Testing | Dependency injection test |

### E2E Test Senaryoları

```kotlin
// RegisterFlowE2ETest.kt
@HiltAndroidTest
class RegisterFlowE2ETest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun complete_registration_flow_shows_feed_screen() {
        // Karşılama ekranı → Kayıt Ol
        onView(withText("Kayıt Ol")).perform(click())
        
        // Form doldurma
        onView(withId(R.id.displayNameField)).perform(typeText("Test Kullanıcı"))
        onView(withId(R.id.usernameField)).perform(typeText("test_kullanici_123"))
        onView(withId(R.id.emailField)).perform(typeText("test@example.com"))
        onView(withId(R.id.passwordField)).perform(typeText("Test1234!"))
        
        // Rol seçimi
        onView(withId(R.id.tenantRadio)).perform(click())
        
        // Onay kutucukları
        onView(withId(R.id.termsCheckbox)).perform(click())
        onView(withId(R.id.kvkkCheckbox)).perform(click())
        onView(withId(R.id.ageCheckbox)).perform(click())
        
        // Kayıt butonu
        onView(withId(R.id.registerButton)).perform(click())
        
        // E-posta doğrulama ekranı görünmeli
        onView(withText("Doğrulama e-postası gönderildi"))
            .check(matches(isDisplayed()))
    }
}
```

---

## 6. Test Veri Fabrikası

```kotlin
// TestDataFactory.kt
object TestDataFactory {

    fun createPost(
        postId: String = "test_post_id",
        status: PostStatus = PostStatus.APPROVED,
        authorId: String = "test_author_id"
    ) = Post(
        postId = postId,
        authorId = authorId,
        authorUsername = "test_user",
        authorDisplayName = "Test User",
        authorAvatarUrl = null,
        authorRole = "tenant",
        isAnonymous = false,
        postType = "problem",
        category = "rent_increase",
        title = "Test Gönderi Başlığı",
        content = "Test gönderi içeriği.",
        imageUrl = null,
        city = "İstanbul",
        commentsAllowed = true,
        status = status,
        likeCount = 10,
        dislikeCount = 1,
        commentCount = 5,
        averageRating = 4.0,
        createdAt = Timestamp.now()
    )

    fun createUser(
        uid: String = "test_uid",
        role: String = "tenant",
        isBanned: Boolean = false
    ) = User(
        uid = uid,
        username = "test_user",
        displayName = "Test User",
        email = "test@example.com",
        role = role,
        city = "İstanbul",
        isBanned = isBanned,
        adsRemoved = false
    )
    
    fun createReference(
        referenceId: String = "test_ref_id",
        authorId: String = "author_uid",
        targetId: String = "target_uid",
        status: String = "approved"
    ) = Reference(
        referenceId = referenceId,
        authorId = authorId,
        targetId = targetId,
        relationshipType = "previous_landlord",
        overallRating = 4.0,
        status = status
    )
}
```

---

## 7. Fake Repository'ler

```kotlin
// FakePostRepository.kt
class FakePostRepository : PostRepository {
    
    private var postResult: Result<Post>? = null
    
    fun setPost(post: Post) {
        postResult = Result.Success(post)
    }
    
    fun setError(error: Throwable) {
        postResult = Result.Error(error)
    }
    
    override suspend fun getPost(postId: String): Result<Post> {
        return postResult ?: Result.Error(Exception("No test data set"))
    }
    
    override suspend fun submitPost(request: SubmitPostRequest): Result<String> {
        return Result.Success("test_post_id")
    }
    
    // ... diğer metodlar
}
```

---

## 8. CI/CD Test Pipeline

```yaml
# .github/workflows/test.yml (örnek — gerçek dosya projeye eklenir)
name: Test Pipeline

on: [push, pull_request]

jobs:
  unit-tests-android:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run Unit Tests
        run: ./gradlew testDebugUnitTest
      - name: Upload Coverage
        uses: codecov/codecov-action@v3

  unit-tests-functions:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Install Functions Dependencies
        run: cd functions && npm install
      - name: Run Functions Tests with Emulator
        run: npm run test:emulator

  rules-tests:
    runs-on: ubuntu-latest
    steps:
      - name: Run Security Rules Tests
        run: npm run test:rules
```

---

## 9. Test Yoğunlaştırılacak Alan Öncelikleri

| Öncelik | Alan | Gerekçe |
|---|---|---|
| Kritik | Security Rules | Güvenlik ihlali riski |
| Kritik | Cloud Functions auth/rol kontrolleri | Yetkisiz erişim riski |
| Kritik | Content filter | Zararlı içerik yayımlama riski |
| Kritik | Billing — satın alma doğrulaması | Replay attack riski |
| Yüksek | State machine geçişleri | Veri tutarsızlığı riski |
| Yüksek | Transaction operasyonları (likeCount vb.) | Race condition riski |
| Yüksek | Account deletion (KVKK) | Hukuki uyum |
| Orta | UI akışları (E2E) | Kullanıcı deneyimi |
| Orta | FCM token yönetimi | Bildirim güvenilirliği |
| Düşük | Analytics olayları | İş metrikleri |

---

*Sonraki doküman: 22-development-phases.md*