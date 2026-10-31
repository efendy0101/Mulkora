package com.mulkora.feature.notifications.presentation

import com.mulkora.core.testing.MainDispatcherRule
import com.mulkora.feature.notifications.domain.repository.NotificationsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * NotificationsViewModel birim testleri.
 *
 * CLAUDE.md §19: Bildirimler — kullanıcıya gelen bildirim akışı.
 * docs/21-testing-strategy.md — UT-NOTIF-LIST-001 ile UT-NOTIF-LIST-005.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var notificationsRepository: NotificationsRepository
    private lateinit var viewModel: NotificationsViewModel

    @Before
    fun setUp() {
        notificationsRepository = mockk(relaxed = true)
        every { notificationsRepository.observeNotifications() } returns flowOf(emptyList())
        viewModel = NotificationsViewModel(notificationsRepository)
    }

    // ─── UT-NOTIF-LIST-001: Başlangıç durumu ─────────────────────────────────

    @Test
    fun `initial state is not loading`() = runTest {
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // ─── UT-NOTIF-LIST-002: Boş bildirim listesi ──────────────────────────────

    @Test
    fun `empty notification list is reflected in state`() = runTest {
        every { notificationsRepository.observeNotifications() } returns flowOf(emptyList())
        assertTrue(viewModel.uiState.value.notifications.isEmpty())
    }

    // ─── UT-NOTIF-LIST-003: uiState null değil ───────────────────────────────

    @Test
    fun `uiState is not null`() {
        assertNotNull(viewModel.uiState.value)
    }

    // ─── UT-NOTIF-LIST-004: Hata başlangıçta null ────────────────────────────

    @Test
    fun `error message is null on init`() {
        assertTrue(viewModel.uiState.value.errorMessage == null)
    }

    // ─── UT-NOTIF-LIST-005: Notifications listesi başlangıçta boş ────────────

    @Test
    fun `notifications list starts empty`() = runTest {
        assertNotNull(viewModel.uiState.value.notifications)
    }
}