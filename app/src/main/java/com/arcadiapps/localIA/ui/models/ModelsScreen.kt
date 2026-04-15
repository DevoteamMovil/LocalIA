package com.arcadiapps.localIA.ui.models

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arcadiapps.localIA.data.model.*
import com.arcadiapps.localIA.inference.EngineState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelsScreen(
    onBack: () -> Unit,
    viewModel: ModelsViewModel = hiltViewModel()
) {
    val models by viewModel.models.collectAsState()
    val activeModel by viewModel.activeModel.collectAsState()
    val engineState by viewModel.engineState.collectAsState()
    val downloadError by viewModel.downloadError.collectAsState()

    var selectedFilter by remember { mutableStateOf<ModelType?>(null) }

    val filtered = if (selectedFilter == null) models
    else models.filter { it.type == selectedFilter }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modelos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Banner de error de descarga
            downloadError?.let { error ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Error: $error",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearDownloadError() }) {
                            Text("OK")
                        }
                    }
                }
            }

            // Filtros por tipo
            FilterChips(
                selected = selectedFilter,
                onSelect = { selectedFilter = it }
            )

            if (engineState is EngineState.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    "Cargando ${(engineState as EngineState.Loading).modelName}…",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.id }) { model ->
                    ModelCard(
                        model = model,
                        isActive = activeModel?.id == model.id,
                        onDownload = { viewModel.downloadModel(model) },
                        onCancel = { viewModel.cancelDownload(model) },
                        onDelete = { viewModel.deleteModel(model) },
                        onSelect = { viewModel.selectModel(model) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChips(selected: ModelType?, onSelect: (ModelType?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("Todos") })
        FilterChip(selected = selected == ModelType.TEXT, onClick = { onSelect(ModelType.TEXT) }, label = { Text("Texto") })
        FilterChip(selected = selected == ModelType.VISION, onClick = { onSelect(ModelType.VISION) }, label = { Text("Visión") })
        FilterChip(selected = selected == ModelType.AUDIO, onClick = { onSelect(ModelType.AUDIO) }, label = { Text("Audio") })
    }
}

@Composable
private fun ModelCard(
    model: AIModel,
    isActive: Boolean,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit
) {
    val containerColor = if (isActive)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (model.type) {
                        ModelType.TEXT -> Icons.Default.TextFields
                        ModelType.VISION -> Icons.Default.Visibility
                        ModelType.AUDIO -> Icons.Default.Mic
                        ModelType.MULTIMODAL -> Icons.Default.AutoAwesome
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(model.name, fontWeight = FontWeight.Bold)
                    Text(
                        "${model.parameterCount} · ${model.quantization} · ${formatSize(model.sizeBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isActive) {
                    Badge { Text("Activo") }
                }
                TierBadge(model.recommendedTier)
            }

            Spacer(Modifier.height(8.dp))
            Text(model.description, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))

            when (model.status) {
                ModelStatus.NOT_DOWNLOADED -> {
                    Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Descargar")
                    }
                }
                ModelStatus.DOWNLOADING -> {
                    Column {
                        LinearProgressIndicator(
                            progress = { model.downloadProgress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${model.downloadProgress}%", style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = onCancel) { Text("Cancelar") }
                        }
                    }
                }
                ModelStatus.READY -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!isActive) {
                            Button(onClick = onSelect, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Usar")
                            }
                        }
                        OutlinedButton(
                            onClick = onDelete,
                            modifier = if (isActive) Modifier.fillMaxWidth() else Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Eliminar")
                        }
                    }
                }
                ModelStatus.ERROR -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Error en descarga",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onDownload) { Text("Reintentar") }
                    }
                }
            }
        }
    }
}

@Composable
private fun TierBadge(tier: DeviceTier) {
    val (label, color) = when (tier) {
        DeviceTier.MEDIUM -> "Media" to MaterialTheme.colorScheme.secondary
        DeviceTier.HIGH -> "Alta" to MaterialTheme.colorScheme.tertiary
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun formatSize(bytes: Long): String {
    val gb = bytes / 1_000_000_000.0
    val mb = bytes / 1_000_000.0
    return if (gb >= 1) "%.1f GB".format(gb) else "%.0f MB".format(mb)
}
