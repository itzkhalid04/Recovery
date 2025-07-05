// RecoverableFile.kt
package com.datarescue.pro.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import kotlinx.datetime.Instant

@Parcelize
data class RecoverableFile(
    val id: String,
    val name: String,
    val path: String,
    val size: Long,
    val type: FileType,
    val dateDeleted: @RawValue Instant? = null,
    val dateModified: @RawValue Instant,
    val thumbnailPath: String? = null,
    val isRecoverable: Boolean,
    val confidence: Int, // 0-100
    val isSelected: Boolean = false
) : Parcelable

enum class FileType(val displayName: String, val emoji: String) {
    PHOTO("Photos", "📷"),
    VIDEO("Videos", "🎬"),
    DOCUMENT("Documents", "📄"),
    AUDIO("Audio", "🎵"),
    ARCHIVE("Archives", "📦"),
    APK("APKs", "📱"),
    OTHER("Others", "📄")
}

enum class ScanType(val displayName: String, val description: String) {
    QUICK("Quick Scan", "Recently deleted files"),
    DEEP("Deep Scan", "Comprehensive recovery"),
    TARGETED("Targeted Scan", "Specific file types")
}

data class ScanProgress(
    val currentFile: String = "",
    val filesScanned: Int = 0,
    val totalFiles: Int = 0,
    val percentage: Int = 0,
    val timeElapsed: Long = 0,
    val estimatedTimeRemaining: Long = 0
)

data class RecoveryResult(
    val success: Boolean,
    val recoveredFiles: Int,
    val failedFiles: Int,
    val errors: List<String>,
    val totalSize: Long
)

data class FileTypeFilter(
    val type: FileType,
    val extensions: List<String>,
    val enabled: Boolean = true
)