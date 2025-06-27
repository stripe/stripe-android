package com.stripe.android.paymentsheet.example.playground.activity

import android.os.Parcelable
import androidx.annotation.ColorInt
import androidx.annotation.FontRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.PRIMARY_BUTTON_SUCCESS_BACKGROUND_COLOR
import com.stripe.android.uicore.StripeThemeDefaults
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

internal object AppearanceStore {
    var state by mutableStateOf(State())
    var forceDarkMode by mutableStateOf(false)

    fun reset() {
        state = State()
        forceDarkMode = false
    }

    data class State(
        val colorsLight: PaymentSheet.Colors = PaymentSheet.Colors.defaultLight,
        val colorsDark: PaymentSheet.Colors = PaymentSheet.Colors.defaultDark,
        val shapes: Shapes = Shapes(),
        val typography: PaymentSheet.Typography = PaymentSheet.Typography.default,
        val primaryButton: PrimaryButton = PrimaryButton(),
        val embedded: Embedded = Embedded(),
        val formInsetValues: Insets = Insets.Default,
        val sectionSpacing: SectionSpacing = SectionSpacing.Default,
        val textFieldInsets: Insets = Insets.Default,
        val verticalModeRowPadding: Float = StripeThemeDefaults.verticalModeRowPadding,
        val iconStyle: IconStyle = IconStyle.Filled,
    ) {
        @OptIn(AppearanceAPIAdditionsPreview::class)
        fun toPaymentSheetAppearance(): PaymentSheet.Appearance {
            return PaymentSheet.Appearance.Builder()
                .colorsLight(colorsLight)
                .colorsDark(colorsDark)
                .shapes(shapes.build())
                .typography(typography)
                .primaryButton(primaryButton.build())
                .embeddedAppearance(PaymentSheet.Appearance.Embedded(embedded.getRow()))
                .verticalModeRowPadding(verticalModeRowPadding)
                .apply {
                    formInsetValues.getPaymentSheetInsets()?.let {
                        formInsetValues(it)
                    }

                    sectionSpacing.getPaymentSheetSpacing()?.let {
                        sectionSpacing(it)
                    }

                    textFieldInsets.getPaymentSheetInsets()?.let {
                        textFieldInsets(it)
                    }
                }
                .iconStyle(iconStyle.toPaymentSheetIconStyle())
                .build()
        }

        @OptIn(AppearanceAPIAdditionsPreview::class)
        sealed interface SectionSpacing : Parcelable {
            fun getPaymentSheetSpacing(): PaymentSheet.Spacing?

            @Serializable
            @Parcelize
            data object Default : SectionSpacing {
                override fun getPaymentSheetSpacing() = null
            }

            @Serializable
            @Parcelize
            data class Custom(
                val spacingDp: Float = 8f,
            ) : SectionSpacing {
                override fun getPaymentSheetSpacing(): PaymentSheet.Spacing {
                    return PaymentSheet.Spacing(
                        spacingDp = spacingDp
                    )
                }
            }
        }

        sealed interface Insets : Parcelable {
            fun getPaymentSheetInsets(): PaymentSheet.Insets?

            @Serializable
            @Parcelize
            data object Default : Insets {
                override fun getPaymentSheetInsets() = null
            }

            @Serializable
            @Parcelize
            data class Custom(
                val start: Float,
                val top: Float,
                val end: Float,
                val bottom: Float
            ) : Insets {
                override fun getPaymentSheetInsets(): PaymentSheet.Insets {
                    return PaymentSheet.Insets(
                        startDp = start,
                        endDp = end,
                        topDp = top,
                        bottomDp = bottom,
                    )
                }
            }

            companion object {
                val defaultFormInsets = Custom(
                    start = 20f,
                    top = 0f,
                    end = 20f,
                    bottom = 40f,
                )

                val defaultTextInsets = Custom(
                    start = StripeThemeDefaults.textFieldInsets.start,
                    top = StripeThemeDefaults.textFieldInsets.top,
                    end = StripeThemeDefaults.textFieldInsets.end,
                    bottom = StripeThemeDefaults.textFieldInsets.bottom,
                )
            }
        }

        @Serializable
        @Parcelize
        data class Embedded(
            val embeddedRowStyle: Row = Row.FlatWithRadio,
            val separatorThicknessDp: Float = 1.0f,
            val startSeparatorInset: Float = 0.0f,
            val endSeparatorInset: Float = 0.0f,
            val additionalVerticalInsetsDp: Float = 4.0f,
            val horizontalInsetsDp: Float = 0.0f,
            val checkmarkInsetsDp: Float = 0.0f,
            val floatingButtonSpacingDp: Float = 12.0f,
            val topSeparatorEnabled: Boolean = true,
            val bottomSeparatorEnabled: Boolean = true,
            val separatorColor: Int = Color(0x33787880).toArgb(),
            val selectedColor: Int = Color(0xFF007AFF).toArgb(),
            val unselectedColor: Int = Color(0x33787880).toArgb(),
            val checkmarkColor: Int = Color(0xFF007AFF).toArgb(),
            val chevronColor: Int = Color.DarkGray.toArgb()
        ) : Parcelable {
            enum class Row {
                FlatWithRadio,
                FlatWithCheckmark,
                FlatWithChevron,
                FloatingButton
            }

            @Suppress("LongMethod")
            fun getRow(): PaymentSheet.Appearance.Embedded.RowStyle {
                return when (embeddedRowStyle) {
                    Row.FlatWithRadio -> PaymentSheet.Appearance.Embedded.RowStyle.FlatWithRadio(
                        separatorThicknessDp = separatorThicknessDp,
                        startSeparatorInsetDp = startSeparatorInset,
                        endSeparatorInsetDp = endSeparatorInset,
                        topSeparatorEnabled = topSeparatorEnabled,
                        bottomSeparatorEnabled = bottomSeparatorEnabled,
                        additionalVerticalInsetsDp = additionalVerticalInsetsDp,
                        horizontalInsetsDp = horizontalInsetsDp,
                        colorsLight = PaymentSheet.Appearance.Embedded.RowStyle.FlatWithRadio.Colors(
                            separatorColor = separatorColor,
                            selectedColor = selectedColor,
                            unselectedColor = unselectedColor
                        ),
                        colorsDark = PaymentSheet.Appearance.Embedded.RowStyle.FlatWithRadio.Colors(
                            separatorColor = Color(0x40FFFFFF).toArgb(),
                            selectedColor = Color(0xFF0074D4).toArgb(),
                            unselectedColor = Color(0x40FFFFFF).toArgb(),
                        )
                    )
                    Row.FlatWithCheckmark -> PaymentSheet.Appearance.Embedded.RowStyle.FlatWithCheckmark(
                        separatorThicknessDp = separatorThicknessDp,
                        startSeparatorInsetDp = startSeparatorInset,
                        endSeparatorInsetDp = endSeparatorInset,
                        topSeparatorEnabled = topSeparatorEnabled,
                        bottomSeparatorEnabled = bottomSeparatorEnabled,
                        checkmarkInsetDp = checkmarkInsetsDp,
                        additionalVerticalInsetsDp = additionalVerticalInsetsDp,
                        horizontalInsetsDp = horizontalInsetsDp,
                        colorsLight = PaymentSheet.Appearance.Embedded.RowStyle.FlatWithCheckmark.Colors(
                            separatorColor = separatorColor,
                            checkmarkColor = checkmarkColor
                        ),
                        colorsDark = PaymentSheet.Appearance.Embedded.RowStyle.FlatWithCheckmark.Colors(
                            separatorColor = Color(0x40FFFFFF).toArgb(),
                            checkmarkColor = Color(0xFF0074D4).toArgb()
                        )
                    )
                    Row.FlatWithChevron -> PaymentSheet.Appearance.Embedded.RowStyle.FlatWithChevron(
                        separatorThicknessDp = separatorThicknessDp,
                        startSeparatorInsetDp = startSeparatorInset,
                        endSeparatorInsetDp = endSeparatorInset,
                        topSeparatorEnabled = topSeparatorEnabled,
                        bottomSeparatorEnabled = bottomSeparatorEnabled,
                        additionalVerticalInsetsDp = additionalVerticalInsetsDp,
                        horizontalInsetsDp = horizontalInsetsDp,
                        colorsLight = PaymentSheet.Appearance.Embedded.RowStyle.FlatWithChevron.Colors(
                            separatorColor = separatorColor,
                            chevronColor = chevronColor
                        ),
                        colorsDark = PaymentSheet.Appearance.Embedded.RowStyle.FlatWithChevron.Colors(
                            separatorColor = Color(0x40FFFFFF).toArgb(),
                            chevronColor = Color.LightGray.toArgb()
                        )
                    )
                    Row.FloatingButton -> PaymentSheet.Appearance.Embedded.RowStyle.FloatingButton(
                        spacingDp = floatingButtonSpacingDp,
                        additionalInsetsDp = additionalVerticalInsetsDp
                    )
                }
            }
        }

        @OptIn(AppearanceAPIAdditionsPreview::class)
        enum class IconStyle {
            Filled,
            Outlined;

            fun toPaymentSheetIconStyle(): PaymentSheet.IconStyle {
                return when (this) {
                    Filled -> PaymentSheet.IconStyle.Filled
                    Outlined -> PaymentSheet.IconStyle.Outlined
                }
            }
        }

        data class Shapes(
            val cornerRadiusDp: Float = StripeThemeDefaults.shapes.cornerRadius,
            val borderStrokeWidthDp: Float = StripeThemeDefaults.shapes.borderStrokeWidth,
            val bottomSheetCornerRadiusDp: Float = cornerRadiusDp,
        ) {
            @OptIn(AppearanceAPIAdditionsPreview::class)
            fun build(): PaymentSheet.Shapes = PaymentSheet.Shapes(
                cornerRadiusDp = cornerRadiusDp,
                borderStrokeWidthDp = borderStrokeWidthDp,
                bottomSheetCornerRadiusDp = bottomSheetCornerRadiusDp,
            )
        }

        data class PrimaryButton(
            val colorsLight: PrimaryButtonColors = PrimaryButtonColors(
                background = null,
                onBackground = StripeThemeDefaults.primaryButtonStyle.colorsLight.onBackground.toArgb(),
                border = StripeThemeDefaults.primaryButtonStyle.colorsLight.border.toArgb(),
                successBackgroundColor = StripeThemeDefaults.primaryButtonStyle.colorsLight.successBackground.toArgb(),
                onSuccessBackgroundColor = StripeThemeDefaults.primaryButtonStyle.colorsLight.onBackground.toArgb(),
            ),
            val colorsDark: PrimaryButtonColors = PrimaryButtonColors(
                background = null,
                onBackground = StripeThemeDefaults.primaryButtonStyle.colorsDark.onBackground.toArgb(),
                border = StripeThemeDefaults.primaryButtonStyle.colorsDark.border.toArgb(),
                successBackgroundColor = StripeThemeDefaults.primaryButtonStyle.colorsDark.successBackground.toArgb(),
                onSuccessBackgroundColor = StripeThemeDefaults.primaryButtonStyle.colorsDark.onBackground.toArgb(),
            ),
            val shape: PrimaryButtonShape = PrimaryButtonShape(),
            val typography: PrimaryButtonTypography = PrimaryButtonTypography(),
        ) {
            fun build(): PaymentSheet.PrimaryButton = PaymentSheet.PrimaryButton(
                colorsLight = colorsLight.build(),
                colorsDark = colorsDark.build(),
                shape = shape.build(),
                typography = typography.build(),
            )
        }

        data class PrimaryButtonTypography(
            @FontRes
            val fontResId: Int? = null,
            val fontSizeSp: Float? = null,
        ) {
            fun build(): PaymentSheet.PrimaryButtonTypography = PaymentSheet.PrimaryButtonTypography(
                fontResId = fontResId,
                fontSizeSp = fontSizeSp,
            )
        }

        data class PrimaryButtonColors(
            @ColorInt
            val background: Int?,
            @ColorInt
            val onBackground: Int,
            @ColorInt
            val border: Int,
            @ColorInt
            val successBackgroundColor: Int = PRIMARY_BUTTON_SUCCESS_BACKGROUND_COLOR.toArgb(),
            @ColorInt
            val onSuccessBackgroundColor: Int = onBackground,
        ) {
            fun build(): PaymentSheet.PrimaryButtonColors = PaymentSheet.PrimaryButtonColors(
                background = background,
                onBackground = onBackground,
                border = border,
                successBackgroundColor = successBackgroundColor,
                onSuccessBackgroundColor = onSuccessBackgroundColor,
            )
        }

        data class PrimaryButtonShape(
            val cornerRadiusDp: Float? = null,
            val borderStrokeWidthDp: Float? = null,
            val heightDp: Float? = null
        ) {
            fun build(): PaymentSheet.PrimaryButtonShape = PaymentSheet.PrimaryButtonShape(
                cornerRadiusDp = cornerRadiusDp,
                borderStrokeWidthDp = borderStrokeWidthDp,
                heightDp = heightDp,
            )
        }

        companion object {
            const val defaultCustomH1FontSizeDp = 20f
            const val defaultCustomH1LetterSpacingSp = 0.13f
            const val defaultCustomH1FontWeight = 400f
        }
    }
}
