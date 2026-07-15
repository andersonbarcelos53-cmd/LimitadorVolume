package com.example.volumelimiter.ui.screens

import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Save
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.volumelimiter.ui.components.AppIcon
import com.example.volumelimiter.viewmodel.AppConfigViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppConfigScreen(
    packageName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppConfigViewModel = viewModel(),
) {
    LaunchedEffect(packageName) {
        viewModel.setPackageName(packageName)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val rule = state.rule
    var sliderValue by remember(rule?.packageName) {
        mutableFloatStateOf(rule?.maxVolumePercent?.toFloat() ?: 40f)
    }

    LaunchedEffect(rule?.maxVolumePercent) {
        rule?.let { sliderValue = it.maxVolumePercent.toFloat() }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Configurar limite") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (rule == null) {
                Text(
                    text = "Este aplicativo não está mais na lista de monitoramento.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                OutlinedButton(onClick = onBack) {
                    Text("Voltar")
                }
            } else {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            AppIcon(packageName = rule.packageName, size = 56.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = rule.appName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = rule.packageName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Limite ativo",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Switch(
                                checked = rule.enabled,
                                onCheckedChange = viewModel::setEnabled,
                            )
                        }

                        Text(
                            text = "Volume máximo: ${sliderValue.roundToInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Slider(
                            value = sliderValue,
                            onValueChange = { sliderValue = it },
                            onValueChangeFinished = {
                                viewModel.setLimit(sliderValue.roundToInt())
                            },
                            valueRange = 0f..100f,
                            steps = 99,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = {
                                    viewModel.setLimit(sliderValue.roundToInt())
                                    viewModel.save()
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Rounded.Save, contentDescription = null)
                                Text("Salvar")
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.remove()
                                    onBack()
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = null)
                                Text("Remover")
                            }
                        }
                    }
                }

                state.message?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
