package com.datarescue.pro.data.repository

import android.content.Context
import android.os.Environment
import com.datarescue.pro.data.native.NativeFileScanner
import com.datarescue.pro.data.native.NativeRecoverableFile
import com.datarescue.pro.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class AdvancedFileRecoveryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nativeScanner: NativeFileScanner,
    private val deviceInfoRepository: DeviceInfoRepository
) {
    
    private val _scanProgress = MutableStateFlow(ScanProgress())
    private val _isScanning = MutableStateFlow(false)
    private var shouldStopScan = false
    
    suspend fun initializeScanner(): Boolean = withContext(Dispatchers.IO) {
        val deviceInfo = deviceInfoRepository.getDeviceInfo()
        nativeScanner.initializeNative(deviceInfo.isRooted)
    }
    
    suspend fun startAdvancedScan(
        scanMode: ScanMode,
        fileTypes: List<FileTypeFilter>,
        partition: String? = null
    ): Flow<List<RecoverableFile>> = flow {
        _isScanning.value = true
        shouldStopScan = false
        
        val startTime = System.currentTimeMillis()
        
        try {
            _scanProgress.value = ScanProgress()
            
            val nativeFiles = when (scanMode) {
                ScanMode.BASIC -> performBasicScan(fileTypes, startTime)
                ScanMode.ADVANCED -> performAdvancedScan(fileTypes, partition, startTime)
                ScanMode.DEEP -> performDeepScan(fileTypes, partition, startTime)
            }
            
            val recoveredFiles = nativeFiles.map { convertNativeToRecoverableFile(it) }
            
            _scanProgress.value = _scanProgress.value.copy(
                percentage = 100,
                timeElapsed = System.currentTimeMillis() - startTime
            )
            
            emit(recoveredFiles)
            
        } catch (e: Exception) {
            throw e
        } finally {
            _isScanning.value = false
        }
    }.flowOn(Dispatchers.IO)
    
    private suspend fun performBasicScan(
        fileTypes: List<FileTypeFilter>,
        startTime: Long
    ): Array<NativeRecoverableFile> {
        val enabledTypes = fileTypes.filter { it.enabled }.map { it.type.ordinal }.toIntArray()
        
        updateProgress("Starting basic scan...", 0, 100, startTime)
        
        return nativeScanner.startQuickScan(enabledTypes)
    }
    
    private suspend fun performAdvancedScan(
        fileTypes: List<FileTypeFilter>,
        partition: String?,
        startTime: Long
    ): Array<NativeRecoverableFile> {
        val enabledTypes = fileTypes.filter { it.enabled }.map { it.type.ordinal }.toIntArray()
        val targetPartition = partition ?: "/data"
        
        updateProgress("Starting advanced scan on $targetPartition...", 0, 100, startTime)
        
        return nativeScanner.startDeepScan(targetPartition, enabledTypes)
    }
    
    private suspend fun performDeepScan(
        fileTypes: List<FileTypeFilter>,
        partition: String?,
        startTime: Long
    ): Array<NativeRecoverableFile> {
        val enabledTypes = fileTypes.filter { it.enabled }.map { it.type.ordinal }.toIntArray()
        val availablePartitions = nativeScanner.getAvailablePartitions()
        
        val allResults = mutableListOf<NativeRecoverableFile>()
        
        val partitionsToScan = if (partition != null) {
            listOf(partition)
        } else {
            availablePartitions.toList()
        }
        
        partitionsToScan.forEachIndexed { index, part ->
            if (shouldStopScan) return allResults.toTypedArray()
            
            updateProgress("Deep scanning partition $part...", 
                          (index * 100) / partitionsToScan.size, 
                          100, startTime)
            
            val partitionResults = nativeScanner.startDeepScan(part, enabledTypes)
            allResults.addAll(partitionResults)
            
            delay(100) // Small delay between partitions
        }
        
        return allResults.toTypedArray()
    }
    
    private fun convertNativeToRecoverableFile(nativeFile: NativeRecoverableFile): RecoverableFile {
        return RecoverableFile(
            id = "${nativeFile.path}_${System.currentTimeMillis()}_${Random.nextInt()}",
            name = nativeFile.name,
            path = nativeFile.path,
            originalPath = nativeFile.originalPath,
            size = nativeFile.size,
            type = FileType.values()[nativeFile.fileType.coerceIn(0, FileType.values().size - 1)],
            dateModified = Instant.fromEpochMilliseconds(nativeFile.dateModified),
            dateDeleted = if (nativeFile.dateDeleted > 0) {
                Instant.fromEpochMilliseconds(nativeFile.dateDeleted)
            } else null,
            isRecoverable = nativeFile.isRecoverable,
            confidence = nativeFile.confidence,
            isSelected = false
        )
    }
    
    suspend fun recoverFiles(
        files: List<RecoverableFile>,
        destinationPath: String
    ): Flow<RecoveryResult> = flow {
        var recoveredCount = 0
        var failedCount = 0
        val errors = mutableListOf<String>()
        var totalSize = 0L
        
        val destinationDir = File(destinationPath)
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }
        
        files.forEachIndexed { index, file ->
            delay(100) // Simulate recovery time
            
            try {
                val outputPath = File(destinationDir, file.name).absolutePath
                val success = nativeScanner.recoverFile(file.path, outputPath)
                
                if (success) {
                    recoveredCount++
                    totalSize += file.size
                } else {
                    failedCount++
                    errors.add("Failed to recover: ${file.name}")
                }
            } catch (e: Exception) {
                failedCount++
                errors.add("Error recovering ${file.name}: ${e.message}")
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
    
    private fun updateProgress(currentFile: String, scanned: Int, total: Int, startTime: Long) {
        val percentage = if (total > 0) {
            ((scanned.toFloat() / total) * 100).toInt().coerceIn(0, 100)
        } else {
            _scanProgress.value.percentage
        }
        
        _scanProgress.value = _scanProgress.value.copy(
            currentFile = currentFile,
            filesScanned = if (scanned > 0) scanned.toLong() else _scanProgress.value.filesScanned + 1,
            totalFiles = if (total > 0) total.toLong() else _scanProgress.value.totalFiles,
            percentage = percentage,
            timeElapsed = System.currentTimeMillis() - startTime
        )
    }
    
    fun getScanProgress(): Flow<ScanProgress> = _scanProgress.asStateFlow()
    
    suspend fun stopScan() {
        shouldStopScan = true
        nativeScanner.stopScan()
        _isScanning.value = false
    }
    
    fun isScanning(): Flow<Boolean> = _isScanning.asStateFlow()
    
    fun getDefaultFileTypeFilters(): List<FileTypeFilter> {
        return listOf(
            FileTypeFilter(FileType.PHOTO, listOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic"), true),
            FileTypeFilter(FileType.VIDEO, listOf("mp4", "avi", "mov", "mkv", "3gp", "m4v"), true),
            FileTypeFilter(FileType.DOCUMENT, listOf("pdf", "doc", "docx", "xls", "xlsx", "txt"), true),
            FileTypeFilter(FileType.AUDIO, listOf("mp3", "wav", "aac", "flac", "ogg", "m4a"), true),
            FileTypeFilter(FileType.ARCHIVE, listOf("zip", "rar", "7z", "tar"), false),
            FileTypeFilter(FileType.APK, listOf("apk"), false)
        )
    }
}