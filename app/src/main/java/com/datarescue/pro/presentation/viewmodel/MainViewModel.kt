// MainViewModel.kt
package com.datarescue.pro.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.datarescue.pro.domain.model.*
import com.datarescue.pro.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val startScanUseCase: StartScanUseCase,
    private val getScanProgressUseCase: GetScanProgressUseCase,
    private val stopScanUseCase: StopScanUseCase,
    private val isScanningUseCase: IsScanningUseCase,
    private val getDefaultFiltersUseCase: GetDefaultFiltersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _scanProgress = MutableStateFlow(ScanProgress())
    val scanProgress: StateFlow<ScanProgress> = _scanProgress

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    init {
        loadInitialState()
        observeScanProgress()
        observeScanningStatus()
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(
                    fileTypeFilters = getDefaultFiltersUseCase()
                )
            }
        }
    }

    private fun observeScanProgress() {
        viewModelScope.launch {
            getScanProgressUseCase().collect { progress ->
                _scanProgress.value = progress
            }
        }
    }

    private fun observeScanningStatus() {
        viewModelScope.launch {
            isScanningUseCase().collect { scanning ->
                _isScanning.value = scanning
            }
        }
    }

    fun onScanTypeChanged(scanType: ScanType) {
        _uiState.update { currentState ->
            currentState.copy(selectedScanType = scanType)
        }
    }

    fun onFileTypeToggled(fileType: FileType, enabled: Boolean) {
        _uiState.update { currentState ->
            val updatedFilters = currentState.fileTypeFilters.map { filter ->
                if (filter.type == fileType) filter.copy(enabled = enabled) else filter
            }
            currentState.copy(fileTypeFilters = updatedFilters)
        }
    }

    fun startScan() {
        viewModelScope.launch {
            startScanUseCase(
                _uiState.value.selectedScanType,
                _uiState.value.fileTypeFilters.filter { it.enabled }
            ).collect { files ->
                _uiState.update { currentState ->
                    currentState.copy(recoveredFiles = files)
                }
            }
        }
    }

    fun stopScan() {
        viewModelScope.launch {
            stopScanUseCase()
        }
    }
}

data class MainUiState(
    val selectedScanType: ScanType = ScanType.QUICK,
    val fileTypeFilters: List<FileTypeFilter> = emptyList(),
    val recoveredFiles: List<RecoverableFile> = emptyList(),
    val error: String? = null
)