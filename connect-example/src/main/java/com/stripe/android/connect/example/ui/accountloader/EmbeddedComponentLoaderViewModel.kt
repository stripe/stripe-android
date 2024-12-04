package com.stripe.android.connect.example.ui.accountloader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.core.FuelError
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.example.BuildConfig
import com.stripe.android.connect.example.data.EmbeddedComponentManagerProvider
import com.stripe.android.connect.example.data.EmbeddedComponentService
import com.stripe.android.connect.example.ui.common.Async
import com.stripe.android.connect.example.ui.common.Fail
import com.stripe.android.connect.example.ui.common.Loading
import com.stripe.android.connect.example.ui.common.Success
import com.stripe.android.connect.example.ui.common.Uninitialized
import com.stripe.android.core.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(PrivateBetaConnectSDK::class)
@HiltViewModel
class EmbeddedComponentLoaderViewModel @Inject constructor(
    private val embeddedComponentService: EmbeddedComponentService,
    private val embeddedComponentManagerProvider: EmbeddedComponentManagerProvider,
) : ViewModel() {

    private val logger: Logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG)
    private val loggingTag = this::class.java.name


    private val _state = MutableStateFlow(EmbeddedComponentLoaderState())
    val state: StateFlow<EmbeddedComponentLoaderState> = _state.asStateFlow()

    init {
        initializeManager()
    }

    // Public methods

    fun reload() {
        loadManager()
    }

    // Private methods

    private fun initializeManager() {
        if (embeddedComponentService.publishableKey.value == null) {
            loadManager()
            return
        }

        val manager = embeddedComponentManagerProvider.provideEmbeddedComponentManager()
        if (manager == null) {
            loadManager()
            return
        }

        _state.update {
            it.copy(embeddedComponentAsync = Success(manager))
        }
    }

    private fun loadManager() {
        viewModelScope.launch {
            try {
                _state.update {
                    it.copy(embeddedComponentAsync = Loading())
                }

                embeddedComponentService.getAccounts()

                val manager = embeddedComponentManagerProvider.provideEmbeddedComponentManager()
                val async = if (manager != null) {
                    Success(manager)
                } else {
                    Fail(IllegalStateException("Error initializing the SDK. Please try again"))
                }
                _state.update {
                    it.copy(embeddedComponentAsync = async)
                }
            } catch (e: FuelError) {
                _state.update {
                    it.copy(embeddedComponentAsync = Fail(e))
                }
                logger.error("($loggingTag) Error getting accounts: $e")
            }
        }
    }

    // State

    @OptIn(PrivateBetaConnectSDK::class)
    data class EmbeddedComponentLoaderState(
        val embeddedComponentAsync: Async<EmbeddedComponentManager> = Uninitialized,
    )
}
