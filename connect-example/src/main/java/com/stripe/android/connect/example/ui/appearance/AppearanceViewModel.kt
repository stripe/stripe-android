package com.stripe.android.connect.example.ui.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.connect.BuildConfig
import com.stripe.android.connect.example.data.SettingsService
import com.stripe.android.core.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val settingsService: SettingsService,
) : ViewModel() {

    private val logger: Logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG)
    private val loggingTag = this::class.java.name

    private val _state = MutableStateFlow(AppearanceState())
    val state = _state.asStateFlow()

    init {
        loadAppearance()

        viewModelScope.launch {
            // when the appearance changes, enable the save button
            _state.map { it.selectedAppearance }
                .distinctUntilChanged()
                .collect {
                    _state.update { it.copy(saveEnabled = true) }
                }
        }
    }

    fun onAppearanceSelected(appearanceId: AppearanceInfo.AppearanceId) {
        _state.update { it.copy(selectedAppearance = appearanceId) }
    }

    fun saveAppearance() {
        viewModelScope.launch {
            with(state.value) {
                settingsService.setAppearanceId(selectedAppearance)
            }
            logger.info("($loggingTag) Appearance saved")

            _state.update { it.copy(saveEnabled = false) }
        }
    }

    // Private functions

    private fun loadAppearance() {
        _state.update { state ->
            val appearance = settingsService.getAppearanceId()
            state.copy(
                selectedAppearance = appearance ?: AppearanceInfo.AppearanceId.Default,
                saveEnabled = false,
            )
        }
    }

    // State

    data class AppearanceState(
        val saveEnabled: Boolean = false,
        val selectedAppearance: AppearanceInfo.AppearanceId = AppearanceInfo.AppearanceId.Default,
        val appearances: List<AppearanceInfo.AppearanceId> = AppearanceInfo.AppearanceId.entries,
    )
}
