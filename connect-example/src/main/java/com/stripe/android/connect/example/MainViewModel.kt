package com.stripe.android.connect.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.core.FuelError
import com.stripe.android.connect.example.data.EmbeddedComponentService
import com.stripe.android.core.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val embeddedComponentService: EmbeddedComponentService,
) : ViewModel() {

    private val logger: Logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG)
    private val loggingTag = this::class.java.name

    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    init {
        fetchAccounts()
    }

    // Public methods

    fun reload() {
        _state.update {
            it.copy(isLoading = true, errorMessage = null)
        }
        fetchAccounts()
    }

    // Private methods

    private fun fetchAccounts() {
        viewModelScope.launch {
            try {
                embeddedComponentService.getAccounts()
                _state.update {
                    it.copy(isLoading = false, errorMessage = null)
                }
            } catch (e: FuelError) {
                _state.update {
                    it.copy(isLoading = false, errorMessage = e.message)
                }
                logger.error("($loggingTag) Error getting accounts: $e")
            }
        }
    }

    // State

    data class MainState(
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
    )
}
