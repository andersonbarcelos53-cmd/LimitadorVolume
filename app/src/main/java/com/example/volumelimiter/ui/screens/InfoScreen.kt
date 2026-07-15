package com.example.volumelimiter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(
    onBack: () -> Unit,
    onOpenUsageSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Informações e permissões") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            InfoCard(
                title = "Acesso ao uso",
                text = "Esta permissão permite detectar qual aplicativo está em primeiro plano. Ela é liberada nas configurações do Android.",
            )
            Button(
                onClick = onOpenUsageSettings,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.Visibility, contentDescription = null)
                Text("Abrir acesso ao uso")
            }

            InfoCard(
                title = "Notificação permanente",
                text = "Enquanto o monitoramento estiver ativo, o Android exibe uma notificação fixa. Ela abre o aplicativo, mas não desliga a proteção sem PIN.",
            )

            InfoCard(
                title = "Como o volume é limitado",
                text = "O aplicativo altera o volume geral de mídia do aparelho. Ele não controla o áudio interno separado de cada aplicativo.",
            )

            InfoCard(
                title = "Bateria",
                text = "Alguns fabricantes encerram serviços em segundo plano. Remova a restrição de bateria se o monitoramento parar sozinho.",
            )
            OutlinedButton(
                onClick = onOpenBatterySettings,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.BatterySaver, contentDescription = null)
                Text("Abrir configurações de bateria")
            }

            OutlinedButton(
                onClick = onOpenAppSettings,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.Settings, contentDescription = null)
                Text("Abrir configurações do aplicativo")
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    text: String,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
