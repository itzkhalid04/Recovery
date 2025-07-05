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

    val scanProgress: StateFlow<ScanProgress> = getScanProgressUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ScanProgress()
        )

    val isScanning: StateFlow<Boolean> = isScanningUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    init {
        loadInitialState()
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
            try {
                _uiState.update { it.copy(error = null, recoveredFiles = emptyList()) }
                
                startScanUseCase(
                    _uiState.value.selectedScanType,
                    _uiState.value.fileTypeFilters.filter { it.enabled }
                ).collect { files ->
                    _uiState.update { currentState ->
                        currentState.copy(recoveredFiles = files)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(error = "Scan failed: ${e.message}")
                }
            }
        }
    }

    fun stopScan() {
        viewModelScope.launch {
            stopScanUseCase()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class MainUiState(
    val selectedScanType: ScanType = ScanType.QUICK,
    val fileTypeFilters: List<FileTypeFilter> = emptyList(),
    val recoveredFiles: List<RecoverableFile> = emptyList(),
    val error: String? = null
)