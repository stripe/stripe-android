package com.stripe.android.connect.example.ui.embeddedcomponentmanagerloader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.core.FuelError
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.example.BuildConfig
import com.stripe.android.connect.example.data.EmbeddedComponentManagerProvider
import com.stripe.android.connect.example.data.EmbeddedComponentService
import com.stripe.android.connect.example.ui.common.Fail
import com.stripe.android.connect.example.ui.common.Loading
import com.stripe.android.connect.example.ui.common.Success
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
    private val loggingTag = this::class.java.simpleName

    private val _state = MutableStateFlow(EmbeddedComponentManagerLoaderState())
    val state: StateFlow<EmbeddedComponentManagerLoaderState> = _state.asStateFlow()

    init {
        initializeManager()
    }

    // Public methods

    fun reload() {
        loadManager()
    }

    // Private methods

    private fun initializeManager() {
        val manager = embeddedComponentManagerProvider.provideEmbeddedComponentManager()
        if (manager == null) {
            loadManager()
            return
        }

        _state.update {
            it.copy(embeddedComponentManagerAsync = Success(manager))
        }
    }

    private fun loadManager() {
        viewModelScope.launch {
            try {
                // Show loading state and reset the manager
                _state.update {
                    it.copy(embeddedComponentManagerAsync = Loading())
                }

                // fetch the accounts and publishable key needed by the SDK
                embeddedComponentService.getAccounts()

                // initialize the SDK, or throw an error if we're unable to
                val manager = embeddedComponentManagerProvider.provideEmbeddedComponentManager()
                val async = if (manager != null) {
                    Success(manager)
                } else {
                    Fail(IllegalStateException("Error initializing the SDK. Please try again"))
                }
                _state.update {
                    it.copy(embeddedComponentManagerAsync = async)
                }
            } catch (e: FuelError) {
                // unexpected error - show an error state and allow the user to retry
                _state.update {
                    it.copy(embeddedComponentManagerAsync = Fail(e))
                }
                logger.error("($loggingTag) Error getting accounts: $e")
            }
        }
    }
}
