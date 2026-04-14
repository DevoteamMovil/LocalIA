package com.arcadiapps.localIA.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    var systemPromptText by remember(settings.systemPrompt) { mutableStateOf(settings.systemPrompt) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Apariencia ──────────────────────────────────────────────────
            SectionHeader(icon = Icons.Default.Palette, title = "Apariencia")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tema", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DarkModeOption.entries.forEach { option ->
                            FilterChip(
                                selected = settings.darkMode == option,
                                onClick = { viewModel.setDarkMode(option) },
                                label = {
                                    Text(when (option) {
                                        DarkModeOption.LIGHT -> "Claro"
                                        DarkModeOption.DARK -> "Oscuro"
                                        DarkModeOption.SYSTEM -> "Sistema"
                                    })
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Generación ──────────────────────────────────────────────────
            SectionHeader(icon = Icons.Default.Tune, title = "Generación")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    // Temperatura
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Temperatura", style = MaterialTheme.typography.bodyMedium)
                            Text("%.2f".format(settings.temperature),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = settings.temperature,
                            onValueChange = { viewModel.setTemperature(it) },
                            valueRange = 0f..2f,
                            steps = 39
                        )
                        Text(
                            "Valores bajos = más preciso · Valores altos = más creativo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider()

                    // Max tokens
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tokens máximos", style = MaterialTheme.typography.bodyMedium)
                            Text("${settings.maxTokens}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = settings.maxTokens.toFloat(),
                            onValueChange = { viewModel.setMaxTokens(it.roundToInt()) },
                            valueRange = 128f..4096f,
                            steps = 30
                        )
                    }

                    HorizontalDivider()

                    // Top-K
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Top-K", style = MaterialTheme.typography.bodyMedium)
                            Text("${settings.topK}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = settings.topK.toFloat(),
                            onValueChange = { viewModel.setTopK(it.roundToInt()) },
                            valueRange = 1f..100f,
                            steps = 98
                        )
                    }

                    HorizontalDivider()

                    // Streaming
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Respuesta en tiempo real", style = MaterialTheme.typography.bodyMedium)
                            Text("Muestra el texto mientras se genera",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = settings.streamingEnabled, onCheckedChange = viewModel::setStreaming)
                    }

                    HorizontalDivider()

                    // Keep loaded
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mantener modelo en memoria", style = MaterialTheme.typography.bodyMedium)
                            Text("Más rápido pero consume más RAM",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = settings.keepModelLoaded, onCheckedChange = viewModel::setKeepLoaded)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── System Prompt ───────────────────────────────────────────────
            SectionHeader(icon = Icons.Default.Psychology, title = "Prompt del sistema")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Define el comportamiento base del modelo en todas las conversaciones.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = systemPromptText,
                        onValueChange = { systemPromptText = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8,
                        placeholder = { Text("Ej: Eres un asistente útil y conciso que responde en español.") }
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                systemPromptText = ""
                                viewModel.setSystemPrompt("")
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Limpiar") }
                        Button(
                            onClick = { viewModel.setSystemPrompt(systemPromptText) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Guardar") }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary)
    }
}
