package com.datarescue.pro.data.repository

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.datarescue.pro.domain.model.*
import com.datarescue.pro.domain.repository.FileRecoveryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class FileRecoveryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : FileRecoveryRepository {

    private val _scanProgress = MutableStateFlow(ScanProgress())
    private val _isScanning = MutableStateFlow(false)
    private var shouldStopScan = false

    private val fileTypeMap = mapOf(
        // Photos
        "jpg" to FileType.PHOTO, "jpeg" to FileType.PHOTO, "png" to FileType.PHOTO,
        "gif" to FileType.PHOTO, "bmp" to FileType.PHOTO, "webp" to FileType.PHOTO,
        "heic" to FileType.PHOTO, "tiff" to FileType.PHOTO,
        
        // Videos
        "mp4" to FileType.VIDEO, "avi" to FileType.VIDEO, "mov" to FileType.VIDEO,
        "mkv" to FileType.VIDEO, "3gp" to FileType.VIDEO, "flv" to FileType.VIDEO,
        "wmv" to FileType.VIDEO, "webm" to FileType.VIDEO,
        
        // Documents
        "pdf" to FileType.DOCUMENT, "doc" to FileType.DOCUMENT, "docx" to FileType.DOCUMENT,
        "xls" to FileType.DOCUMENT, "xlsx" to FileType.DOCUMENT, "ppt" to FileType.DOCUMENT,
        "pptx" to FileType.DOCUMENT, "txt" to FileType.DOCUMENT,
        
        // Audio
        "mp3" to FileType.AUDIO, "wav" to FileType.AUDIO, "aac" to FileType.AUDIO,
        "flac" to FileType.AUDIO, "ogg" to FileType.AUDIO, "m4a" to FileType.AUDIO,
        
        // Archives
        "zip" to FileType.ARCHIVE, "rar" to FileType.ARCHIVE, "7z" to FileType.ARCHIVE,
        "tar" to FileType.ARCHIVE, "gz" to FileType.ARCHIVE,
        
        // APK
        "apk" to FileType.APK
    )

    override suspend fun startScan(
        scanType: ScanType,
        fileTypes: List<FileTypeFilter>
    ): Flow<List<RecoverableFile>> = flow {
        _isScanning.value = true
        shouldStopScan = false
        
        val startTime = System.currentTimeMillis()
        val recoveredFiles = mutableListOf<RecoverableFile>()
        
        try {
            val scanPaths = getScanPaths(scanType)
            var totalFiles = 0
            
            // Estimate total files
            scanPaths.forEach { path ->
                totalFiles += estimateFileCount(File(path))
            }
            
            _scanProgress.value = _scanProgress.value.copy(
                totalFiles = totalFiles,
                filesScanned = 0,
                percentage = 0
            )
            
            // Scan each path
            scanPaths.forEach { path ->
                if (!shouldStopScan) {
                    val pathResults = scanPath(File(path), fileTypes, startTime)
                    recoveredFiles.addAll(pathResults)
                    emit(recoveredFiles.toList())
                }
            }
            
        } finally {
            _isScanning.value = false
        }
    }.flowOn(Dispatchers.IO)

    override fun getScanProgress(): Flow<ScanProgress> = _scanProgress.asStateFlow()

    override suspend fun recoverFiles(
        files: List<RecoverableFile>,
        destinationPath: String
    ): Flow<RecoveryResult> = flow {
        var recoveredCount = 0
        var failedCount = 0
        val errors = mutableListOf<String>()
        var totalSize = 0L
        
        files.forEachIndexed { index, file ->
            delay(100) // Simulate recovery time
            
            try {
                // Simulate file recovery
                val sourceFile = File(file.path)
                if (sourceFile.exists()) {
                    // In a real implementation, copy the file to destination
                    recoveredCount++
                    totalSize += file.size
                } else {
                    failedCount++
                    errors.add("File not found: ${file.name}")
                }
            } catch (e: Exception) {
                failedCount++
                errors.add("Failed to recover ${file.name}: ${e.message}")
            }
            
            // Emit progress
            emit(RecoveryResult(
                success = recoveredCount > 0,
                recoveredFiles = recoveredCount,
                failedFiles = failedCount,
                errors = errors.toList(),
                totalSize = totalSize
            ))
        }
    }.flowOn(Dispatchers.IO)

    override fun getDefaultFileTypeFilters(): List<FileTypeFilter> {
        return listOf(
            FileTypeFilter(FileType.PHOTO, listOf("jpg", "jpeg", "png", "gif", "bmp", "webp"), true),
            FileTypeFilter(FileType.VIDEO, listOf("mp4", "avi", "mov", "mkv", "3gp"), true),
            FileTypeFilter(FileType.DOCUMENT, listOf("pdf", "doc", "docx", "xls", "xlsx", "txt"), true),
            FileTypeFilter(FileType.AUDIO, listOf("mp3", "wav", "aac", "flac", "ogg"), true),
            FileTypeFilter(FileType.ARCHIVE, listOf("zip", "rar", "7z", "tar"), false),
            FileTypeFilter(FileType.APK, listOf("apk"), false)
        )
    }

    override suspend fun stopScan() {
        shouldStopScan = true
        _isScanning.value = false
    }

    override fun isScanning(): Flow<Boolean> = _isScanning.asStateFlow()

    private fun getScanPaths(scanType: ScanType): List<String> {
        val basePaths = mutableListOf<String>()
        
        // Add standard directories
        basePaths.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath)
        basePaths.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath)
        basePaths.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath)
        basePaths.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath)
        basePaths.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath)
        basePaths.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).absolutePath)
        
        return when (scanType) {
            ScanType.QUICK -> basePaths.take(3)
            ScanType.DEEP -> basePaths + Environment.getExternalStorageDirectory().absolutePath
            ScanType.TARGETED -> basePaths
        }
    }

    private fun estimateFileCount(directory: File): Int {
        return try {
            directory.listFiles()?.size ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private suspend fun scanPath(
        directory: File,
        fileTypes: List<FileTypeFilter>,
        startTime: Long
    ): List<RecoverableFile> {
        val results = mutableListOf<RecoverableFile>()
        
        try {
            directory.listFiles()?.forEach { file ->
                if (shouldStopScan) return results
                
                _scanProgress.value = _scanProgress.value.copy(
                    currentFile = file.name,
                    filesScanned = _scanProgress.value.filesScanned + 1,
                    percentage = if (_scanProgress.value.totalFiles > 0) {
                        (_scanProgress.value.filesScanned * 100) / _scanProgress.value.totalFiles
                    } else 0,
                    timeElapsed = System.currentTimeMillis() - startTime
                )
                
                if (file.isFile) {
                    val recoverableFile = analyzeFile(file)
                    if (recoverableFile != null && shouldIncludeFile(recoverableFile, fileTypes)) {
                        results.add(recoverableFile)
                    }
                } else if (file.isDirectory && file.canRead()) {
                    // Recursively scan subdirectories
                    results.addAll(scanPath(file, fileTypes, startTime))
                }
                
                delay(10) // Small delay to prevent UI blocking
            }
        } catch (e: Exception) {
            // Handle permission errors gracefully
        }
        
        return results
    }

    private fun analyzeFile(file: File): RecoverableFile? {
        return try {
            val extension = file.extension.lowercase()
            val fileType = fileTypeMap[extension] ?: FileType.OTHER
            
            val isRecoverable = isFileRecoverable(file)
            val confidence = calculateConfidence(file, isRecoverable)
            
            RecoverableFile(
                id = "${file.absolutePath}_${System.currentTimeMillis()}",
                name = file.name,
                path = file.absolutePath,
                size = file.length(),
                type = fileType,
                dateModified = Instant.fromEpochMilliseconds(file.lastModified()),
                isRecoverable = isRecoverable,
                confidence = confidence
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun isFileRecoverable(file: File): Boolean {
        return try {
            // Check if file is in cache or temp directory
            val path = file.absolutePath.lowercase()
            if (path.contains("cache") || path.contains("tmp") || path.contains("temp")) {
                return true
            }
            
            // Check if file was recently modified
            val dayInMs = 24 * 60 * 60 * 1000L
            val recentlyModified = (System.currentTimeMillis() - file.lastModified()) < (7 * dayInMs)
            
            recentlyModified
        } catch (e: Exception) {
            false
        }
    }

    private fun calculateConfidence(file: File, isRecoverable: Boolean): Int {
        if (!isRecoverable) return 0
        
        var confidence = 50 // Base confidence
        
        // Higher confidence for larger files
        when {
            file.length() > 10 * 1024 * 1024 -> confidence += 30 // > 10MB
            file.length() > 1024 * 1024 -> confidence += 20 // > 1MB
        }
        
        // Higher confidence for recently modified files
        val daysSinceModified = (System.currentTimeMillis() - file.lastModified()) / (24 * 60 * 60 * 1000L)
        when {
            daysSinceModified < 1 -> confidence += 30
            daysSinceModified < 7 -> confidence += 20
            daysSinceModified < 30 -> confidence += 10
        }
        
        // Add some randomness for demo purposes
        confidence += Random.nextInt(-10, 11)
        
        return confidence.coerceIn(0, 100)
    }

    private fun shouldIncludeFile(file: RecoverableFile, fileTypes: List<FileTypeFilter>): Boolean {
        val typeFilter = fileTypes.find { it.type == file.type }
        return typeFilter?.enabled ?: true
    }
}