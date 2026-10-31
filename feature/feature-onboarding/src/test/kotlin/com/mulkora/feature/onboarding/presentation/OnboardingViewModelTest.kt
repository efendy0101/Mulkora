package com.mulkora.feature.onboarding.presentation

import com.mulkora.core.database.datastore.UserPreferencesDataStore
import com.mulkora.core.testing.MainDispatcherRule
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * OnboardingViewModel birim testleri.
 *
 * CLAUDE.md §20: Onboarding durumu DataStore'da kalıcı saklanır.
 * docs/21-testing-strategy.md — UT-ONBOARDING-001 ile UT-ONBOARDING-003.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var userPreferencesDataStore: UserPreferencesDataStore
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        userPreferencesDataStore = mockk()
        viewModel = OnboardingViewModel(userPreferencesDataStore)
    }

    // ─── UT-ONBOARDING-001: Tamamlama DataStore'a yazılır ────────────────────

    @Test
    fun `markOnboardingCompleted calls dataStore setOnboardingCompleted with true`() = runTest {
        coJustRun { userPreferencesDataStore.setOnboardingCompleted(true) }

        viewModel.markOnboardingCompleted()

        coVerify(exactly = 1) { userPreferencesDataStore.setOnboardingCompleted(true) }
    }

    // ─── UT-ONBOARDING-002: Tekrar çağrı tekrar yazılır ─────────────────────

    @Test
    fun `markOnboardingCompleted called twice writes to dataStore twice`() = runTest {
        coJustRun { userPreferencesDataStore.setOnboardingCompleted(true) }

        viewModel.markOnboardingCompleted()
        viewModel.markOnboardingCompleted()

        coVerify(exactly = 2) { userPreferencesDataStore.setOnboardingCompleted(true) }
    }

    // ─── UT-ONBOARDING-003: false değer yazılmaz ─────────────────────────────

    @Test
    fun `markOnboardingCompleted never calls setOnboardingCompleted with false`() = runTest {
        coJustRun { userPreferencesDataStore.setOnboardingCompleted(any()) }

        viewModel.markOnboardingCompleted()

        coVerify(exactly = 0) { userPreferencesDataStore.setOnboardingCompleted(false) }
    }
}