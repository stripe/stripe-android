package com.stripe.android.connect.example.ui.embeddedcomponentmanagerloader

import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.core.FuelError
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.example.core.Fail
import com.stripe.android.connect.example.core.Loading
import com.stripe.android.connect.example.core.Success
import com.stripe.android.connect.example.data.EmbeddedComponentManagerFactory
import com.stripe.android.connect.example.data.EmbeddedComponentService
import com.stripe.android.connect.example.data.SettingsService
import com.stripe.android.connect.example.ui.appearance.AppearanceInfo
import com.stripe.android.core.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(PrivateBetaConnectSDK::class)
@HiltViewModel
class EmbeddedComponentLoaderViewModel @Inject constructor(
    private val embeddedComponentService: EmbeddedComponentService,
    private val embeddedComponentManagerFactory: EmbeddedComponentManagerFactory,
    private val settingsService: SettingsService,
    private val logger: Logger,
) : ViewModel(), DefaultLifecycleObserver {

    private val loggingTag = this::class.java.simpleName

    private val _state = MutableStateFlow(EmbeddedComponentManagerLoaderState())
    val state: StateFlow<EmbeddedComponentManagerLoaderState> = _state.asStateFlow()

    init {
        loadManagerIfNecessary()
    }

    fun reload() {
        loadManager()
    }

    override fun onCreate(owner: LifecycleOwner) {
        val activity = owner as? ComponentActivity
            ?: return

        // Bind appearance settings to the manager.
        activity.lifecycleScope.launch {
            val managerFlow = _state
                .map { it.embeddedComponentManagerAsync() }
                .filterNotNull()
            val appearanceFlow = settingsService.getAppearanceIdFlow()
                .filterNotNull()
                .map { id -> AppearanceInfo.getAppearance(id, activity).appearance }
            combine(managerFlow, appearanceFlow, ::Pair).collectLatest { (manager, appearance) ->
                logger.debug("($loggingTag) Updating appearance in $activity")
                manager.update(appearance)
            }
        }
    }

    private fun loadManagerIfNecessary() {
        if (_state.value.embeddedComponentManagerAsync() != null) {
            return
        }
        val manager = embeddedComponentManagerFactory.createEmbeddedComponentManager()
        if (manager != null) {
            _state.update { it.copy(embeddedComponentManagerAsync = Success(manager)) }
            return
        }
        loadManager()
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
                val manager = embeddedComponentManagerFactory.createEmbeddedComponentManager()
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
