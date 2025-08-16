package com.example.posecoach.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posecoach.network.FullResponse
import com.example.posecoach.network.SimpleResponse
import com.example.posecoach.repository.PoseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import java.io.File

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class SimpleOk(val data: SimpleResponse) : UiState()
    data class FullOk(val data: FullResponse) : UiState()
    data class OverlayOk(val bytes: ByteArray) : UiState()
    data class Error(val message: String) : UiState()
}

class PoseViewModel : ViewModel() {

    private val repo = PoseRepository()

    private val _state = MutableLiveData<UiState>(UiState.Idle)
    val state: LiveData<UiState> = _state

    fun analyzeSimple(image: File, mode: String?) {
        _state.value = UiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resp = repo.analyzeSimple(image, mode)
                if (resp.isSuccessful && resp.body() != null) {
                    _state.postValue(UiState.SimpleOk(resp.body()!!))
                } else {
                    _state.postValue(UiState.Error("HTTP ${resp.code()}: ${resp.errorBody()?.string() ?: "unknown"}"))
                }
            } catch (e: Exception) {
                _state.postValue(UiState.Error(e.message ?: "unknown error"))
            }
        }
    }

    fun analyzeFull(image: File, mode: String?) {
        _state.value = UiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resp = repo.analyzeFull(image, mode)
                if (resp.isSuccessful && resp.body() != null) {
                    _state.postValue(UiState.FullOk(resp.body()!!))
                } else {
                    _state.postValue(UiState.Error("HTTP ${resp.code()}: ${resp.errorBody()?.string() ?: "unknown"}"))
                }
            } catch (e: Exception) {
                _state.postValue(UiState.Error(e.message ?: "unknown error"))
            }
        }
    }

    fun overlay(image: File, mode: String?) {
        _state.value = UiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resp = repo.overlay(image, mode)
                if (resp.isSuccessful && resp.body() != null) {
                    val bytes = resp.body()!!.bytes()
                    _state.postValue(UiState.OverlayOk(bytes))
                } else {
                    _state.postValue(UiState.Error("HTTP ${resp.code()}: ${resp.errorBody()?.string() ?: "unknown"}"))
                }
            } catch (e: Exception) {
                _state.postValue(UiState.Error(e.message ?: "unknown error"))
            }
        }
    }
}
