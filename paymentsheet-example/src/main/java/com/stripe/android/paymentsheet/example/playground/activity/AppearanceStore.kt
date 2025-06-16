package com.stripe.android.paymentsheet.example.playground.activity

import android.os.Parcelable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet
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
        val shapes: PaymentSheet.Shapes = PaymentSheet.Shapes.default,
        val typography: PaymentSheet.Typography = PaymentSheet.Typography.default,
        val primaryButton: PaymentSheet.PrimaryButton = PaymentSheet.PrimaryButton(),
        val embedded: Embedded = Embedded(),
        val formInsetValues: Insets = Insets.Default,
        val sectionSpacing: SectionSpacing = SectionSpacing.Default,
        val textFieldInsets: Insets = Insets.Default,
        val iconStyle: IconStyle = IconStyle.Filled,
    ) {
        @OptIn(ExperimentalEmbeddedPaymentElementApi::class, AppearanceAPIAdditionsPreview::class)
        fun toPaymentSheetAppearance(): PaymentSheet.Appearance {
            return PaymentSheet.Appearance.Builder()
                .colorsLight(colorsLight)
                .colorsDark(colorsDark)
                .shapes(shapes)
                .typography(typography)
                .primaryButton(primaryButton)
                .embeddedAppearance(PaymentSheet.Appearance.Embedded(embedded.getRow()))
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
            }
        }

        @OptIn(ExperimentalEmbeddedPaymentElementApi::class)
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
            val checkmarkColor: Int = Color(0xFF007AFF).toArgb()
        ) : Parcelable {
            enum class Row {
                FlatWithRadio,
                FlatWithCheckmark,
                FloatingButton
            }

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
    }
}
