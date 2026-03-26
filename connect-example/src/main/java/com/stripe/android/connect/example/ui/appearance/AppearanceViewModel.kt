package com.stripe.android.connect.example.ui.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.connect.appearance.TextTransform
import com.stripe.android.connect.example.data.SettingsService
import com.stripe.android.core.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val settingsService: SettingsService,
    private val logger: Logger,
) : ViewModel() {
    private val loggingTag = this::class.java.simpleName

    private val _state = MutableStateFlow(AppearanceState())
    val state = _state.asStateFlow()

    init {
        loadAppearance()
    }

    fun onAppearanceSelected(appearanceId: AppearanceInfo.AppearanceId) {
        _state.update { it.copy(selectedAppearance = appearanceId, saveEnabled = true) }
    }

    fun onOverridesChanged(overrides: NewTokenOverridesUiState) {
        _state.update { it.copy(tokenOverrides = overrides, saveEnabled = true) }
    }

    fun clearOverrides() {
        _state.update { it.copy(tokenOverrides = NewTokenOverridesUiState(), saveEnabled = true) }
    }

    fun saveAppearance() {
        viewModelScope.launch {
            with(state.value) {
                settingsService.setAppearanceId(selectedAppearance)
                settingsService.setNewTokenOverrides(tokenOverrides.toNewTokenOverrides())
            }
            logger.info("($loggingTag) Appearance saved")
            _state.update { it.copy(saveEnabled = false) }
        }
    }

    // Private functions

    private fun loadAppearance() {
        _state.update { state ->
            val appearance = settingsService.getAppearanceId()
            val overrides = settingsService.getNewTokenOverrides()
            state.copy(
                selectedAppearance = appearance ?: AppearanceInfo.AppearanceId.Default,
                tokenOverrides = NewTokenOverridesUiState.fromNewTokenOverrides(overrides),
                saveEnabled = false,
            )
        }
    }

    // State

    data class AppearanceState(
        val saveEnabled: Boolean = false,
        val selectedAppearance: AppearanceInfo.AppearanceId = AppearanceInfo.AppearanceId.Default,
        val appearances: List<AppearanceInfo.AppearanceId> = AppearanceInfo.AppearanceId.entries,
        val tokenOverrides: NewTokenOverridesUiState = NewTokenOverridesUiState(),
    )
}

/**
 * UI state for the new token overrides form. String fields are empty when not set.
 * TextTransform fields default to None; None maps to null (no override) in [toNewTokenOverrides].
 * Color fields are stored as hex strings (e.g. "FF0000" or "FFFF0000").
 */
@Suppress("LongParameterList")
data class NewTokenOverridesUiState(
    // Button
    val buttonLabelTextTransform: TextTransform = TextTransform.None,
    val buttonLabelFontWeight: String = "",
    val buttonLabelFontSize: String = "",
    val buttonPaddingY: String = "",
    val buttonPaddingX: String = "",
    val buttonDangerColorBackground: String = "",
    val buttonDangerColorBorder: String = "",
    val buttonDangerColorText: String = "",
    // Badge
    val badgeLabelTextTransform: TextTransform = TextTransform.None,
    val badgeLabelFontWeight: String = "",
    val badgeLabelFontSize: String = "",
    val badgePaddingY: String = "",
    val badgePaddingX: String = "",
    // Action
    val actionPrimaryTextTransform: TextTransform = TextTransform.None,
    val actionSecondaryTextTransform: TextTransform = TextTransform.None,
    // Form
    val formPlaceholderTextColor: String = "",
    val inputFieldPaddingX: String = "",
    val inputFieldPaddingY: String = "",
    // Table
    val tableRowPaddingY: String = "",
) {
    @Suppress("LongMethod")
    fun toNewTokenOverrides(): NewTokenOverrides {
        return NewTokenOverrides(
            buttonLabelTextTransform = buttonLabelTextTransform.takeUnlessNone(),
            buttonLabelFontWeight = buttonLabelFontWeight.toIntOrNull(),
            buttonLabelFontSize = buttonLabelFontSize.toFloatOrNull(),
            buttonPaddingY = buttonPaddingY.toFloatOrNull(),
            buttonPaddingX = buttonPaddingX.toFloatOrNull(),
            buttonDangerColorBackground = buttonDangerColorBackground.parseColor(),
            buttonDangerColorBorder = buttonDangerColorBorder.parseColor(),
            buttonDangerColorText = buttonDangerColorText.parseColor(),
            badgeLabelTextTransform = badgeLabelTextTransform.takeUnlessNone(),
            badgeLabelFontWeight = badgeLabelFontWeight.toIntOrNull(),
            badgeLabelFontSize = badgeLabelFontSize.toFloatOrNull(),
            badgePaddingY = badgePaddingY.toFloatOrNull(),
            badgePaddingX = badgePaddingX.toFloatOrNull(),
            actionPrimaryTextTransform = actionPrimaryTextTransform.takeUnlessNone(),
            actionSecondaryTextTransform = actionSecondaryTextTransform.takeUnlessNone(),
            formPlaceholderTextColor = formPlaceholderTextColor.parseColor(),
            inputFieldPaddingX = inputFieldPaddingX.toFloatOrNull(),
            inputFieldPaddingY = inputFieldPaddingY.toFloatOrNull(),
            tableRowPaddingY = tableRowPaddingY.toFloatOrNull(),
        )
    }

    companion object {
        @Suppress("LongMethod")
        fun fromNewTokenOverrides(overrides: NewTokenOverrides): NewTokenOverridesUiState {
            return NewTokenOverridesUiState(
                buttonLabelTextTransform = overrides.buttonLabelTextTransform ?: TextTransform.None,
                buttonLabelFontWeight = overrides.buttonLabelFontWeight?.toString() ?: "",
                buttonLabelFontSize = overrides.buttonLabelFontSize?.toString() ?: "",
                buttonPaddingY = overrides.buttonPaddingY?.toString() ?: "",
                buttonPaddingX = overrides.buttonPaddingX?.toString() ?: "",
                buttonDangerColorBackground = overrides.buttonDangerColorBackground?.let { toHexString(it) } ?: "",
                buttonDangerColorBorder = overrides.buttonDangerColorBorder?.let { toHexString(it) } ?: "",
                buttonDangerColorText = overrides.buttonDangerColorText?.let { toHexString(it) } ?: "",
                badgeLabelTextTransform = overrides.badgeLabelTextTransform ?: TextTransform.None,
                badgeLabelFontWeight = overrides.badgeLabelFontWeight?.toString() ?: "",
                badgeLabelFontSize = overrides.badgeLabelFontSize?.toString() ?: "",
                badgePaddingY = overrides.badgePaddingY?.toString() ?: "",
                badgePaddingX = overrides.badgePaddingX?.toString() ?: "",
                actionPrimaryTextTransform = overrides.actionPrimaryTextTransform ?: TextTransform.None,
                actionSecondaryTextTransform = overrides.actionSecondaryTextTransform ?: TextTransform.None,
                formPlaceholderTextColor = overrides.formPlaceholderTextColor?.let { toHexString(it) } ?: "",
                inputFieldPaddingX = overrides.inputFieldPaddingX?.toString() ?: "",
                inputFieldPaddingY = overrides.inputFieldPaddingY?.toString() ?: "",
                tableRowPaddingY = overrides.tableRowPaddingY?.toString() ?: "",
            )
        }

        private fun toHexString(colorInt: Int): String = String.format("%08X", colorInt)
    }
}

private fun TextTransform.takeUnlessNone(): TextTransform? =
    takeUnless { it == TextTransform.None }

private fun String.parseColor(): Int? {
    if (isEmpty()) return null
    return try {
        val hex = if (startsWith("#")) this else "#$this"
        android.graphics.Color.parseColor(hex)
    } catch (e: IllegalArgumentException) {
        null
    }
}
