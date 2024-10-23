package com.stripe.android.connect.example

import androidx.lifecycle.ViewModel
import com.github.kittinunf.fuel.core.FuelError
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.example.data.EmbeddedComponentService
import com.stripe.android.core.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val embeddedComponentService: EmbeddedComponentService = EmbeddedComponentService.getInstance(),
    private val networkingScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val logger: Logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG),
) : ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    init {
        fetchAccounts()
    }

    // public methods

    fun reload() {
        _state.update {
            it.copy(isLoading = true, errorMessage = null)
        }
        fetchAccounts()
    }

    // private methods

    @OptIn(PrivateBetaConnectSDK::class)
    private fun fetchAccounts() {
        networkingScope.launch {
            try {
                embeddedComponentService.getAccounts()
                _state.update {
                    it.copy(isLoading = false, errorMessage = null)
                }
            } catch (e: FuelError) {
                _state.update {
                    it.copy(isLoading = false, errorMessage = e.message)
                }
                logger.error("(MainViewModel) Error getting accounts: $e")
            }
        }
    }

    // state

    data class MainState(
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
    )
}
