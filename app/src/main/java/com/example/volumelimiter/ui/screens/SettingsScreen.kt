package com.example.volumelimiter.ui.screens

import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.volumelimiter.viewmodel.MainUiState
import com.example.volumelimiter.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenUsageSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenSecurity: () -> Unit,
    onLockNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var timeoutValue by remember(state.parentalControls.autoLockTimeoutSeconds) {
        mutableFloatStateOf(state.parentalControls.autoLockTimeoutSeconds.toFloat())
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Configurações") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Voltar")
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
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        SettingSwitch(
                            title = "Iniciar após reiniciar",
                            subtitle = "Reativa o monitoramento se ele estava ligado.",
                            checked = state.parentalControls.autoStartOnBoot,
                            onCheckedChange = viewModel::setAutoStartOnBoot,
                        )
                        SettingSwitch(
                            title = "Detalhes na notificação",
                            subtitle = "Mostra aplicativo e limite aplicado na notificação.",
                            checked = state.parentalControls.showNotificationDetails,
                            onCheckedChange = viewModel::setShowNotificationDetails,
                        )
                        Text(
                            text = "Bloquear painel após ${timeoutValue.roundToInt()} segundos",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Slider(
                            value = timeoutValue,
                            onValueChange = { timeoutValue = it },
                            onValueChangeFinished = {
                                viewModel.setAutoLockTimeoutSeconds(timeoutValue.roundToInt())
                            },
                            valueRange = 5f..300f,
                            steps = 58,
                        )
                    }
                }
            }

            item {
                PermissionChecklist(state = state)
            }

            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Ações",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Button(
                            onClick = { viewModel.testVolumeLimit(50) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Testar limite de volume em 50%")
                        }
                        OutlinedButton(
                            onClick = onOpenUsageSettings,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Abrir acesso ao uso")
                        }
                        OutlinedButton(
                            onClick = onOpenNotificationSettings,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Abrir notificações")
                        }
                        OutlinedButton(
                            onClick = onOpenBatterySettings,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Abrir bateria")
                        }
                        OutlinedButton(
                            onClick = onOpenSecurity,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Alterar PIN")
                        }
                        OutlinedButton(
                            onClick = onLockNow,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Bloquear agora")
                        }
                    }
                }
            }

            item {
                Text(
                    text = "O Android pode encerrar serviços em segundo plano em alguns aparelhos. Não há reinício automático após Forçar parada.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PermissionChecklist(state: MainUiState) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Permissões e acessos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            ChecklistLine("Acesso ao uso", state.usagePermissionGranted)
            ChecklistLine("Notificações", state.notificationPermissionGranted)
            ChecklistLine("Monitoramento", state.monitoringEnabled && state.status.isServiceRunning)
            ChecklistLine("Inicialização automática", state.parentalControls.autoStartOnBoot)
            ChecklistLine("Bateria sem restrição", state.ignoringBatteryOptimizations)
        }
    }
}

@Composable
private fun ChecklistLine(
    label: String,
    ok: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        Text(
            text = "$label: ${if (ok) "ok" else "pendente"}",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
