package com.datarescue.pro.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.datarescue.pro.data.repository.DeviceInfoRepository
import com.datarescue.pro.domain.model.DeviceInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceInfoViewModel @Inject constructor(
    private val deviceInfoRepository: DeviceInfoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceInfoUiState())
    val uiState: StateFlow<DeviceInfoUiState> = _uiState.asStateFlow()

    fun loadDeviceInfo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val deviceInfo = deviceInfoRepository.getDeviceInfo()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    deviceInfo = deviceInfo,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to analyze device: ${e.message}"
                )
            }
        }
    }
}

data class DeviceInfoUiState(
    val isLoading: Boolean = false,
    val deviceInfo: DeviceInfo? = null,
    val error: String? = null
)