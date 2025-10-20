package com.stripe.android.paymentsheet.example.playground.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.godaddy.android.colorpicker.ClassicColorPicker
import com.godaddy.android.colorpicker.HsvColor
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.paymentsheet.example.R
import kotlin.math.roundToInt

private val BASE_FONT_SIZE = 20.sp
private val BASE_PADDING = 8.dp
private val SECTION_LABEL_COLOR = Color(159, 159, 169)
private const val DEFAULT_PRIMARY_BUTTON_HEIGHT = 48f

internal class AppearanceBottomSheetDialogFragment : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppearancePicker(
                    currentAppearance = AppearanceStore.state,
                    updateAppearance = { AppearanceStore.state = it },
                    resetAppearance = ::resetAppearance
                )
            }
        }
    }

    private fun resetAppearance() {
        AppearanceStore.reset()
    }

    companion object {
        fun newInstance(): AppearanceBottomSheetDialogFragment {
            return AppearanceBottomSheetDialogFragment()
        }
    }
}

@Composable
private fun AppearancePicker(
    currentAppearance: AppearanceStore.State,
    updateAppearance: (AppearanceStore.State) -> Unit,
    resetAppearance: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val nestedScrollConnection = rememberNestedScrollInteropConnection()

    Scaffold(
        topBar = {
            AppearanceTopAppBar(resetAppearance)
        },
        modifier = Modifier
            .systemBarsPadding()
            .nestedScroll(nestedScrollConnection),
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            CustomizationCard("Icons") {
                Icons(
                    currentAppearance = currentAppearance,
                    updateAppearance = updateAppearance,
                )
            }
            CustomizationCard("Colors") {
                Colors(
                    currentAppearance = currentAppearance,
                    updateAppearance = updateAppearance,
                )
            }
            CustomizationCard("Shapes") {
                Shapes(
                    currentAppearance = currentAppearance,
                    updateAppearance = updateAppearance,
                )
            }
            CustomizationCard("Form Insets") {
                Insets(
                    currentInsets = currentAppearance.formInsetValues,
                    defaultCustomInsets = AppearanceStore.State.Insets.defaultFormInsets,
                    updateInsets = {
                        updateAppearance(
                            currentAppearance.copy(formInsetValues = it)
                        )
                    },
                )
            }
            CustomizationCard("Text Field Insets") {
                Insets(
                    currentInsets = currentAppearance.textFieldInsets,
                    defaultCustomInsets = AppearanceStore.State.Insets.defaultTextInsets,
                    updateInsets = {
                        updateAppearance(
                            currentAppearance.copy(textFieldInsets = it)
                        )
                    },
                )
            }
            CustomizationCard("Section Spacing") {
                SectionSpacing(
                    currentAppearance = currentAppearance,
                    updateAppearance = updateAppearance,
                )
            }
            CustomizationCard("Typography") {
                Typography(
                    currentAppearance = currentAppearance,
                    updateAppearance = updateAppearance,
                )
            }
            CustomizationCard("PrimaryButton") {
                PrimaryButton(
                    currentAppearance = currentAppearance,
                    updateAppearance = updateAppearance,
                )
            }
            CustomizationCard("Vertical Mode") {
                VerticalMode(
                    currentAppearance = currentAppearance,
                    updateAppearance = updateAppearance,
                )
            }
            CustomizationCard("Embedded") {
                EmbeddedPicker(
                    currentAppearance = currentAppearance,
                    updateAppearance = updateAppearance,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AppearanceTopAppBar(resetAppearance: () -> Unit) {
    TopAppBar(
        title = { Text("Appearance") },
        actions = {
            TextButton(onClick = {
                resetAppearance()
            }) {
                Text(text = stringResource(R.string.reset))
            }
        },
        backgroundColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface,
    )
}

@Composable
private fun CustomizationCard(
    label: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        Text(
            text = label,
            fontSize = 24.sp,
            color = SECTION_LABEL_COLOR,
            modifier = Modifier.padding(vertical = BASE_PADDING)
        )
        Card(
            backgroundColor = Color.White,
            elevation = 2.dp,
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun Icons(
    currentAppearance: AppearanceStore.State,
    updateAppearance: (AppearanceStore.State) -> Unit,
) {
    IconStyleDropDown(
        style = currentAppearance.iconStyle,
    ) {
        updateAppearance(
            currentAppearance.copy(
                iconStyle = it
            )
        )
    }
}

@Composable
private fun Colors(
    currentAppearance: AppearanceStore.State,
    updateAppearance: (AppearanceStore.State) -> Unit,
) {
    ColorItem(
        label = "primary",
        currentColor = Color(currentAppearance.colorsLight.primary),
        onColorPicked = { color ->
            currentAppearance.copy(
                colorsLight = currentAppearance.colorsLight.copy(
                    primary = color.toArgb()
                ),
                colorsDark = currentAppearance.colorsDark.copy(
                    primary = color.toArgb()
                )
            )
        },
        updateAppearance = updateAppearance,
    )
    Divider()
    ColorItem(
        label = "surface",
        currentColor = Color(currentAppearance.colorsLight.surface),
        onColorPicked = {
            currentAppearance.copy(
                colorsLight = currentAppearance.colorsLight.copy(
                    surface = it.toArgb()
                ),
                colorsDark = currentAppearance.colorsDark.copy(
                    surface = it.toArgb()
                )
            )
        },
        updateAppearance = updateAppearance,
    )
    Divider()
    ColorItem(
        label = "component",
        currentColor = Color(currentAppearance.colorsLight.component),
        onColorPicked = {
            currentAppearance.copy(
                colorsLight = currentAppearance.colorsLight.copy(
                    component = it.toArgb()
                ),
                colorsDark = currentAppearance.colorsDark.copy(
                    component = it.toArgb()
                )
            )
        },
        updateAppearance = updateAppearance,
    )
    Divider()
    ColorItem(
        label = "componentBorder",
        currentColor = Color(currentAppearance.colorsLight.componentBorder),
        onColorPicked = {
            currentAppearance.copy(
                colorsLight = currentAppearance.colorsLight.copy(
                    componentBorder = it.toArgb()
                ),
                colorsDark = currentAppearance.colorsDark.copy(
                    componentBorder = it.toArgb()
                )
            )
        },
        updateAppearance = updateAppearance,
    )
    Divider()
    ColorItem(
        label = "componentDivider",
        currentColor = Color(currentAppearance.colorsLight.componentDivider),
        onColorPicked = {
            currentAppearance.copy(
                colorsLight = currentAppearance.colorsLight.copy(
                    componentDivider = it.toArgb()
                ),
                colorsDark = currentAppearance.colorsDark.copy(
                    componentDivider = it.toArgb()
                )
            )
        },
        updateAppearance = updateAppearance,
    )
    Divider()
    ColorItem(
        label = "onComponent",
        currentColor = Color(currentAppearance.colorsLight.onComponent),
        onColorPicked = {
            currentAppearance.copy(
                colorsLight = currentAppearance.colorsLight.copy(
                    onComponent = it.toArgb()
                ),
                colorsDark = currentAppearance.colorsDark.copy(
                    onComponent = it.toArgb()
                )
            )
        },
        updateAppearance = updateAppearance,
    )
    Divider()
    ColorItem(
        label = "onSurface",
        currentColor = Color(currentAppearance.colorsLight.onSurface),
        onColorPicked = {
            currentAppearance.copy(
                colorsLight = currentAppearance.colorsLight.copy(
                    onSurface = it.toArgb()
                ),
                colorsDark = currentAppearance.colorsDark.copy(
                    onSurface = it.toArgb()
                )
            )
        },
        updateAppearance = updateAppearance,
    )
    Divider()
    ColorItem(
        label = "subtitle",
        currentColor = Color(currentAppearance.colorsLight.subtitle),
        onColorPicked = {
            currentAppearance.copy(
                colorsLight = currentAppearance.colorsLight.copy(
                    subtitle = it.toArgb()
                ),
                colorsDark = currentAppearance.colorsDark.copy(
                    subtitle = it.toArgb()
                )
            )
        },
        updateAppearance = updateAppearance,
    )
    Divider()
    ColorItem(
        label = "placeholderText",
        currentColor = Color(currentAppearance.colorsLight.placeholderText),
        onColorPicked = {
            currentAppearance.copy(
                colorsLight = currentAppearance.colorsLight.copy(
                    placeholderText = it.toArgb()
                ),
                colorsDark = currentAppearance.colorsDark.copy(
                    placeholderText = it.toArgb()
                )
            )
        },
        updateAppearance = updateAppearance,
    )
    Divider()
    ColorItem(
        label = "appBarIcon",
        currentColor = Color(currentAppearance.colorsLight.appBarIcon),
        onColorPicked = {
            currentAppearance.copy(
                colorsLight = currentAppearance.colorsLight.copy(
                    appBarIcon = it.toArgb()
                ),
                colorsDark = currentAppearance.colorsDark.copy(
                    appBarIcon = it.toArgb()
                )
            )
        },
        updateAppearance = updateAppearance,
    )
    Divider()
    ColorItem(
        label = "error",
        currentColor = Color(currentAppearance.colorsLight.error),
        onColorPicked = {
            currentAppearance.copy(
                colorsLight = currentAppearance.colorsLight.copy(
                    error = it.toArgb()
                ),
                colorsDark = currentAppearance.colorsDark.copy(
                    error = it.toArgb()
                )
            )
        },
        updateAppearance = updateAppearance,
    )
}

@OptIn(AppearanceAPIAdditionsPreview::class)
@Composable
private fun Shapes(
    currentAppearance: AppearanceStore.State,
    updateAppearance: (AppearanceStore.State) -> Unit,
) {
    IncrementDecrementItem("cornerRadiusDp", currentAppearance.shapes.cornerRadiusDp) {
        updateAppearance(
            currentAppearance.copy(
                shapes = AppearanceStore.State.Shapes(
                    cornerRadiusDp = it,
                    borderStrokeWidthDp = currentAppearance.shapes.borderStrokeWidthDp,
                    bottomSheetCornerRadiusDp = currentAppearance.shapes.bottomSheetCornerRadiusDp
                )
            )
        )
    }
    Divider()
    IncrementDecrementItem("borderStrokeWidthDp", currentAppearance.shapes.borderStrokeWidthDp) {
        updateAppearance(
            currentAppearance.copy(
                shapes = AppearanceStore.State.Shapes(
                    cornerRadiusDp = currentAppearance.shapes.cornerRadiusDp,
                    borderStrokeWidthDp = it,
                    bottomSheetCornerRadiusDp = currentAppearance.shapes.bottomSheetCornerRadiusDp
                )
            )
        )
    }
    Divider()
    IncrementDecrementItem("bottomSheetCornerRadiusDp", currentAppearance.shapes.bottomSheetCornerRadiusDp) {
        updateAppearance(
            currentAppearance.copy(
                shapes = AppearanceStore.State.Shapes(
                    cornerRadiusDp = currentAppearance.shapes.cornerRadiusDp,
                    borderStrokeWidthDp = currentAppearance.shapes.borderStrokeWidthDp,
                    bottomSheetCornerRadiusDp = it,
                )
            )
        )
    }
}

@Composable
private fun Insets(
    currentInsets: AppearanceStore.State.Insets,
    defaultCustomInsets: AppearanceStore.State.Insets.Custom,
    updateInsets: (AppearanceStore.State.Insets) -> Unit,
) {
    AppearanceToggle("Custom", currentInsets is AppearanceStore.State.Insets.Custom) {
        updateInsets(
            if (it) {
                defaultCustomInsets
            } else {
                AppearanceStore.State.Insets.Default
            }
        )
    }

    if (currentInsets is AppearanceStore.State.Insets.Custom) {
        Divider()
        IncrementDecrementItem("start", currentInsets.start) {
            updateInsets(
                currentInsets.copy(
                    start = it
                )
            )
        }
        Divider()
        IncrementDecrementItem("top", currentInsets.top) {
            updateInsets(
                currentInsets.copy(
                    top = it
                )
            )
        }
        Divider()
        IncrementDecrementItem("end", currentInsets.end) {
            updateInsets(
                currentInsets.copy(
                    end = it
                )
            )
        }
        Divider()
        IncrementDecrementItem("bottom", currentInsets.bottom) {
            updateInsets(
                currentInsets.copy(
                    bottom = it
                )
            )
        }
    }
}

@Composable
private fun SectionSpacing(
    currentAppearance: AppearanceStore.State,
    updateAppearance: (AppearanceStore.State) -> Unit,
) {
    val sectionSpacing = currentAppearance.sectionSpacing

    AppearanceToggle("Custom", sectionSpacing is AppearanceStore.State.SectionSpacing.Custom) {
        updateAppearance(
            currentAppearance.copy(
                sectionSpacing = if (it) {
                    AppearanceStore.State.SectionSpacing.Custom()
                } else {
                    AppearanceStore.State.SectionSpacing.Default
                }
            )
        )
    }

    if (sectionSpacing is AppearanceStore.State.SectionSpacing.Custom) {
        Divider()
        IncrementDecrementItem("value", sectionSpacing.spacingDp) {
            updateAppearance(
                currentAppearance.copy(
                    sectionSpacing = sectionSpacing.copy(it)
                )
            )
        }
    }
}

@OptIn(AppearanceAPIAdditionsPreview::class)
@Composable
private fun Typography(
    currentAppearance: AppearanceStore.State,
    updateAppearance: (AppearanceStore.State) -> Unit,
) {
    FontScaleSlider(currentAppearance.typography.sizeScaleFactor) {
        updateAppearance(
            currentAppearance.copy(
                typography = AppearanceStore.State.Typography(
                    sizeScaleFactor = it,
                    fontResId = currentAppearance.typography.fontResId,
                    custom = currentAppearance.typography.custom,
                )
            )
        )
    }
    Divider()
    FontDropDown(currentAppearance.typography.fontResId) {
        updateAppearance(
            currentAppearance.copy(
                typography = AppearanceStore.State.Typography(
                    sizeScaleFactor = currentAppearance.typography.sizeScaleFactor,
                    fontResId = it,
                    custom = currentAppearance.typography.custom,
                )
            )
        )
    }
    Divider()

    val h1 = currentAppearance.typography.custom.h1

    AppearanceToggle("customH1", h1 != null) {
        updateAppearance(
            currentAppearance.copy(
                typography = AppearanceStore.State.Typography(
                    sizeScaleFactor = currentAppearance.typography.sizeScaleFactor,
                    fontResId = currentAppearance.typography.fontResId,
                    custom = AppearanceStore.State.Typography.Custom(
                        h1 = if (it) {
                            AppearanceStore.State.Typography.Font(
                                fontSizeSp = 20f,
                                fontWeight = 400,
                                letterSpacingSp = 0.13f
                            )
                        } else {
                            null
                        }
                    )
                )
            )
        )
    }

    h1?.run {
        Divider()
        FontDropDown(fontFamily) {
            updateAppearance(
                currentAppearance.copy(
                    typography = AppearanceStore.State.Typography(
                        sizeScaleFactor = currentAppearance.typography.sizeScaleFactor,
                        fontResId = currentAppearance.typography.fontResId,
                        custom = AppearanceStore.State.Typography.Custom(
                            h1 = AppearanceStore.State.Typography.Font(
                                fontFamily = it,
                                fontWeight = fontWeight,
                                fontSizeSp = fontSizeSp,
                                letterSpacingSp = letterSpacingSp,
                            )
                        )
                    )
                )
            )
        }
        Divider()
        IncrementDecrementItem(
            label = "fontSizeSp",
            value = fontSizeSp ?: AppearanceStore.State.defaultCustomH1LetterSpacingSp,
        ) {
            updateAppearance(
                currentAppearance.copy(
                    typography = AppearanceStore.State.Typography(
                        sizeScaleFactor = currentAppearance.typography.sizeScaleFactor,
                        fontResId = currentAppearance.typography.fontResId,
                        custom = AppearanceStore.State.Typography.Custom(
                            h1 = AppearanceStore.State.Typography.Font(
                                fontFamily = fontFamily,
                                fontWeight = fontWeight,
                                fontSizeSp = it,
                                letterSpacingSp = letterSpacingSp,
                            )
                        )
                    )
                )
            )
        }
        Divider()
        IncrementDecrementItem(
            label = "fontWeight",
            value = fontWeight?.toFloat() ?: AppearanceStore.State.defaultCustomH1FontSizeDp,
            incrementDecrementAmount = 100f
        ) {
            updateAppearance(
                currentAppearance.copy(
                    typography = AppearanceStore.State.Typography(
                        sizeScaleFactor = currentAppearance.typography.sizeScaleFactor,
                        fontResId = currentAppearance.typography.fontResId,
                        custom = AppearanceStore.State.Typography.Custom(
                            h1 = AppearanceStore.State.Typography.Font(
                                fontFamily = fontFamily,
                                fontWeight = it.roundToInt(),
                                fontSizeSp = fontSizeSp,
                                letterSpacingSp = letterSpacingSp,
                            )
                        )
                    )
                )
            )
        }
        Divider()
        IncrementDecrementItem(
            label = "letterSpacingSp",
            value = letterSpacingSp ?: AppearanceStore.State.defaultCustomH1LetterSpacingSp,
            incrementDecrementAmount = 0.01f
        ) {
            updateAppearance(
                currentAppearance.copy(
                    typography = AppearanceStore.State.Typography(
                        sizeScaleFactor = currentAppearance.typography.sizeScaleFactor,
                        fontResId = currentAppearance.typography.fontResId,
                        custom = AppearanceStore.State.Typography.Custom(
                            h1 = AppearanceStore.State.Typography.Font(
                                fontFamily = fontFamily,
                                fontWeight = fontWeight,
                                fontSizeSp = fontSizeSp,
                                letterSpacingSp = it,
                            )
                        )
                    )
                )
            )
        }
    }
}

@Composable
private fun PrimaryButton(
    currentAppearance: AppearanceStore.State,
    updateAppearance: (AppearanceStore.State) -> Unit,
) {
    val currentButton = currentAppearance.primaryButton

    val currentBackground = currentButton.colorsLight.background?.let {
        Color(it)
    } ?: run {
        Color(currentAppearance.colorsLight.primary)
    }
    ColorItem(
        label = "background",
        currentColor = currentBackground,
        onColorPicked = {
            currentAppearance.copy(
                primaryButton = currentButton.copy(
                    colorsLight = currentButton.colorsLight.copy(
                        background = it.toArgb()
                    ),
                    colorsDark = currentButton.colorsDark.copy(
                        background = it.toArgb()
                    )
                )
            )
        },
        updateAppearance = updateAppearance,
    )

    Divider()

    ColorItem(
        label = "onBackground",
        currentColor = Color(currentButton.colorsLight.onBackground),
        onColorPicked = {
            currentAppearance.copy(
                primaryButton = currentButton.copy(
                    colorsLight = currentButton.colorsLight.copy(
                        onBackground = it.toArgb()
                    ),
                    colorsDark = currentButton.colorsDark.copy(
                        onBackground = it.toArgb()
                    )
                )
            )
        },
        updateAppearance = updateAppearance,
    )

    Divider()
    ColorItem(
        label = "border",
        currentColor = Color(currentButton.colorsLight.border),
        onColorPicked = {
            currentAppearance.copy(
                primaryButton = currentButton.copy(
                    colorsLight = currentButton.colorsLight.copy(
                        border = it.toArgb()
                    ),
                    colorsDark = currentButton.colorsDark.copy(
                        border = it.toArgb()
                    )
                )
            )
        },
        updateAppearance = updateAppearance,
    )

    Divider()

    ColorItem(
        label = "successBackground",
        currentColor = Color(currentButton.colorsLight.successBackgroundColor),
        onColorPicked = {
            currentAppearance.copy(
                primaryButton = currentButton.copy(
                    colorsLight = currentButton.colorsLight.copy(
                        successBackgroundColor = it.toArgb()
                    ),
                    colorsDark = currentButton.colorsDark.copy(
                        successBackgroundColor = it.toArgb()
                    )
                )
            )
        },
        updateAppearance = updateAppearance,
    )

    Divider()

    ColorItem(
        label = "onSuccessBackground",
        currentColor = Color(
            color = currentButton.colorsLight.onSuccessBackgroundColor
                ?: currentButton.colorsLight.onBackground
        ),
        onColorPicked = {
            currentAppearance.copy(
                primaryButton = currentButton.copy(
                    colorsLight = currentButton.colorsLight.copy(
                        onSuccessBackgroundColor = it.toArgb()
                    ),
                    colorsDark = currentButton.colorsDark.copy(
                        onSuccessBackgroundColor = it.toArgb()
                    )
                )
            )
        },
        updateAppearance = updateAppearance,
    )

    val currentCornerRadius = currentButton.shape.cornerRadiusDp
        ?: currentAppearance.shapes.cornerRadiusDp
    Divider()
    IncrementDecrementItem("cornerRadiusDp", currentCornerRadius) {
        updateAppearance(
            currentAppearance.copy(
                primaryButton = currentButton.copy(
                    shape = currentButton.shape.copy(
                        cornerRadiusDp = it
                    )
                )
            )
        )
    }

    val currentBorderStrokeWidth = currentButton.shape.borderStrokeWidthDp
        ?: currentAppearance.shapes.borderStrokeWidthDp
    Divider()
    IncrementDecrementItem("borderStrokeWidthDp", currentBorderStrokeWidth) {
        updateAppearance(
            currentAppearance.copy(
                primaryButton = currentButton.copy(
                    shape = currentButton.shape.copy(
                        borderStrokeWidthDp = it
                    )
                )
            )
        )
    }

    Divider()

    val currentButtonHeight = currentButton.shape.heightDp ?: DEFAULT_PRIMARY_BUTTON_HEIGHT
    IncrementDecrementItem("buttonHeightDp", currentButtonHeight) {
        updateAppearance(
            currentAppearance.copy(
                primaryButton = currentButton.copy(
                    shape = currentButton.shape.copy(
                        heightDp = it
                    )
                )
            )
        )
    }

    val currentFontSize = currentButton.typography.fontSizeSp
        ?: (currentAppearance.typography.sizeScaleFactor * 16)
    Divider()
    IncrementDecrementItem("fontSizeSp", currentFontSize) {
        updateAppearance(
            currentAppearance.copy(
                primaryButton = currentButton.copy(
                    typography = currentButton.typography.copy(
                        fontSizeSp = it
                    )
                )
            )
        )
    }

    val currentFontFamily = currentButton.typography.fontResId
        ?: currentAppearance.typography.fontResId
    Divider()
    FontDropDown(currentFontFamily) {
        updateAppearance(
            currentAppearance.copy(
                primaryButton = currentButton.copy(
                    typography = currentButton.typography.copy(
                        fontResId = it
                    )
                )
            )
        )
    }
}

@Composable
private fun VerticalMode(
    currentAppearance: AppearanceStore.State,
    updateAppearance: (AppearanceStore.State) -> Unit,
) {
    IncrementDecrementItem("verticalModeRowPaddingDp", currentAppearance.verticalModeRowPadding) {
        updateAppearance(
            currentAppearance.copy(
                verticalModeRowPadding = it
            )
        )
    }
}

@Composable
private fun EmbeddedPicker(
    currentAppearance: AppearanceStore.State,
    updateAppearance: (AppearanceStore.State) -> Unit
) {
    val embeddedAppearance = currentAppearance.embedded
    val updateEmbedded: (AppearanceStore.State.Embedded) -> Unit = { updatedEmbedded ->
        updateAppearance(
            currentAppearance.copy(
                embedded = updatedEmbedded
            )
        )
    }

    RowStyleDropDown(embeddedAppearance.embeddedRowStyle) { style ->
        updateEmbedded(
            embeddedAppearance.copy(
                embeddedRowStyle = style
            )
        )
    }
    ColorItem(
        label = "separatorColor",
        currentColor = Color(embeddedAppearance.separatorColor),
        onColorPicked = {
            embeddedAppearance.copy(
                separatorColor = it.toArgb()
            )
        },
        updateAppearance = updateEmbedded,
    )
    Divider()

    ColorItem(
        label = "selectedColor",
        currentColor = Color(embeddedAppearance.selectedColor),
        onColorPicked = {
            embeddedAppearance.copy(
                selectedColor = it.toArgb()
            )
        },
        updateAppearance = updateEmbedded,
    )
    Divider()

    ColorItem(
        label = "unselectedColor",
        currentColor = Color(embeddedAppearance.unselectedColor),
        onColorPicked = {
            embeddedAppearance.copy(
                unselectedColor = it.toArgb()
            )
        },
        updateAppearance = updateEmbedded,
    )
    Divider()

    ColorItem(
        label = "checkmarkColor",
        currentColor = Color(embeddedAppearance.checkmarkColor),
        onColorPicked = {
            embeddedAppearance.copy(
                checkmarkColor = it.toArgb()
            )
        },
        updateAppearance = updateEmbedded,
    )
    Divider()

    ColorItem(
        label = "disclosureColor",
        currentColor = Color(embeddedAppearance.disclosureColor),
        onColorPicked = {
            embeddedAppearance.copy(
                disclosureColor = it.toArgb()
            )
        },
        updateAppearance = updateEmbedded,
    )
    Divider()

    IncrementDecrementItem("startSeparatorInsetDp", embeddedAppearance.startSeparatorInset) {
        updateEmbedded(
            embeddedAppearance.copy(
                startSeparatorInset = it
            )
        )
    }
    Divider()

    IncrementDecrementItem("endSeparatorInsetDp", embeddedAppearance.endSeparatorInset) {
        updateEmbedded(
            embeddedAppearance.copy(
                endSeparatorInset = it
            )
        )
    }
    Divider()

    IncrementDecrementItem("separatorThicknessDp", embeddedAppearance.separatorThicknessDp) {
        updateEmbedded(
            embeddedAppearance.copy(
                separatorThicknessDp = it
            )
        )
    }
    Divider()

    IncrementDecrementItem("additionalVerticalInsetsDp", embeddedAppearance.additionalVerticalInsetsDp) {
        updateEmbedded(
            embeddedAppearance.copy(
                additionalVerticalInsetsDp = it
            )
        )
    }
    Divider()

    IncrementDecrementItem("horizontalInsetsDp", embeddedAppearance.horizontalInsetsDp) {
        updateEmbedded(
            embeddedAppearance.copy(
                horizontalInsetsDp = it
            )
        )
    }
    Divider()

    IncrementDecrementItem("checkmarkInsetsDp", embeddedAppearance.checkmarkInsetsDp) {
        updateEmbedded(
            embeddedAppearance.copy(
                checkmarkInsetsDp = it
            )
        )
    }
    Divider()

    IncrementDecrementItem("floatingButtonSpacingDp", embeddedAppearance.floatingButtonSpacingDp) {
        updateEmbedded(
            embeddedAppearance.copy(
                floatingButtonSpacingDp = it
            )
        )
    }
    Divider()

    AppearanceToggle("topSeparatorEnabled", embeddedAppearance.topSeparatorEnabled) {
        updateEmbedded(
            embeddedAppearance.copy(
                topSeparatorEnabled = it
            )
        )
    }
    Divider()

    AppearanceToggle("bottomSeparatorEnabled", embeddedAppearance.bottomSeparatorEnabled) {
        updateEmbedded(
            embeddedAppearance.copy(
                bottomSeparatorEnabled = it
            )
        )
    }
    Divider()

    IncrementDecrementItem(
        "verticalIconMargin",
        embeddedAppearance.verticalPaymentMethodIconMargin ?: 0f
    ) {
        updateEmbedded(
            embeddedAppearance.copy(
                verticalPaymentMethodIconMargin = it
            )
        )
    }
    Divider()

    IncrementDecrementItem(
        "horizontalIconMargin",
        embeddedAppearance.horizontalPaymentMethodIconMargin ?: 0f
    ) {
        updateEmbedded(
            embeddedAppearance.copy(
                horizontalPaymentMethodIconMargin = it
            )
        )
    }
    Divider()

    EmbeddedFontDropDown(embeddedAppearance.titleFont, "titleFont") {
        updateEmbedded(
            embeddedAppearance.copy(
                titleFont = it
            )
        )
    }
    Divider()

    EmbeddedFontDropDown(embeddedAppearance.subtitleFont, "subtitleFont") {
        updateEmbedded(
            embeddedAppearance.copy(
                subtitleFont = it
            )
        )
    }
    Divider()

    IconDropDown(embeddedAppearance.disclosureIconRes) {
        updateEmbedded(
            embeddedAppearance.copy(
                disclosureIconRes = it
            )
        )
    }
}

@Composable
private fun <T> ColorItem(
    label: String,
    currentColor: Color,
    onColorPicked: (Color) -> T,
    updateAppearance: (T) -> Unit,
) {
    val openDialog = remember { mutableStateOf(false) }
    ColorPicker(openDialog, currentColor) {
        updateAppearance(onColorPicked(it))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = BASE_PADDING)
            .clickable { openDialog.value = true },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = BASE_FONT_SIZE)
        ColorIcon(innerColor = currentColor)
    }
}

@Composable
private fun ColorPicker(
    openDialog: MutableState<Boolean>,
    defaultColor: Color,
    onClose: (Color) -> Unit,
) {
    val currentColor = remember { mutableStateOf(defaultColor) }
    if (openDialog.value) {
        AlertDialog(
            onDismissRequest = {
                openDialog.value = false
                onClose(currentColor.value)
            },
            text = {
                ClassicColorPicker(
                    modifier = Modifier.fillMaxSize(),
                    color = HsvColor.from(currentColor.value),
                    onColorChanged = {
                        currentColor.value = it.toColor()
                    }
                )
            },
            buttons = {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = BASE_PADDING),
                    colors = ButtonDefaults.buttonColors(backgroundColor = currentColor.value),
                    onClick = {
                        openDialog.value = false
                        onClose(currentColor.value)
                    }
                ) {
                    Text(text = "Pick Color")
                }
            }
        )
    }
}

@Composable
private fun ColorIcon(innerColor: Color) {
    val brush = Brush.verticalGradient(
        listOf(Color.Red, Color.Yellow, Color.Green, Color.Blue, Color.Magenta)
    )
    Box(
        modifier = Modifier
            .border(BorderStroke(3.dp, brush), CircleShape)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.Transparent)
        )
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(innerColor)
                .align(Alignment.Center)
        )
    }
}

@Composable
private fun IncrementDecrementItem(
    label: String,
    value: Float,
    incrementDecrementAmount: Float = 1f,
    onValueChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = BASE_PADDING),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "$label: $value", fontSize = BASE_FONT_SIZE)
        Row {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .height(32.dp)
                    .width(50.dp)
                    .clickable {
                        val newValue = value - incrementDecrementAmount
                        onValueChange(if (newValue < 0) 0.0f else newValue)
                    }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = null,
                )
            }
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .height(32.dp)
                    .width(50.dp)
                    .clickable {
                        onValueChange(value + incrementDecrementAmount)
                    }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun AppearanceToggle(label: String, value: Boolean, onValueChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = BASE_PADDING),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "$label: $value", fontSize = BASE_FONT_SIZE)
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            checked = value,
            onCheckedChange = {
                onValueChange(it)
            }
        )
    }
}

@Composable
private fun FontScaleSlider(sliderPosition: Float, onValueChange: (Float) -> Unit) {
    Text(
        text = "sizeScaleFactor: $sliderPosition",
        fontSize = BASE_FONT_SIZE,
        modifier = Modifier.padding(horizontal = BASE_PADDING)
    )
    Slider(
        value = sliderPosition,
        valueRange = 0f..2f,
        onValueChange = {
            onValueChange(it)
        }
    )
}

@Composable
private fun IconStyleDropDown(
    style: AppearanceStore.State.IconStyle,
    onIconStyleSelected: (AppearanceStore.State.IconStyle) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxSize()
            .padding(all = BASE_PADDING)
            .wrapContentSize(Alignment.TopStart)
    ) {
        Text(
            text = "IconStyle: $style",
            fontSize = BASE_FONT_SIZE,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { expanded = true })
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            AppearanceStore.State.IconStyle.entries.forEach {
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onIconStyleSelected(it)
                    }
                ) {
                    Text(it.name)
                }
            }
        }
    }
}

@Composable
private fun RowStyleDropDown(
    style: AppearanceStore.State.Embedded.Row,
    rowStyleSelectedCallback: (AppearanceStore.State.Embedded.Row) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxSize()
            .padding(all = BASE_PADDING)
            .wrapContentSize(Alignment.TopStart)
    ) {
        Text(
            text = "RowStyle: $style",
            fontSize = BASE_FONT_SIZE,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { expanded = true })
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            AppearanceStore.State.Embedded.Row.entries.forEach {
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        rowStyleSelectedCallback(it)
                    }
                ) {
                    Text(it.name)
                }
            }
        }
    }
}

@Composable
private fun FontDropDown(fontResId: Int?, fontSelectedCallback: (Int?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val items = mapOf(
        R.font.cursive to "Cursive",
        R.font.opensans to "OpenSans",
        null to "Default"
    )

    items[fontResId]?.let {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(all = BASE_PADDING)
                .wrapContentSize(Alignment.TopStart)
        ) {
            Text(
                text = "Font Resource: $it",
                fontFamily = getFontFromResource(fontResId),
                fontSize = BASE_FONT_SIZE,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { expanded = true })
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                items.forEach { font ->
                    FontDropDownMenuItem(label = font.value, fontResId = font.key) {
                        expanded = false
                        fontSelectedCallback(font.key)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmbeddedFontDropDown(
    currentFont: AppearanceStore.State.Typography.Font?,
    displayText: String,
    fontSelectedCallback: (AppearanceStore.State.Typography.Font?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val items = mapOf(
        AppearanceStore.State.Typography.Font(
            fontSizeSp = 12f,
            fontWeight = 200,
            letterSpacingSp = 8f,
        ) to "Small",
        AppearanceStore.State.Typography.Font(
            fontSizeSp = 16f,
            fontWeight = 400,
            letterSpacingSp = 8f,
        ) to "Medium",
        AppearanceStore.State.Typography.Font(
            fontSizeSp = 24f,
            fontWeight = 700,
            letterSpacingSp = 8f,
        ) to "Large",
        null to "Default"
    )

    items[currentFont]?.let {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize().padding(all = BASE_PADDING).wrapContentSize(Alignment.TopStart)
        ) {
            Text(
                text = "$displayText: $it",
                fontSize = BASE_FONT_SIZE,
                modifier = Modifier.fillMaxWidth().clickable(onClick = { expanded = true })
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                items.forEach { font ->
                    val style = TextStyle(
                        fontSize = font.key?.fontSizeSp?.sp ?: TextUnit.Unspecified,
                        fontWeight = font.key?.fontWeight?.let { FontWeight(it) },
                        fontFamily = font.key?.fontFamily?.let { FontFamily(Font(it)) },
                        letterSpacing = font.key?.letterSpacingSp?.sp ?: TextUnit.Unspecified,
                    )
                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            fontSelectedCallback(font.key)
                        },
                    ) {
                        Text(
                            text = font.value,
                            style = style
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FontDropDownMenuItem(label: String, fontResId: Int?, onClick: () -> Unit) {
    DropdownMenuItem(
        onClick = onClick,
    ) {
        Text(
            text = label,
            fontFamily = getFontFromResource(fontResId)
        )
    }
}

@Composable
private fun IconDropDown(iconResId: Int?, iconSelectedCallback: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val items = mapOf(
        com.stripe.android.R.drawable.stripe_ic_arrow_down to "Down",
        com.stripe.android.R.drawable.stripe_ic_add_black_32dp to "Add",
        com.stripe.android.paymentsheet.R.drawable.stripe_ic_chevron_right to "Default"
    )

    items[iconResId].let {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(all = BASE_PADDING)
                .wrapContentSize(Alignment.TopStart)
        ) {
            Text(
                text = "Icon Resource: $it",
                fontSize = BASE_FONT_SIZE,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { expanded = true })
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                items.forEach { icon ->
                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            iconSelectedCallback(icon.key)
                        }
                    ) {
                        Text(
                            text = icon.value
                        )
                        icon.key?.let { iconResId ->
                            Icon(
                                painter = painterResource(iconResId),
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getFontFromResource(fontResId: Int?): FontFamily {
    return fontResId?.let {
        FontFamily(Font(it))
    } ?: run {
        FontFamily.Default
    }
}
