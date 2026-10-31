package com.mulkora.feature.auth.presentation.roleselection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SupervisedUserCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mulkora.core.common.model.UserRole

/**
 * Rol seçim ekranı.
 * CLAUDE.md §3: Kullanıcı Tenant, Landlord veya Both rolünden birini seçer.
 * CLAUDE.md §4: Moderatör/admin rolü bu ekranda seçilemez.
 */
@Composable
fun RoleSelectionScreen(
    onContinue: () -> Unit,
    viewModel: RoleSelectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Mülkora'da Kimsiniz?",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Platformu nasıl kullanacağınızı belirtin. Bu seçimi daha sonra değiştirebilirsiniz.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            viewModel.selectableRoles.forEach { role ->
                RoleCard(
                    role = role,
                    isSelected = uiState.selectedRole == role,
                    onClick = { viewModel.onRoleSelected(role) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            uiState.error?.let { errorMsg ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMsg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.onContinueClicked(onContinue) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.selectedRole != null && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Devam Et")
                }
            }
        }
    }
}

@Composable
private fun RoleCard(
    role: UserRole,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val (icon, description) = when (role) {
        UserRole.TENANT -> Pair(
            Icons.Default.Home,
            "Bir ev veya dairede kiracı olarak yaşıyorum."
        )
        UserRole.LANDLORD -> Pair(
            Icons.Default.Apartment,
            "Bir veya birden fazla mülkü kiraya veriyorum."
        )
        UserRole.BOTH -> Pair(
            Icons.Default.SupervisedUserCircle,
            "Hem kiracıyım hem de mülk sahibiyim."
        )
        else -> Pair(Icons.Default.Home, "")
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = if (isSelected)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column {
                Text(
                    text = role.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}