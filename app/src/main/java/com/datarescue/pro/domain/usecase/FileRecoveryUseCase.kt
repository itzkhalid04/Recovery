package com.datarescue.pro.domain.usecase

import com.datarescue.pro.domain.model.*
import com.datarescue.pro.domain.repository.FileRecoveryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class StartScanUseCase @Inject constructor(
    private val repository: FileRecoveryRepository
) {
    suspend operator fun invoke(
        scanType: ScanType,
        fileTypes: List<FileTypeFilter>
    ): Flow<List<RecoverableFile>> {
        return repository.startScan(scanType, fileTypes)
    }
}

class GetScanProgressUseCase @Inject constructor(
    private val repository: FileRecoveryRepository
) {
    operator fun invoke(): Flow<ScanProgress> {
        return repository.getScanProgress()
    }
}

class RecoverFilesUseCase @Inject constructor(
    private val repository: FileRecoveryRepository
) {
    suspend operator fun invoke(
        files: List<RecoverableFile>,
        destinationPath: String
    ): Flow<RecoveryResult> {
        return repository.recoverFiles(files, destinationPath)
    }
}

class GetDefaultFiltersUseCase @Inject constructor(
    private val repository: FileRecoveryRepository
) {
    operator fun invoke(): List<FileTypeFilter> {
        return repository.getDefaultFileTypeFilters()
    }
}

class StopScanUseCase @Inject constructor(
    private val repository: FileRecoveryRepository
) {
    suspend operator fun invoke() {
        repository.stopScan()
    }
}

class IsScanningUseCase @Inject constructor(
    private val repository: FileRecoveryRepository
) {
    operator fun invoke(): Flow<Boolean> {
        return repository.isScanning()
    }
}