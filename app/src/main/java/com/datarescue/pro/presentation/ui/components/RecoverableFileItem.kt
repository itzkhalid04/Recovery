package com.datarescue.pro.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.datarescue.pro.domain.model.RecoverableFile
import com.datarescue.pro.presentation.ui.theme.*

@Composable
fun RecoverableFileItem(
    file: RecoverableFile,
    onSelectionChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = file.isSelected,
                onCheckedChange = onSelectionChanged
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = file.type.emoji,
                fontSize = 24.sp,
                modifier = Modifier.padding(end = 12.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${file.type.displayName.uppercase()} • ${formatFileSize(file.size)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${file.confidence}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = getConfidenceColor(file.confidence)
                )
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                ) {
                    LinearProgressIndicator(
                        progress = file.confidence / 100f,
                        modifier = Modifier.fillMaxSize(),
                        color = getConfidenceColor(file.confidence)
                    )
                }
            }
        }
    }
}

@Composable
private fun getConfidenceColor(confidence: Int) = when {
    confidence >= 80 -> SuccessGreen
    confidence >= 60 -> WarningOrange
    confidence >= 40 -> ConfidenceYellow
    else -> ErrorRed
}

private fun formatFileSize(bytes: Long): String {
    if (bytes == 0L) return "0 B"
    
    val k = 1024
    val sizes = arrayOf("B", "KB", "MB", "GB")
    val i = (Math.log(bytes.toDouble()) / Math.log(k.toDouble())).toInt()
    
    return String.format("%.1f %s", bytes / Math.pow(k.toDouble(), i.toDouble()), sizes[i])
}