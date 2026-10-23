package com.virueditx.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.virueditx.app.engine.EngineResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * MVVM bridge between the screens and the ViruEngine.
 * Screens never call the engine directly — they call [run], watch this state,
 * and navigate on success. Failures arrive as plain-language text, ready to show.
 */
class ToolViewModel(app: Application) : AndroidViewModel(app) {

    var working by mutableStateOf(false)
        private set
    var progressText by mutableStateOf("")
        private set
    var errorText by mutableStateOf<String?>(null)
        private set

    private var job: Job? = null

    fun <T> run(
        block: suspend (reportProgress: (String) -> Unit) -> EngineResult<T>,
        onSuccess: (T) -> Unit
    ) {
        if (working) return
        working = true
        errorText = null
        progressText = "Working…"
        job = viewModelScope.launch {
            when (val result = block { p -> progressText = p }) {
                is EngineResult.Success -> {
                    working = false
                    progressText = ""
                    onSuccess(result.value)
                }
                is EngineResult.Failure -> {
                    working = false
                    progressText = ""
                    errorText = result.reason
                }
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        working = false
        progressText = ""
    }

    fun clearError() {
        errorText = null
    }

    override fun onCleared() {
        job?.cancel()
        super.onCleared()
    }
}
