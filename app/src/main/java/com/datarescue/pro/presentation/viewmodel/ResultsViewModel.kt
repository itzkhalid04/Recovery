package com.datarescue.pro.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.datarescue.pro.domain.model.*
import com.datarescue.pro.domain.usecase.RecoverFilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val recoverFilesUseCase: RecoverFilesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResultsUiState())
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    fun setRecoveredFiles(files: List<RecoverableFile>) {
        _uiState.value = _uiState.value.copy(recoveredFiles = files)
    }

    fun onFilterChanged(filter: FileType?) {
        _uiState.value = _uiState.value.copy(currentFilter = filter)
    }

    fun onFileSelectionChanged(file: RecoverableFile, selected: Boolean) {
        val updatedFiles = _uiState.value.recoveredFiles.map { f ->
            if (f.id == file.id) f.copy(isSelected = selected) else f
        }
        _uiState.value = _uiState.value.copy(recoveredFiles = updatedFiles)
    }

    fun selectAllFiles() {
        val filteredFiles = getFilteredFiles()
        val updatedFiles = _uiState.value.recoveredFiles.map { file ->
            if (filteredFiles.contains(file)) {
                file.copy(isSelected = true)
            } else {
                file
            }
        }
        _uiState.value = _uiState.value.copy(recoveredFiles = updatedFiles)
    }

    fun deselectAllFiles() {
        val updatedFiles = _uiState.value.recoveredFiles.map { file ->
            file.copy(isSelected = false)
        }
        _uiState.value = _uiState.value.copy(recoveredFiles = updatedFiles)
    }

    fun recoverSelectedFiles() {
        val selectedFiles = _uiState.value.recoveredFiles.filter { it.isSelected }
        if (selectedFiles.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRecovering = true)
            
            try {
                recoverFilesUseCase(selectedFiles, "/storage/emulated/0/DataRescue")
                    .collect { result ->
                        _uiState.value = _uiState.value.copy(
                            recoveryResult = result,
                            isRecovering = false
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    recoveryResult = RecoveryResult(
                        success = false,
                        recoveredFiles = 0,
                        failedFiles = selectedFiles.size,
                        errors = listOf("Recovery failed: ${e.message}"),
                        totalSize = 0L
                    ),
                    isRecovering = false
                )
            }
        }
    }

    fun dismissRecoveryResult() {
        _uiState.value = _uiState.value.copy(recoveryResult = null)
    }

    private fun getFilteredFiles(): List<RecoverableFile> {
        return if (_uiState.value.currentFilter == null) {
            _uiState.value.recoveredFiles
        } else {
            _uiState.value.recoveredFiles.filter { it.type == _uiState.value.currentFilter }
        }
    }

    fun getFileCountByType(type: FileType): Int {
        return _uiState.value.recoveredFiles.count { it.type == type }
    }

    fun getSelectedFiles(): List<RecoverableFile> {
        return _uiState.value.recoveredFiles.filter { it.isSelected }
    }

    fun getTotalSelectedSize(): Long {
        return getSelectedFiles().sumOf { it.size }
    }
}

data class ResultsUiState(
    val recoveredFiles: List<RecoverableFile> = emptyList(),
    val currentFilter: FileType? = null,
    val isRecovering: Boolean = false,
    val recoveryResult: RecoveryResult? = null
)