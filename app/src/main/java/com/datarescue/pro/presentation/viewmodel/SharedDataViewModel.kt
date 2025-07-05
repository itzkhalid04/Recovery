package com.datarescue.pro.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.datarescue.pro.domain.model.RecoverableFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SharedDataViewModel @Inject constructor() : ViewModel() {
    
    private val _recoveredFiles = MutableStateFlow<List<RecoverableFile>>(emptyList())
    val recoveredFiles: StateFlow<List<RecoverableFile>> = _recoveredFiles.asStateFlow()
    
    fun setRecoveredFiles(files: List<RecoverableFile>) {
        _recoveredFiles.value = files
    }
    
    fun clearRecoveredFiles() {
        _recoveredFiles.value = emptyList()
    }
}