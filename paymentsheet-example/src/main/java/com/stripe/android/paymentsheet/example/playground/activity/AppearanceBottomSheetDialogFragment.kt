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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.godaddy.android.colorpicker.ClassicColorPicker
import com.godaddy.android.colorpicker.HsvColor
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.uicore.StripeThemeDefaults

private val BASE_FONT_SIZE = 20.sp
private val BASE_PADDING = 8.dp
private val SECTION_LABEL_COLOR = Color(159, 159, 169)

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
                )
            }
        }
    }

    companion object {
        fun newInstance(): AppearanceBottomSheetDialogFragment {
            return AppearanceBottomSheetDialogFragment()
        }
    }
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@Composable
private fun AppearancePicker(
    currentAppearance: PaymentSheet.Appearance,
    updateAppearance: (PaymentSheet.Appearance) -> Unit,
) {
    val scrollState = rememberScrollState()
    val nestedScrollConnection = rememberNestedScrollInteropConnection()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance") },
                actions = {
                    TextButton(onClick = AppearanceStore::reset) {
                        Text(text = stringResource(R.string.reset))
                    }
                },
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.onSurface,
            )
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
            CustomizationCard("Embedded") {
                RowStyleDropDown(currentAppearance.getEmbedded().getRowStyle()) { style ->
                    updateAppearance(currentAppearance.copy(embeddedAppearance = Embedded(style)))
                }
                Divider()
                when (currentAppearance.getEmbedded().getRowStyle()) {
                    is Embedded.RowStyle.FloatingButton ->
                        FloatingButton(
                            currentAppearance = currentAppearance,
                            updateAppearance = updateAppearance,
                        )
                    is Embedded.RowStyle.FlatWithCheckmark ->
                        FlatWithCheckmark(
                            currentAppearance = currentAppearance,
                            updateAppearance = updateAppearance,
                        )
                    is Embedded.RowStyle.FlatWithRadio ->
                        FlatWithRadio(
                            currentAppearance = currentAppearance,
                            updateAppearance = updateAppearance,
                        )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
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
private fun Colors(
    currentAppearance: PaymentSheet.Appearance,
    updateAppearance: (PaymentSheet.Appearance) -> Unit,
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

@Composable
private fun Shapes(
    currentAppearance: PaymentSheet.Appearance,
    updateAppearance: (PaymentSheet.Appearance) -> Unit,
) {
    IncrementDecrementItem("cornerRadiusDp", currentAppearance.shapes.cornerRadiusDp) {
        updateAppearance(
            currentAppearance.copy(
                shapes = currentAppearance.shapes.copy(
                    cornerRadiusDp = it
                )
            )
        )
    }
    Divider()
    IncrementDecrementItem("borderStrokeWidthDp", currentAppearance.shapes.borderStrokeWidthDp) {
        updateAppearance(
            currentAppearance.copy(
                shapes = currentAppearance.shapes.copy(
                    borderStrokeWidthDp = it
                )
            )
        )
    }
}

@Composable
private fun Typography(
    currentAppearance: PaymentSheet.Appearance,
    updateAppearance: (PaymentSheet.Appearance) -> Unit,
) {
    FontScaleSlider(currentAppearance.typography.sizeScaleFactor) {
        updateAppearance(
            currentAppearance.copy(
                typography = currentAppearance.typography.copy(
                    sizeScaleFactor = it
                )
            )
        )
    }
    Divider()
    FontDropDown(currentAppearance.typography.fontResId) {
        updateAppearance(
            currentAppearance.copy(
                typography = currentAppearance.typography.copy(
                    fontResId = it
                )
            )
        )
    }
}

@Composable
private fun PrimaryButton(
    currentAppearance: PaymentSheet.Appearance,
    updateAppearance: (PaymentSheet.Appearance) -> Unit,
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

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@Composable
private fun FlatWithRadio(
    currentAppearance: PaymentSheet.Appearance,
    updateAppearance: (PaymentSheet.Appearance) -> Unit,
) {
    val currentEmbeddedRowStyle = currentAppearance.getEmbedded().getRowStyle() as Embedded.RowStyle.FlatWithRadio

    ColorItem(
        label = "separatorColor",
        currentColor = Color(currentEmbeddedRowStyle.separatorColor),
        onColorPicked = {
            currentAppearance.copy(
                embeddedAppearance = Embedded(
                    updateFlatWithRadio(
                        current = currentEmbeddedRowStyle,
                        separatorColor = it.toArgb()
                    )
                )
            )
        },
        updateAppearance = updateAppearance,
    )
    Divider()

    ColorItem(
        label = "selectedColor",
        currentColor = Color(currentEmbeddedRowStyle.selectedColor),
        onColorPicked = {
            currentAppearance.copy(
                embeddedAppearance = Embedded(
                    updateFlatWithRadio(
                        current = currentEmbeddedRowStyle,
                        selectedColor = it.toArgb()
                    )
                )
            )
        },
        updateAppearance = updateAppearance,
    )
    Divider()

    ColorItem(
        label = "unselectedColor",
        currentColor = Color(currentEmbeddedRowStyle.unselectedColor),
        onColorPicked = {
            currentAppearance.copy(
                embeddedAppearance = Embedded(
                    updateFlatWithRadio(
                        current = currentEmbeddedRowStyle,
                        unselectedColor = it.toArgb()
                    )
                )
            )
        },
        updateAppearance = updateAppearance,
    )

    IncrementDecrementItem("separatorInsetsDp", currentEmbeddedRowStyle.separatorInsetsDp) {
        updateAppearance(
            currentAppearance.copy(
                embeddedAppearance = Embedded(
                    updateFlatWithRadio(
                        current = currentEmbeddedRowStyle,
                        separatorInsetsDp = it
                    )
                )
            )
        )
    }
    Divider()

    IncrementDecrementItem("separatorThicknessDp", currentEmbeddedRowStyle.separatorThicknessDp) {
        updateAppearance(
            currentAppearance.copy(
                embeddedAppearance = Embedded(
                    updateFlatWithRadio(
                        current = currentEmbeddedRowStyle,
                        separatorThicknessDp = it
                    )
                )
            )
        )
    }
    Divider()

    IncrementDecrementItem("additionalInsets", currentEmbeddedRowStyle.additionalInsetsDp) {
        updateAppearance(
            currentAppearance.copy(
                embeddedAppearance = Embedded(
                    updateFlatWithRadio(
                        current = currentEmbeddedRowStyle,
                        additionalInsetsDp = it
                    )
                )
            )
        )
    }
    Divider()

    AppearanceToggle("topSeparatorEnabled", currentEmbeddedRowStyle.topSeparatorEnabled) {
        updateAppearance(
            currentAppearance.copy(
                embeddedAppearance = Embedded(
                    updateFlatWithRadio(
                        current = currentEmbeddedRowStyle,
                        topSeparatorEnabled = it
                    )
                )
            )
        )
    }
    Divider()

    AppearanceToggle("bottomSeparatorEnabled", currentEmbeddedRowStyle.bottomSeparatorEnabled) {
        updateAppearance(
            currentAppearance.copy(
                embeddedAppearance = Embedded(
                    updateFlatWithRadio(
                        current = currentEmbeddedRowStyle,
                        bottomSeparatorEnabled = it
                    )
                )
            )
        )
    }
    Divider()
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@Composable
private fun FlatWithCheckmark(
    currentAppearance: PaymentSheet.Appearance,
    updateAppearance: (PaymentSheet.Appearance) -> Unit,
) {
    val currentStyle = currentAppearance.getEmbedded().getRowStyle() as Embedded.RowStyle.FlatWithCheckmark

    ColorItem(
        label = "separatorColor",
        currentColor = Color(currentStyle.separatorColor),
        onColorPicked = {
            currentAppearance.copy(
                embeddedAppearance = Embedded(
                    updateFlatWithCheckmark(
                        current = currentStyle,
                        separatorColor = it.toArgb()
                    )
                )
            )
        },
        updateAppearance = updateAppearance,
    )
    Divider()

    ColorItem(
        label = "checkmarkColor",
        currentColor = Color(currentStyle.checkmarkColor),
        onColorPicked = {
            currentAppearance.copy(
                embeddedAppearance = Embedded(
                    updateFlatWithCheckmark(
                        current = currentStyle,
                        checkmarkColor = it.toArgb()
                    )
                )
            )
        },
        updateAppearance = updateAppearance,
    )
    Divider()

    IncrementDecrementItem("separatorThicknessDp", currentStyle.separatorThicknessDp) {
        updateAppearance(
            currentAppearance.copy(
                embeddedAppearance = Embedded(
                    updateFlatWithCheckmark(
                        current = currentStyle,
                        separatorThicknessDp = it
                    )
                )
            )
        )
    }
    Divider()

    IncrementDecrementItem("separatorInsetsDp", currentStyle.separatorInsetsDp) {
        updateAppearance(
            currentAppearance.copy(
                embeddedAppearance = Embedded(
                    updateFlatWithCheckmark(
                        current = currentStyle,
                        separatorInsetsDp = it
                    )
                )
            )
        )
    }
    Divider()

    IncrementDecrementItem("checkmarkInsetsDp", currentStyle.checkmarkInsetDp) {
        updateAppearance(
            currentAppearance.copy(
                embeddedAppearance = Embedded(
                    updateFlatWithCheckmark(
                        current = currentStyle,
                        checkmarkInsetsDp = it
                    )
                )
            )
        )
    }
    Divider()

    IncrementDecrementItem("additionalInsetsDp", currentStyle.additionalInsetsDp) {
        updateAppearance(
            currentAppearance.copy(
                embeddedAppearance = Embedded(
                    updateFlatWithCheckmark(
                        current = currentStyle,
                        additionalInsetsDp = it
                    )
                )
            )
        )
    }
    Divider()

    AppearanceToggle("topSeparatorEnabled", currentStyle.topSeparatorEnabled) {
        updateAppearance(
            currentAppearance.copy(
                embeddedAppearance = Embedded(
                    updateFlatWithCheckmark(
                        current = currentStyle,
                        topSeparatorEnabled = it
                    )
                )
            )
        )
    }
    Divider()

    AppearanceToggle("bottomSeparatorEnabled", currentStyle.bottomSeparatorEnabled) {
        updateAppearance(
            currentAppearance.copy(
                embeddedAppearance = Embedded(
                    updateFlatWithCheckmark(
                        current = currentStyle,
                        bottomSeparatorEnabled = it
                    )
                )
            )
        )
    }
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@Composable
private fun FloatingButton(
    currentAppearance: PaymentSheet.Appearance,
    updateAppearance: (PaymentSheet.Appearance) -> Unit,
) {
    val currentEmbeddedRowStyle = currentAppearance.getEmbedded().getRowStyle() as Embedded.RowStyle.FloatingButton

    IncrementDecrementItem("spacingDp", currentEmbeddedRowStyle.spacingDp) {
        updateAppearance(
            currentAppearance.copy(
                embeddedAppearance = Embedded(
                    Embedded.RowStyle.FloatingButton(
                        spacingDp = it,
                        additionalInsetsDp = currentEmbeddedRowStyle.additionalInsetsDp
                    )
                )
            )
        )
    }
    Divider()

    IncrementDecrementItem("additionalInsets", currentEmbeddedRowStyle.additionalInsetsDp) {
        updateAppearance(
            currentAppearance.copy(
                embeddedAppearance = Embedded(
                    Embedded.RowStyle.FloatingButton(
                        spacingDp = currentEmbeddedRowStyle.spacingDp,
                        additionalInsetsDp = it
                    )
                )
            )
        )
    }
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
private fun updateFlatWithRadio(
    current: Embedded.RowStyle.FlatWithRadio,
    separatorThicknessDp: Float? = null,
    separatorColor: Int? = null,
    separatorInsetsDp: Float? = null,
    topSeparatorEnabled: Boolean? = null,
    bottomSeparatorEnabled: Boolean? = null,
    selectedColor: Int? = null,
    unselectedColor: Int? = null,
    additionalInsetsDp: Float? = null
): Embedded.RowStyle.FlatWithRadio {
    return Embedded.RowStyle.FlatWithRadio(
        separatorThicknessDp = separatorThicknessDp ?: current.separatorThicknessDp,
        separatorColor = separatorColor ?: current.separatorColor,
        separatorInsetsDp = separatorInsetsDp ?: current.separatorInsetsDp,
        topSeparatorEnabled = topSeparatorEnabled ?: current.topSeparatorEnabled,
        bottomSeparatorEnabled = bottomSeparatorEnabled ?: current.bottomSeparatorEnabled,
        selectedColor = selectedColor ?: current.selectedColor,
        unselectedColor = unselectedColor ?: current.unselectedColor,
        additionalInsetsDp = additionalInsetsDp ?: current.additionalInsetsDp,
    )
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
private fun updateFlatWithCheckmark(
    current: Embedded.RowStyle.FlatWithCheckmark,
    separatorThicknessDp: Float? = null,
    separatorColor: Int? = null,
    separatorInsetsDp: Float? = null,
    topSeparatorEnabled: Boolean? = null,
    bottomSeparatorEnabled: Boolean? = null,
    checkmarkColor: Int? = null,
    checkmarkInsetsDp: Float? = null,
    additionalInsetsDp: Float? = null
): Embedded.RowStyle.FlatWithCheckmark {
    return Embedded.RowStyle.FlatWithCheckmark(
        separatorThicknessDp = separatorThicknessDp ?: current.separatorThicknessDp,
        separatorColor = separatorColor ?: current.separatorColor,
        separatorInsetsDp = separatorInsetsDp ?: current.separatorInsetsDp,
        topSeparatorEnabled = topSeparatorEnabled ?: current.topSeparatorEnabled,
        bottomSeparatorEnabled = bottomSeparatorEnabled ?: current.bottomSeparatorEnabled,
        checkmarkColor = checkmarkColor ?: current.checkmarkColor,
        checkmarkInsetDp = checkmarkInsetsDp ?: current.checkmarkInsetDp,
        additionalInsetsDp = additionalInsetsDp ?: current.additionalInsetsDp,
    )
}

@Composable
private fun ColorItem(
    label: String,
    currentColor: Color,
    onColorPicked: (Color) -> PaymentSheet.Appearance,
    updateAppearance: (PaymentSheet.Appearance) -> Unit,
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
private fun IncrementDecrementItem(label: String, value: Float, onValueChange: (Float) -> Unit) {
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
                        val newValue = value - 1
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
                        onValueChange(value + 1)
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

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@Composable
private fun RowStyleDropDown(style: Embedded.RowStyle, rowStyleSelectedCallback: (Embedded.RowStyle) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxSize()
            .padding(all = BASE_PADDING)
            .wrapContentSize(Alignment.TopStart)
    ) {
        Text(
            text = "RowStyle: ${style::class.simpleName}",
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
            DropdownMenuItem(
                onClick = {
                    expanded = false
                    rowStyleSelectedCallback(
                        Embedded.RowStyle.FlatWithRadio(
                            separatorThicknessDp = StripeThemeDefaults.flat.separatorThickness,
                            separatorColor = StripeThemeDefaults.colorsLight.componentBorder.toArgb(),
                            separatorInsetsDp = StripeThemeDefaults.flat.separatorInsets,
                            topSeparatorEnabled = StripeThemeDefaults.flat.topSeparatorEnabled,
                            bottomSeparatorEnabled = StripeThemeDefaults.flat.bottomSeparatorEnabled,
                            selectedColor = StripeThemeDefaults.colorsLight.materialColors.primary.toArgb(),
                            unselectedColor = StripeThemeDefaults.colorsLight.componentBorder.toArgb(),
                            additionalInsetsDp = StripeThemeDefaults.embeddedCommon.additionalInsetsDp
                        )
                    )
                }
            ) {
                Text("FlatWithRadio")
            }
            DropdownMenuItem(
                onClick = {
                    expanded = false
                    rowStyleSelectedCallback(
                        Embedded.RowStyle.FlatWithCheckmark(
                            separatorThicknessDp = StripeThemeDefaults.flat.separatorThickness,
                            separatorColor = StripeThemeDefaults.colorsLight.componentBorder.toArgb(),
                            separatorInsetsDp = StripeThemeDefaults.flat.separatorInsets,
                            topSeparatorEnabled = StripeThemeDefaults.flat.topSeparatorEnabled,
                            bottomSeparatorEnabled = StripeThemeDefaults.flat.bottomSeparatorEnabled,
                            checkmarkColor = StripeThemeDefaults.colorsLight.materialColors.primary.toArgb(),
                            checkmarkInsetDp = StripeThemeDefaults.embeddedCommon.checkmarkInsetDp,
                            additionalInsetsDp = StripeThemeDefaults.embeddedCommon.additionalInsetsDp
                        )
                    )
                }
            ) {
                Text("FlatWithCheckmark")
            }
            DropdownMenuItem(
                onClick = {
                    expanded = false
                    rowStyleSelectedCallback(
                        Embedded.RowStyle.FloatingButton(
                            spacingDp = StripeThemeDefaults.floating.spacing,
                            additionalInsetsDp = StripeThemeDefaults.embeddedCommon.additionalInsetsDp
                        )
                    )
                }
            ) {
                Text("FloatingButton")
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

private fun getFontFromResource(fontResId: Int?): FontFamily {
    return fontResId?.let {
        FontFamily(Font(it))
    } ?: run {
        FontFamily.Default
    }
}
