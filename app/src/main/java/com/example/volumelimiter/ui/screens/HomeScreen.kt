package com.example.volumelimiter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.volumelimiter.ui.components.PermissionNotice
import com.example.volumelimiter.ui.components.RuleListItem
import com.example.volumelimiter.viewmodel.MainUiState
import com.example.volumelimiter.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onAddApp: () -> Unit,
    onOpenConfig: (String) -> Unit,
    onOpenInfo: () -> Unit,
    onOpenUsageSettings: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Limitador de Volume") },
                actions = {
                    IconButton(onClick = onOpenInfo) {
                        Icon(Icons.Rounded.Info, contentDescription = "Informações")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                MonitoringHeader(
                    state = state,
                    onCheckedChange = { checked ->
                        if (checked) {
                            if (!state.usagePermissionGranted) {
                                onOpenUsageSettings()
                            } else {
                                if (!state.notificationPermissionGranted) {
                                    onRequestNotificationPermission()
                                }
                                viewModel.setMonitoringEnabled(true)
                            }
                        } else {
                            viewModel.setMonitoringEnabled(false)
                        }
                    },
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            if (!state.usagePermissionGranted) {
                item {
                    PermissionNotice(
                        title = "Acesso ao uso pendente",
                        message = "O Android precisa permitir que o app veja qual aplicativo está em primeiro plano.",
                        buttonText = "Liberar acesso",
                        onClick = onOpenUsageSettings,
                    )
                }
            }

            if (!state.notificationPermissionGranted) {
                item {
                    PermissionNotice(
                        title = "Notificação pendente",
                        message = "A notificação permanente informa que o monitoramento está ativo.",
                        buttonText = "Permitir notificação",
                        onClick = onRequestNotificationPermission,
                    )
                }
            }

            item {
                StatusSummary(state = state)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Aplicativos configurados",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Button(onClick = onAddApp) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Text("Adicionar")
                    }
                }
            }

            if (state.rules.isEmpty()) {
                item {
                    Text(
                        text = "Nenhum aplicativo configurado ainda.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
            } else {
                items(
                    items = state.rules,
                    key = { it.packageName },
                ) { rule ->
                    RuleListItem(
                        rule = rule,
                        onClick = { onOpenConfig(rule.packageName) },
                        onEnabledChange = { enabled ->
                            viewModel.setRuleEnabled(rule.packageName, enabled)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MonitoringHeader(
    state: MainUiState,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Ativar monitoramento",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (state.status.isServiceRunning) "Serviço ativo" else "Serviço parado",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = state.monitoringEnabled,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
private fun StatusSummary(
    state: MainUiState,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            StatusLine(
                label = "Aplicativo atual",
                value = state.status.currentAppName
                    ?: state.status.currentPackageName
                    ?: "Não identificado",
            )
            StatusLine(
                label = "Limite aplicado",
                value = state.status.appliedRule?.let {
                    "${it.appName}: ${it.maxVolumePercent}%"
                } ?: "Nenhum",
            )
            StatusLine(
                label = "Volume atual",
                value = state.currentVolumePercent?.let { "$it%" } ?: "Indisponível",
            )
            state.actionMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatusLine(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1.2f),
        )
    }
}
