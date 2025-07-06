package com.datarescue.pro.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.datarescue.pro.data.repository.AdvancedFileRecoveryRepository
import com.datarescue.pro.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val advancedRepository: AdvancedFileRecoveryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val scanProgress: StateFlow<ScanProgress> = advancedRepository.getScanProgress()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ScanProgress()
        )

    val isScanning: StateFlow<Boolean> = advancedRepository.isScanning()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    init {
        loadInitialState()
        initializeScanner()
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(
                    fileTypeFilters = advancedRepository.getDefaultFileTypeFilters()
                )
            }
        }
    }

    private fun initializeScanner() {
        viewModelScope.launch {
            try {
                val initialized = advancedRepository.initializeScanner()
                if (!initialized) {
                    _uiState.update { it.copy(error = "Failed to initialize native scanner") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Scanner initialization error: ${e.message}") }
            }
        }
    }

    fun setScanMode(scanMode: ScanMode) {
        _uiState.update { it.copy(selectedScanMode = scanMode) }
    }

    fun onScanTypeChanged(scanType: ScanType) {
        _uiState.update { it.copy(selectedScanType = scanType) }
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
                
                advancedRepository.startAdvancedScan(
                    _uiState.value.selectedScanMode,
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
            advancedRepository.stopScan()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class MainUiState(
    val selectedScanMode: ScanMode = ScanMode.BASIC,
    val selectedScanType: ScanType = ScanType.QUICK,
    val fileTypeFilters: List<FileTypeFilter> = emptyList(),
    val recoveredFiles: List<RecoverableFile> = emptyList(),
    val error: String? = null
)