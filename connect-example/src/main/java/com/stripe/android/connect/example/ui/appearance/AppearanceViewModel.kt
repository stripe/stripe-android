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
        _state.update {
            it.copy(
                selectedAppearance = appearanceId,
                customThemeOverrides = CustomThemeOverridesState(),
                saveEnabled = true,
            )
        }
    }

    fun onOverridesChanged(overrides: CustomThemeOverridesState) {
        _state.update { it.copy(customThemeOverrides = overrides, saveEnabled = true) }
    }

    fun clearOverrides() {
        _state.update { it.copy(customThemeOverrides = CustomThemeOverridesState(), saveEnabled = true) }
    }

    fun saveAppearance() {
        viewModelScope.launch {
            with(state.value) {
                settingsService.setAppearanceId(selectedAppearance)
                settingsService.setCustomThemeOverrides(customThemeOverrides.toCustomThemeOverrides())
            }
            logger.info("($loggingTag) Appearance saved")
            _state.update { it.copy(saveEnabled = false) }
        }
    }

    // Private functions

    private fun loadAppearance() {
        _state.update { state ->
            val appearanceId = settingsService.getAppearanceId() ?: AppearanceInfo.AppearanceId.Default
            val savedOverrides = settingsService.getCustomThemeOverrides()
            state.copy(
                selectedAppearance = appearanceId,
                customThemeOverrides = CustomThemeOverridesState.fromCustomThemeOverrides(savedOverrides),
                saveEnabled = false,
            )
        }
    }

    // State

    data class AppearanceState(
        val saveEnabled: Boolean = false,
        val selectedAppearance: AppearanceInfo.AppearanceId = AppearanceInfo.AppearanceId.Default,
        val appearances: List<AppearanceInfo.AppearanceId> = AppearanceInfo.AppearanceId.entries,
        val customThemeOverrides: CustomThemeOverridesState = CustomThemeOverridesState(),
    )
}

/**
 * State for the new token overrides form. String fields are empty when not set.
 * TextTransform defaults to None.
 * Color inputs are stored as hex strings (e.g. "FF0000" or "FFFF0000").
 */
@Suppress("LongParameterList")
data class CustomThemeOverridesState(
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
    // Spacing
    val spacingUnit: String = "",
) {
    @Suppress("LongMethod")
    fun toCustomThemeOverrides(): CustomThemeOverrides {
        return CustomThemeOverrides(
            buttonLabelTextTransform = buttonLabelTextTransform,
            buttonLabelFontWeight = buttonLabelFontWeight.toIntOrNull(),
            buttonLabelFontSize = buttonLabelFontSize.toFloatOrNull(),
            buttonPaddingY = buttonPaddingY.toFloatOrNull(),
            buttonPaddingX = buttonPaddingX.toFloatOrNull(),
            buttonDangerColorBackground = buttonDangerColorBackground.parseColor(),
            buttonDangerColorBorder = buttonDangerColorBorder.parseColor(),
            buttonDangerColorText = buttonDangerColorText.parseColor(),
            badgeLabelTextTransform = badgeLabelTextTransform,
            badgeLabelFontWeight = badgeLabelFontWeight.toIntOrNull(),
            badgeLabelFontSize = badgeLabelFontSize.toFloatOrNull(),
            badgePaddingY = badgePaddingY.toFloatOrNull(),
            badgePaddingX = badgePaddingX.toFloatOrNull(),
            actionPrimaryTextTransform = actionPrimaryTextTransform,
            actionSecondaryTextTransform = actionSecondaryTextTransform,
            formPlaceholderTextColor = formPlaceholderTextColor.parseColor(),
            inputFieldPaddingX = inputFieldPaddingX.toFloatOrNull(),
            inputFieldPaddingY = inputFieldPaddingY.toFloatOrNull(),
            tableRowPaddingY = tableRowPaddingY.toFloatOrNull(),
            spacingUnit = spacingUnit.toFloatOrNull(),
        )
    }

    companion object {
        fun fromCustomThemeOverrides(overrides: CustomThemeOverrides): CustomThemeOverridesState {
            return CustomThemeOverridesState(
                buttonLabelTextTransform = overrides.buttonLabelTextTransform.orNone(),
                buttonLabelFontWeight = overrides.buttonLabelFontWeight.toStringOrEmpty(),
                buttonLabelFontSize = overrides.buttonLabelFontSize.toStringOrEmpty(),
                buttonPaddingY = overrides.buttonPaddingY.toStringOrEmpty(),
                buttonPaddingX = overrides.buttonPaddingX.toStringOrEmpty(),
                buttonDangerColorBackground = overrides.buttonDangerColorBackground.toHexOrEmpty(),
                buttonDangerColorBorder = overrides.buttonDangerColorBorder.toHexOrEmpty(),
                buttonDangerColorText = overrides.buttonDangerColorText.toHexOrEmpty(),
                badgeLabelTextTransform = overrides.badgeLabelTextTransform.orNone(),
                badgeLabelFontWeight = overrides.badgeLabelFontWeight.toStringOrEmpty(),
                badgeLabelFontSize = overrides.badgeLabelFontSize.toStringOrEmpty(),
                badgePaddingY = overrides.badgePaddingY.toStringOrEmpty(),
                badgePaddingX = overrides.badgePaddingX.toStringOrEmpty(),
                actionPrimaryTextTransform = overrides.actionPrimaryTextTransform.orNone(),
                actionSecondaryTextTransform = overrides.actionSecondaryTextTransform.orNone(),
                formPlaceholderTextColor = overrides.formPlaceholderTextColor.toHexOrEmpty(),
                inputFieldPaddingX = overrides.inputFieldPaddingX.toStringOrEmpty(),
                inputFieldPaddingY = overrides.inputFieldPaddingY.toStringOrEmpty(),
                tableRowPaddingY = overrides.tableRowPaddingY.toStringOrEmpty(),
                spacingUnit = overrides.spacingUnit.toStringOrEmpty(),
            )
        }
    }
}

private fun String.parseColor(): Int? {
    if (isEmpty()) return null
    return try {
        val hex = if (startsWith("#")) this else "#$this"
        android.graphics.Color.parseColor(hex)
    } catch (_: IllegalArgumentException) {
        null
    }
}

private fun toHexString(colorInt: Int): String = String.format("%08X", colorInt)

private fun TextTransform?.orNone(): TextTransform = this ?: TextTransform.None

private fun Any?.toStringOrEmpty(): String = this?.toString() ?: ""

private fun Int?.toHexOrEmpty(): String = this?.let { toHexString(it) } ?: ""
