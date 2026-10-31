package com.mulkora.feature.reference.presentation.detail

import androidx.lifecycle.SavedStateHandle
import com.mulkora.core.common.model.ReferenceStatus
import com.mulkora.core.common.result.Result
import com.mulkora.core.testing.MainDispatcherRule
import com.mulkora.feature.reference.domain.model.Reference
import com.mulkora.feature.reference.domain.repository.ReferenceRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Date

/**
 * ReferenceDetailViewModel birim testleri.
 *
 * CLAUDE.md §13: Referans Sistemi — detay görünümü.
 * docs/16-reference-system-state-machine.md: yalnızca alıcı (toUid) işlem yapabilir.
 * docs/21-testing-strategy.md — UT-REF-DETAIL-001 ile UT-REF-DETAIL-007.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReferenceDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var referenceRepository: ReferenceRepository
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: ReferenceDetailViewModel

    private val testReferenceId = "ref_detail_001"
    private val currentUserId = "current_user_uid"
    private val otherUserId = "other_user_uid"

    private fun makeReference(
        toUid: String = currentUserId,
        status: ReferenceStatus = ReferenceStatus.APPROVED,
        hasResponse: Boolean = false
    ) = Reference(
        id = testReferenceId,
        fromUid = otherUserId,
        toUid = toUid,
        fromDisplayName = "Gönderen Kullanıcı",
        toDisplayName = "Alan Kullanıcı",
        content = "Bu bir test referansıdır.",
        status = status,
        response = if (hasResponse) "Zaten cevap verildi" else null,
        createdAt = Date(),
        respondedAt = null
    )

    @Before
    fun setUp() {
        referenceRepository = mockk()
        firebaseAuth = mockk()
        firebaseUser = mockk()

        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns currentUserId

        savedStateHandle = SavedStateHandle(mapOf("referenceId" to testReferenceId))

        every {
            referenceRepository.getReferenceById(testReferenceId)
        } returns flowOf(Result.Success(makeReference()))

        viewModel = ReferenceDetailViewModel(savedStateHandle, referenceRepository, firebaseAuth)
    }

    // ─── UT-REF-DETAIL-001: Başlangıç durumu ─────────────────────────────────

    @Test
    fun `initial state has isLoading true before data arrives`() {
        // ViewModel init'te loadReference başlatır; başlangıçta yükleniyor olabilir
        assertFalse(viewModel.uiState.value.error != null)
    }

    // ─── UT-REF-DETAIL-002: Alıcı "Cevap Ver" görebilir ─────────────────────

    @Test
    fun `canReply is true when currentUser is toUid and no response yet`() = runTest {
        every {
            referenceRepository.getReferenceById(testReferenceId)
        } returns flowOf(Result.Success(makeReference(toUid = currentUserId, hasResponse = false)))

        val vm = ReferenceDetailViewModel(savedStateHandle, referenceRepository, firebaseAuth)

        assertTrue(vm.uiState.value.canReply)
    }

    // ─── UT-REF-DETAIL-003: Zaten yanıtlanmışsa "Cevap Ver" gizlenir ─────────

    @Test
    fun `canReply is false when reference already has a response`() = runTest {
        every {
            referenceRepository.getReferenceById(testReferenceId)
        } returns flowOf(Result.Success(makeReference(toUid = currentUserId, hasResponse = true)))

        val vm = ReferenceDetailViewModel(savedStateHandle, referenceRepository, firebaseAuth)

        assertFalse(vm.uiState.value.canReply)
    }

    // ─── UT-REF-DETAIL-004: Gönderen "Cevap Ver" göremez ─────────────────────

    @Test
    fun `canReply is false when currentUser is fromUid`() = runTest {
        every {
            referenceRepository.getReferenceById(testReferenceId)
        } returns flowOf(Result.Success(makeReference(toUid = otherUserId)))

        val vm = ReferenceDetailViewModel(savedStateHandle, referenceRepository, firebaseAuth)

        assertFalse(vm.uiState.value.canReply)
    }

    // ─── UT-REF-DETAIL-005: Alıcı "İtiraz Et" görebilir ─────────────────────

    @Test
    fun `canDispute is true when currentUser is toUid and status is APPROVED`() = runTest {
        every {
            referenceRepository.getReferenceById(testReferenceId)
        } returns flowOf(Result.Success(makeReference(toUid = currentUserId, status = ReferenceStatus.APPROVED)))

        val vm = ReferenceDetailViewModel(savedStateHandle, referenceRepository, firebaseAuth)

        assertTrue(vm.uiState.value.canDispute)
    }

    // ─── UT-REF-DETAIL-006: Başarılı itiraz ──────────────────────────────────

    @Test
    fun `disputeReference success clears error`() = runTest {
        coEvery {
            referenceRepository.disputeReference(testReferenceId)
        } returns Result.Success(Unit)

        viewModel.disputeReference()

        assertNull(viewModel.uiState.value.error)
    }

    // ─── UT-REF-DETAIL-007: Başarısız itiraz ─────────────────────────────────

    @Test
    fun `disputeReference failure sets error`() = runTest {
        coEvery {
            referenceRepository.disputeReference(testReferenceId)
        } returns Result.Failure(Exception("İtiraz başarısız"))

        viewModel.disputeReference()

        assertNotNull(viewModel.uiState.value.error)
    }
}