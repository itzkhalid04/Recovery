package com.datarescue.pro.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.datarescue.pro.domain.model.DeviceInfo
import com.datarescue.pro.domain.model.ScanMode
import com.datarescue.pro.presentation.ui.theme.PrimaryBlue
import com.datarescue.pro.presentation.ui.theme.PrimaryPurple
import com.datarescue.pro.presentation.ui.theme.SuccessGreen
import com.datarescue.pro.presentation.ui.theme.ErrorRed
import com.datarescue.pro.presentation.viewmodel.DeviceInfoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoScreen(
    onScanModeSelected: (ScanMode) -> Unit,
    viewModel: DeviceInfoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadDeviceInfo()
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(PrimaryBlue, PrimaryPurple)
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (uiState.deviceInfo?.isRooted == true) Icons.Default.Security else Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Device Analysis",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = "System capabilities and recovery options",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Analyzing device...")
                }
            }
        } else if (uiState.error != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = ErrorRed
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Analysis Failed",
                        style = MaterialTheme.typography.titleLarge,
                        color = ErrorRed
                    )
                    Text(
                        text = uiState.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadDeviceInfo() }) {
                        Text("Retry")
                    }
                }
            }
        } else {
            uiState.deviceInfo?.let { deviceInfo ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Root Status Card
                    item {
                        RootStatusCard(deviceInfo = deviceInfo)
                    }

                    // Device Capabilities Card
                    item {
                        DeviceCapabilitiesCard(deviceInfo = deviceInfo)
                    }

                    // Storage Information Card
                    item {
                        StorageInfoCard(deviceInfo = deviceInfo)
                    }

                    // Available Partitions Card
                    if (deviceInfo.availablePartitions.isNotEmpty()) {
                        item {
                            PartitionsCard(partitions = deviceInfo.availablePartitions)
                        }
                    }

                    // Scan Mode Selection
                    item {
                        ScanModeSelectionCard(
                            deviceInfo = deviceInfo,
                            onScanModeSelected = onScanModeSelected
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RootStatusCard(deviceInfo: DeviceInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (deviceInfo.isRooted) Icons.Default.Security else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (deviceInfo.isRooted) SuccessGreen else ErrorRed,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Root Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (deviceInfo.isRooted) "ROOTED" else "NOT ROOTED",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (deviceInfo.isRooted) SuccessGreen else ErrorRed
                )
                
                if (deviceInfo.isRooted) {
                    Text(
                        text = deviceInfo.rootMethod,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (deviceInfo.isRooted) {
                    "Advanced recovery features available including deep file system scanning and raw device access."
                } else {
                    "Basic recovery features available. Root access would enable advanced recovery capabilities."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DeviceCapabilitiesCard(deviceInfo: DeviceInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Recovery Capabilities",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            val capabilities = listOf(
                "System Partition Access" to deviceInfo.capabilities.canAccessSystemPartition,
                "Deep File System Scan" to deviceInfo.capabilities.canPerformDeepScan,
                "Deleted File Recovery" to deviceInfo.capabilities.canRecoverDeletedFiles,
                "Raw Device Access" to deviceInfo.capabilities.canAccessRawDevice
            )

            capabilities.forEach { (capability, available) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (available) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (available) SuccessGreen else ErrorRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = capability,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Supported File Systems: ${deviceInfo.capabilities.supportedFileSystems.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StorageInfoCard(deviceInfo: DeviceInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Storage Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "File System",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = deviceInfo.fileSystemType,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Storage",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatBytes(deviceInfo.totalStorage),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val usedStorage = deviceInfo.totalStorage - deviceInfo.freeStorage
            val usagePercentage = if (deviceInfo.totalStorage > 0) {
                (usedStorage.toFloat() / deviceInfo.totalStorage * 100).toInt()
            } else 0

            LinearProgressIndicator(
                progress = usagePercentage / 100f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Used: ${formatBytes(usedStorage)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Free: ${formatBytes(deviceInfo.freeStorage)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun PartitionsCard(partitions: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Available Partitions (${partitions.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            partitions.take(5).forEach { partition ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = partition,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }

            if (partitions.size > 5) {
                Text(
                    text = "... and ${partitions.size - 5} more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun ScanModeSelectionCard(
    deviceInfo: DeviceInfo,
    onScanModeSelected: (ScanMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Select Recovery Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Basic Mode
            ScanModeButton(
                title = "Basic Recovery",
                description = "Scan accessible areas and recently deleted files",
                icon = Icons.Default.Search,
                enabled = true,
                recommended = !deviceInfo.isRooted,
                onClick = { onScanModeSelected(ScanMode.BASIC) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Advanced Mode
            ScanModeButton(
                title = "Advanced Recovery",
                description = "Deep file system analysis with root access",
                icon = Icons.Default.Security,
                enabled = deviceInfo.capabilities.canPerformDeepScan,
                recommended = deviceInfo.isRooted,
                onClick = { onScanModeSelected(ScanMode.ADVANCED) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Deep Mode
            ScanModeButton(
                title = "Deep Recovery",
                description = "Raw device scanning and file carving",
                icon = Icons.Default.Storage,
                enabled = deviceInfo.capabilities.canAccessRawDevice,
                recommended = false,
                onClick = { onScanModeSelected(ScanMode.DEEP) }
            )
        }
    }
}

@Composable
private fun ScanModeButton(
    title: String,
    description: String,
    icon: ImageVector,
    enabled: Boolean,
    recommended: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (enabled) onClick else { },
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = if (recommended) MaterialTheme.colorScheme.primaryContainer
                           else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (enabled) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (recommended) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RECOMMENDED",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                if (!enabled) {
                    Text(
                        text = "Requires root access",
                        style = MaterialTheme.typography.labelSmall,
                        color = ErrorRed
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes == 0L) return "0 B"
    
    val k = 1024
    val sizes = arrayOf("B", "KB", "MB", "GB", "TB")
    val i = (Math.log(bytes.toDouble()) / Math.log(k.toDouble())).toInt()
    
    return String.format("%.1f %s", bytes / Math.pow(k.toDouble(), i.toDouble()), sizes[i])
}