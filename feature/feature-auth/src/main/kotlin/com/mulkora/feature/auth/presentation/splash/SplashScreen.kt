package com.mulkora.feature.auth.presentation.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Splash (açılış) ekranı.
 * CLAUDE.md Bölüm 4: Firebase Auth oturumu kontrol edilerek kullanıcı yönlendirilir.
 *
 * Navigasyon sırası (SplashViewModel'den):
 *   1. Onboarding henüz görülmediyse → Onboarding
 *   2. Oturum varsa → Feed
 *   3. Oturum yoksa → Login
 */
@Composable
fun SplashScreen(
    onNavigateToFeed: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToOnboarding: () -> Unit = {},
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val navigationTarget by viewModel.navigationTarget.collectAsStateWithLifecycle()

    LaunchedEffect(navigationTarget) {
        when (navigationTarget) {
            SplashNavigationTarget.Feed -> onNavigateToFeed()
            SplashNavigationTarget.Login -> onNavigateToLogin()
            SplashNavigationTarget.Onboarding -> onNavigateToOnboarding()
            null -> Unit
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Mülkora",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Kiracı ve ev sahibinin ortak zemini.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator()
        }
    }
}