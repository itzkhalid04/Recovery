package com.datarescue.pro.domain.repository

import com.datarescue.pro.domain.model.*
import kotlinx.coroutines.flow.Flow

interface FileRecoveryRepository {
    suspend fun startScan(
        scanType: ScanType,
        fileTypes: List<FileTypeFilter>
    ): Flow<List<RecoverableFile>>
    
    fun getScanProgress(): Flow<ScanProgress>
    
    suspend fun recoverFiles(
        files: List<RecoverableFile>,
        destinationPath: String
    ): Flow<RecoveryResult>
    
    fun getDefaultFileTypeFilters(): List<FileTypeFilter>
    
    suspend fun stopScan()
    
    fun isScanning(): Flow<Boolean>
}