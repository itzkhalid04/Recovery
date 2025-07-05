package com.datarescue.pro.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.datarescue.pro.domain.model.FileType
import com.datarescue.pro.domain.model.RecoverableFile
import com.datarescue.pro.presentation.ui.components.RecoverableFileItem
import com.datarescue.pro.presentation.viewmodel.ResultsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    recoveredFiles: List<RecoverableFile>,
    onNavigateBack: () -> Unit,
    viewModel: ResultsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(recoveredFiles) {
        viewModel.setRecoveredFiles(recoveredFiles)
    }

    val filteredFiles = remember(uiState.recoveredFiles, uiState.currentFilter) {
        if (uiState.currentFilter == null) {
            uiState.recoveredFiles
        } else {
            uiState.recoveredFiles.filter { it.type == uiState.currentFilter }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Recovery Results") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        // Stats Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FileType.values().take(4).forEach { type ->
                    val count = viewModel.getFileCountByType(type)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = type.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Filter Chips
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        onClick = { viewModel.onFilterChanged(null) },
                        label = { Text("All") },
                        selected = uiState.currentFilter == null
                    )
                    FileType.values().take(4).forEach { type ->
                        FilterChip(
                            onClick = { viewModel.onFilterChanged(type) },
                            label = { Text(type.displayName) },
                            selected = uiState.currentFilter == type
                        )
                    }
                }
            }

            items(filteredFiles) { file ->
                RecoverableFileItem(
                    file = file,
                    onSelectionChanged = { selected ->
                        viewModel.onFileSelectionChanged(file, selected)
                    }
                )
            }
        }

        // Bottom Action Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${viewModel.getSelectedFiles().size} files selected",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatFileSize(viewModel.getTotalSelectedSize()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = { viewModel.selectAllFiles() }) {
                            Text("Select All")
                        }
                        TextButton(onClick = { viewModel.deselectAllFiles() }) {
                            Text("Deselect All")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = { viewModel.recoverSelectedFiles() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = viewModel.getSelectedFiles().isNotEmpty() && !uiState.isRecovering,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isRecovering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Recovering...")
                    } else {
                        Text(
                            text = "Recover Selected Files",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Recovery Result Dialog
    uiState.recoveryResult?.let { result ->
        AlertDialog(
            onDismissRequest = { /* Handle dismiss */ },
            title = { Text("Recovery Complete") },
            text = {
                Column {
                    Text("Successfully recovered: ${result.recoveredFiles} files")
                    if (result.failedFiles > 0) {
                        Text("Failed to recover: ${result.failedFiles} files")
                    }
                    Text("Total size: ${formatFileSize(result.totalSize)}")
                }
            },
            confirmButton = {
                TextButton(onClick = onNavigateBack) {
                    Text("OK")
                }
            }
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes == 0L) return "0 B"
    
    val k = 1024
    val sizes = arrayOf("B", "KB", "MB", "GB")
    val i = (Math.log(bytes.toDouble()) / Math.log(k.toDouble())).toInt()
    
    return String.format("%.1f %s", bytes / Math.pow(k.toDouble(), i.toDouble()), sizes[i])
}