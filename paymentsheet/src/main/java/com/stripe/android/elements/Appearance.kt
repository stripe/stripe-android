package com.stripe.android.elements

import android.content.Context
import android.os.Parcelable
import androidx.annotation.ColorInt
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.FontRes
import androidx.annotation.RestrictTo
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.PRIMARY_BUTTON_SUCCESS_BACKGROUND_COLOR
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.android.uicore.getRawValueFromDimenResource
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

@Parcelize
data class Appearance
@OptIn(AppearanceAPIAdditionsPreview::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    /**
     * Describes the colors used while the system is in light mode.
     */
    val colorsLight: Colors = Colors.defaultLight,

    /**
     * Describes the colors used while the system is in dark mode.
     */
    val colorsDark: Colors = Colors.defaultDark,

    /**
     * Describes the appearance of shapes.
     */
    val shapes: Shapes = Shapes.default,

    /**
     * Describes the typography used for text.
     */
    val typography: Typography = Typography.default,

    /**
     * Describes the appearance of the primary button (e.g., the "Pay" button).
     */
    val primaryButton: PrimaryButton = PrimaryButton(),

    /**
     * Describes the appearance of the Embedded Payment Element
     */
    internal val embeddedAppearance: Embedded = Embedded.default,

    /**
     * Describes the inset values used for all forms
     */
    internal val formInsetValues: Insets = Insets.defaultFormInsetValues,

    /**
     * Defines spacing between conceptual sections of a form. This does not control padding
     * between input fields. Negative values will also be ignored and default spacing will
     * be applied.
     */
    internal val sectionSpacing: Spacing = Spacing.defaultSectionSpacing,

    /**
     * Defines spacing inside the input fields of a form.
     */
    internal val textFieldInsets: Insets = Insets.defaultTextFieldInsets,

    /**
     * Defines the visual style of icons in Payment Element
     */
    internal val iconStyle: IconStyle = IconStyle.default,

    internal val verticalModeRowPadding: Float = StripeThemeDefaults.verticalModeRowPadding,
) : Parcelable {
    constructor() : this(
        colorsLight = Colors.defaultLight,
        colorsDark = Colors.defaultDark,
        shapes = Shapes.default,
        typography = Typography.default,
        primaryButton = PrimaryButton(),
    )

    constructor(
        colorsLight: Colors = Colors.defaultLight,
        colorsDark: Colors = Colors.defaultDark,
        shapes: Shapes = Shapes.default,
        typography: Typography = Typography.default,
        primaryButton: PrimaryButton = PrimaryButton(),
    ) : this(
        colorsLight = colorsLight,
        colorsDark = colorsDark,
        shapes = shapes,
        typography = typography,
        primaryButton = primaryButton,
        embeddedAppearance = Embedded.default
    )

    @OptIn(AppearanceAPIAdditionsPreview::class)
    constructor(
        colorsLight: Colors = Colors.defaultLight,
        colorsDark: Colors = Colors.defaultDark,
        shapes: Shapes = Shapes.default,
        typography: Typography = Typography.default,
        primaryButton: PrimaryButton = PrimaryButton(),
        embeddedAppearance: Embedded = Embedded.default,
        formInsetValues: Insets = Insets.defaultFormInsetValues,
    ) : this(
        colorsLight = colorsLight,
        colorsDark = colorsDark,
        shapes = shapes,
        typography = typography,
        primaryButton = primaryButton,
        embeddedAppearance = embeddedAppearance,
        formInsetValues = formInsetValues,
        sectionSpacing = Spacing.defaultSectionSpacing,
    )

    fun getColors(isDark: Boolean): Colors {
        return if (isDark) colorsDark else colorsLight
    }

    @Parcelize
    @Poko
    @OptIn(AppearanceAPIAdditionsPreview::class)
    class Embedded internal constructor(
        internal val style: RowStyle,
        internal val paymentMethodIconMargins: Insets?,
        internal val titleFont: Typography.Font?,
        internal val subtitleFont: Typography.Font?,
    ) : Parcelable {

        constructor(
            style: RowStyle
        ) : this(
            style = style,
            paymentMethodIconMargins = null,
            titleFont = null,
            subtitleFont = null
        )

        internal companion object {
            val default = Embedded(
                style = RowStyle.FlatWithRadio.default
            )
        }

        @Parcelize
        sealed class RowStyle : Parcelable {

            internal abstract fun hasSeparators(): Boolean
            internal abstract fun startSeparatorHasDefaultInset(): Boolean

            @Parcelize
            @Poko
            class FlatWithRadio(
                internal val separatorThicknessDp: Float,
                internal val startSeparatorInsetDp: Float,
                internal val endSeparatorInsetDp: Float,
                internal val topSeparatorEnabled: Boolean,
                internal val bottomSeparatorEnabled: Boolean,
                internal val additionalVerticalInsetsDp: Float,
                internal val horizontalInsetsDp: Float,
                internal val colorsLight: Colors,
                internal val colorsDark: Colors
            ) : RowStyle() {
                constructor(
                    context: Context,
                    @DimenRes separatorThicknessRes: Int,
                    @DimenRes startSeparatorInsetRes: Int,
                    @DimenRes endSeparatorInsetRes: Int,
                    topSeparatorEnabled: Boolean,
                    bottomSeparatorEnabled: Boolean,
                    @DimenRes additionalVerticalInsetsRes: Int,
                    @DimenRes horizontalInsetsRes: Int,
                    colorsLight: Colors,
                    colorsDark: Colors
                ) : this(
                    separatorThicknessDp = context.getRawValueFromDimenResource(separatorThicknessRes),
                    startSeparatorInsetDp = context.getRawValueFromDimenResource(startSeparatorInsetRes),
                    endSeparatorInsetDp = context.getRawValueFromDimenResource(endSeparatorInsetRes),
                    topSeparatorEnabled = topSeparatorEnabled,
                    bottomSeparatorEnabled = bottomSeparatorEnabled,
                    additionalVerticalInsetsDp = context.getRawValueFromDimenResource(additionalVerticalInsetsRes),
                    horizontalInsetsDp = context.getRawValueFromDimenResource(horizontalInsetsRes),
                    colorsLight = colorsLight,
                    colorsDark = colorsDark
                )

                override fun hasSeparators() = true
                override fun startSeparatorHasDefaultInset() = true
                internal fun getColors(isDark: Boolean): Colors = if (isDark) colorsDark else colorsLight

                @Parcelize
                @Poko
                class Colors(
                    /**
                     * The color of the separator line between rows.
                     */
                    @ColorInt
                    internal val separatorColor: Int,

                    /**
                     * The color of the radio button when selected.
                     */
                    @ColorInt
                    internal val selectedColor: Int,

                    /**
                     * The color of the radio button when unselected.
                     */
                    @ColorInt
                    internal val unselectedColor: Int,
                ) : Parcelable

                internal companion object {
                    val default = FlatWithRadio(
                        separatorThicknessDp = StripeThemeDefaults.flat.separatorThickness,
                        startSeparatorInsetDp = StripeThemeDefaults.flat.separatorInsets,
                        endSeparatorInsetDp = StripeThemeDefaults.flat.separatorInsets,
                        topSeparatorEnabled = StripeThemeDefaults.flat.topSeparatorEnabled,
                        bottomSeparatorEnabled = StripeThemeDefaults.flat.bottomSeparatorEnabled,
                        additionalVerticalInsetsDp = StripeThemeDefaults.embeddedCommon.additionalVerticalInsetsDp,
                        horizontalInsetsDp = StripeThemeDefaults.embeddedCommon.horizontalInsetsDp,
                        colorsLight = Colors(
                            separatorColor = StripeThemeDefaults.radioColorsLight.separatorColor.toArgb(),
                            selectedColor = StripeThemeDefaults.radioColorsLight.selectedColor.toArgb(),
                            unselectedColor = StripeThemeDefaults.radioColorsLight.unselectedColor.toArgb()
                        ),
                        colorsDark = Colors(
                            separatorColor = StripeThemeDefaults.radioColorsDark.separatorColor.toArgb(),
                            selectedColor = StripeThemeDefaults.radioColorsDark.selectedColor.toArgb(),
                            unselectedColor = StripeThemeDefaults.radioColorsDark.unselectedColor.toArgb()
                        ),
                    )
                }

                class Builder {
                    private var separatorThicknessDp = StripeThemeDefaults.flat.separatorThickness
                    private var startSeparatorInsetDp = StripeThemeDefaults.flat.separatorInsets
                    private var endSeparatorInsetDp = StripeThemeDefaults.flat.separatorInsets
                    private var topSeparatorEnabled = StripeThemeDefaults.flat.topSeparatorEnabled
                    private var bottomSeparatorEnabled = StripeThemeDefaults.flat.bottomSeparatorEnabled
                    private var additionalVerticalInsetsDp = StripeThemeDefaults.embeddedCommon
                        .additionalVerticalInsetsDp
                    private var horizontalInsetsDp = StripeThemeDefaults.embeddedCommon.horizontalInsetsDp
                    private var colorsLight = Colors(
                        separatorColor = StripeThemeDefaults.radioColorsLight.separatorColor.toArgb(),
                        selectedColor = StripeThemeDefaults.radioColorsLight.selectedColor.toArgb(),
                        unselectedColor = StripeThemeDefaults.radioColorsLight.unselectedColor.toArgb()
                    )
                    private var colorsDark = Colors(
                        separatorColor = StripeThemeDefaults.radioColorsDark.separatorColor.toArgb(),
                        selectedColor = StripeThemeDefaults.radioColorsDark.selectedColor.toArgb(),
                        unselectedColor = StripeThemeDefaults.radioColorsDark.unselectedColor.toArgb()
                    )

                    /**
                     * The thickness of the separator line between rows.
                     */
                    fun separatorThicknessDp(thickness: Float) = apply {
                        this.separatorThicknessDp = thickness
                    }

                    /**
                     * The start inset of the separator line between rows.
                     */
                    fun startSeparatorInsetDp(inset: Float) = apply {
                        this.startSeparatorInsetDp = inset
                    }

                    /**
                     * The end inset of the separator line between rows.
                     */
                    fun endSeparatorInsetDp(inset: Float) = apply {
                        this.endSeparatorInsetDp = inset
                    }

                    /**
                     * Determines if the top separator is visible at the top of the Embedded Mobile Payment Element.
                     */
                    fun topSeparatorEnabled(enabled: Boolean) = apply {
                        this.topSeparatorEnabled = enabled
                    }

                    /**
                     * Determines if the bottom separator is visible at the bottom of the Embedded Mobile Payment
                     * Element.
                     */
                    fun bottomSeparatorEnabled(enabled: Boolean) = apply {
                        this.bottomSeparatorEnabled = enabled
                    }

                    /**
                     * Additional vertical insets applied to a payment method row.
                     * - Note: Increasing this value increases the height of each row.
                     */
                    fun additionalVerticalInsetsDp(insets: Float) = apply {
                        this.additionalVerticalInsetsDp = insets
                    }

                    /**
                     * Horizontal insets applied to a payment method row.
                     */
                    fun horizontalInsetsDp(insets: Float) = apply {
                        this.horizontalInsetsDp = insets
                    }

                    /**
                     * Describes the colors used while the system is in light mode.
                     */
                    fun colorsLight(colors: Colors) = apply {
                        this.colorsLight = colors
                    }

                    /**
                     * Describes the colors used while the system is in dark mode.
                     */
                    fun colorsDark(colors: Colors) = apply {
                        this.colorsDark = colors
                    }

                    fun build(): FlatWithRadio {
                        return FlatWithRadio(
                            separatorThicknessDp = separatorThicknessDp,
                            startSeparatorInsetDp = startSeparatorInsetDp,
                            endSeparatorInsetDp = endSeparatorInsetDp,
                            topSeparatorEnabled = topSeparatorEnabled,
                            bottomSeparatorEnabled = bottomSeparatorEnabled,
                            additionalVerticalInsetsDp = additionalVerticalInsetsDp,
                            horizontalInsetsDp = horizontalInsetsDp,
                            colorsLight = colorsLight,
                            colorsDark = colorsDark
                        )
                    }
                }
            }

            @Parcelize
            @Poko
            class FlatWithCheckmark(
                internal val separatorThicknessDp: Float,
                internal val startSeparatorInsetDp: Float,
                internal val endSeparatorInsetDp: Float,
                internal val topSeparatorEnabled: Boolean,
                internal val bottomSeparatorEnabled: Boolean,
                internal val checkmarkInsetDp: Float,
                internal val additionalVerticalInsetsDp: Float,
                internal val horizontalInsetsDp: Float,
                internal val colorsLight: Colors,
                internal val colorsDark: Colors
            ) : RowStyle() {
                constructor(
                    context: Context,
                    @DimenRes separatorThicknessRes: Int,
                    @DimenRes startSeparatorInsetRes: Int,
                    @DimenRes endSeparatorInsetRes: Int,
                    topSeparatorEnabled: Boolean,
                    bottomSeparatorEnabled: Boolean,
                    @DimenRes checkmarkInsetRes: Int,
                    @DimenRes additionalVerticalInsetsRes: Int,
                    @DimenRes horizontalInsetsRes: Int,
                    colorsLight: Colors,
                    colorsDark: Colors
                ) : this(
                    separatorThicknessDp = context.getRawValueFromDimenResource(separatorThicknessRes),
                    startSeparatorInsetDp = context.getRawValueFromDimenResource(startSeparatorInsetRes),
                    endSeparatorInsetDp = context.getRawValueFromDimenResource(endSeparatorInsetRes),
                    topSeparatorEnabled = topSeparatorEnabled,
                    bottomSeparatorEnabled = bottomSeparatorEnabled,
                    checkmarkInsetDp = context.getRawValueFromDimenResource(checkmarkInsetRes),
                    additionalVerticalInsetsDp = context.getRawValueFromDimenResource(additionalVerticalInsetsRes),
                    horizontalInsetsDp = context.getRawValueFromDimenResource(horizontalInsetsRes),
                    colorsLight = colorsLight,
                    colorsDark = colorsDark
                )

                @Parcelize
                @Poko
                class Colors(
                    /**
                     * The color of the separator line between rows.
                     */
                    @ColorInt
                    internal val separatorColor: Int,

                    /**
                     * The color of the checkmark.
                     */
                    @ColorInt
                    internal val checkmarkColor: Int,
                ) : Parcelable

                override fun hasSeparators() = true
                override fun startSeparatorHasDefaultInset() = false
                internal fun getColors(isDark: Boolean): Colors = if (isDark) colorsDark else colorsLight

                internal companion object {
                    val default = FlatWithCheckmark(
                        separatorThicknessDp = StripeThemeDefaults.flat.separatorThickness,
                        startSeparatorInsetDp = StripeThemeDefaults.flat.separatorInsets,
                        endSeparatorInsetDp = StripeThemeDefaults.flat.separatorInsets,
                        topSeparatorEnabled = StripeThemeDefaults.flat.topSeparatorEnabled,
                        bottomSeparatorEnabled = StripeThemeDefaults.flat.bottomSeparatorEnabled,
                        checkmarkInsetDp = StripeThemeDefaults.embeddedCommon.checkmarkInsetDp,
                        additionalVerticalInsetsDp = StripeThemeDefaults.embeddedCommon.additionalVerticalInsetsDp,
                        horizontalInsetsDp = StripeThemeDefaults.embeddedCommon.horizontalInsetsDp,
                        colorsLight = Colors(
                            separatorColor = StripeThemeDefaults.checkmarkColorsLight.separatorColor.toArgb(),
                            checkmarkColor = StripeThemeDefaults.checkmarkColorsLight.checkmarkColor.toArgb()
                        ),
                        colorsDark = Colors(
                            separatorColor = StripeThemeDefaults.checkmarkColorsDark.separatorColor.toArgb(),
                            checkmarkColor = StripeThemeDefaults.checkmarkColorsDark.checkmarkColor.toArgb()
                        )
                    )
                }

                @Suppress("TooManyFunctions")
                class Builder {
                    private var separatorThicknessDp = StripeThemeDefaults.flat.separatorThickness
                    private var startSeparatorInsetDp = StripeThemeDefaults.flat.separatorInsets
                    private var endSeparatorInsetDp = StripeThemeDefaults.flat.separatorInsets
                    private var topSeparatorEnabled = StripeThemeDefaults.flat.topSeparatorEnabled
                    private var bottomSeparatorEnabled = StripeThemeDefaults.flat.bottomSeparatorEnabled
                    private var checkmarkInsetDp = StripeThemeDefaults.embeddedCommon.checkmarkInsetDp
                    private var additionalVerticalInsetsDp = StripeThemeDefaults.embeddedCommon
                        .additionalVerticalInsetsDp
                    private var horizontalInsetsDp = StripeThemeDefaults.embeddedCommon.horizontalInsetsDp
                    private var colorsLight = Colors(
                        separatorColor = StripeThemeDefaults.checkmarkColorsLight.separatorColor.toArgb(),
                        checkmarkColor = StripeThemeDefaults.checkmarkColorsLight.checkmarkColor.toArgb(),
                    )
                    private var colorsDark = Colors(
                        separatorColor = StripeThemeDefaults.checkmarkColorsDark.separatorColor.toArgb(),
                        checkmarkColor = StripeThemeDefaults.checkmarkColorsDark.checkmarkColor.toArgb(),
                    )

                    /**
                     * The thickness of the separator line between rows.
                     */
                    fun separatorThicknessDp(thickness: Float) = apply {
                        this.separatorThicknessDp = thickness
                    }

                    /**
                     * The start inset of the separator line between rows.
                     */
                    fun startSeparatorInsetDp(inset: Float) = apply {
                        this.startSeparatorInsetDp = inset
                    }

                    /**
                     * The end inset of the separator line between rows.
                     */
                    fun endSeparatorInsetDp(inset: Float) = apply {
                        this.endSeparatorInsetDp = inset
                    }

                    /**
                     * Determines if the top separator is visible at the top of the Embedded Mobile Payment Element.
                     */
                    fun topSeparatorEnabled(enabled: Boolean) = apply {
                        this.topSeparatorEnabled = enabled
                    }

                    /**
                     * Determines if the bottom separator is visible at the bottom of the Embedded Mobile Payment
                     * Element.
                     */
                    fun bottomSeparatorEnabled(enabled: Boolean) = apply {
                        this.bottomSeparatorEnabled = enabled
                    }

                    /**
                     * Inset of the checkmark from the end of the row.
                     */
                    fun checkmarkInsetDp(insets: Float) = apply {
                        this.checkmarkInsetDp = insets
                    }

                    /**
                     * Additional vertical insets applied to a payment method row.
                     * - Note: Increasing this value increases the height of each row.
                     */
                    fun additionalVerticalInsetsDp(insets: Float) = apply {
                        this.additionalVerticalInsetsDp = insets
                    }

                    /**
                     * Horizontal insets applied to a payment method row.
                     */
                    fun horizontalInsetsDp(insets: Float) = apply {
                        this.horizontalInsetsDp = insets
                    }

                    /**
                     * Describes the colors used while the system is in light mode.
                     */
                    fun colorsLight(colors: Colors) = apply {
                        this.colorsLight = colors
                    }

                    /**
                     * Describes the colors used while the system is in dark mode.
                     */
                    fun colorsDark(colors: Colors) = apply {
                        this.colorsDark = colors
                    }

                    fun build(): FlatWithCheckmark {
                        return FlatWithCheckmark(
                            separatorThicknessDp = separatorThicknessDp,
                            startSeparatorInsetDp = startSeparatorInsetDp,
                            endSeparatorInsetDp = endSeparatorInsetDp,
                            topSeparatorEnabled = topSeparatorEnabled,
                            bottomSeparatorEnabled = bottomSeparatorEnabled,
                            checkmarkInsetDp = checkmarkInsetDp,
                            additionalVerticalInsetsDp = additionalVerticalInsetsDp,
                            horizontalInsetsDp = horizontalInsetsDp,
                            colorsLight = colorsLight,
                            colorsDark = colorsDark
                        )
                    }
                }
            }

            @Parcelize
            @Poko
            class FloatingButton(
                internal val spacingDp: Float,
                internal val additionalInsetsDp: Float,
            ) : RowStyle() {
                constructor(
                    context: Context,
                    @DimenRes spacingRes: Int,
                    @DimenRes additionalInsetsRes: Int
                ) : this(
                    spacingDp = context.getRawValueFromDimenResource(spacingRes),
                    additionalInsetsDp = context.getRawValueFromDimenResource(additionalInsetsRes)
                )

                override fun hasSeparators() = false
                override fun startSeparatorHasDefaultInset() = false

                internal companion object {
                    val default = FloatingButton(
                        spacingDp = StripeThemeDefaults.floating.spacing,
                        additionalInsetsDp = StripeThemeDefaults.embeddedCommon.additionalVerticalInsetsDp
                    )
                }

                class Builder {
                    private var spacingDp = StripeThemeDefaults.floating.spacing
                    private var additionalInsetsDp = StripeThemeDefaults.embeddedCommon.additionalVerticalInsetsDp

                    /**
                     * The spacing between payment method rows.
                     */
                    fun spacingDp(spacing: Float) = apply {
                        this.spacingDp = spacing
                    }

                    /**
                     * Additional vertical insets applied to a payment method row.
                     * - Note: Increasing this value increases the height of each row.
                     */
                    fun additionalInsetsDp(insets: Float) = apply {
                        this.additionalInsetsDp = insets
                    }

                    fun build(): FloatingButton {
                        return FloatingButton(
                            spacingDp = spacingDp,
                            additionalInsetsDp = additionalInsetsDp
                        )
                    }
                }
            }

            @Parcelize
            @Poko
            class FlatWithDisclosure internal constructor(
                internal val separatorThicknessDp: Float,
                internal val startSeparatorInsetDp: Float,
                internal val endSeparatorInsetDp: Float,
                internal val topSeparatorEnabled: Boolean,
                internal val additionalVerticalInsetsDp: Float,
                internal val bottomSeparatorEnabled: Boolean,
                internal val horizontalInsetsDp: Float,
                internal val colorsLight: Colors,
                internal val colorsDark: Colors,
                @DrawableRes
                internal val disclosureIconRes: Int
            ) : RowStyle() {
                constructor(
                    context: Context,
                    @DimenRes separatorThicknessRes: Int,
                    @DimenRes startSeparatorInsetRes: Int,
                    @DimenRes endSeparatorInsetRes: Int,
                    topSeparatorEnabled: Boolean,
                    bottomSeparatorEnabled: Boolean,
                    @DimenRes additionalVerticalInsetsRes: Int,
                    @DimenRes horizontalInsetsRes: Int,
                    colorsLight: Colors,
                    colorsDark: Colors
                ) : this(
                    separatorThicknessDp = context.getRawValueFromDimenResource(separatorThicknessRes),
                    startSeparatorInsetDp = context.getRawValueFromDimenResource(startSeparatorInsetRes),
                    endSeparatorInsetDp = context.getRawValueFromDimenResource(endSeparatorInsetRes),
                    topSeparatorEnabled = topSeparatorEnabled,
                    bottomSeparatorEnabled = bottomSeparatorEnabled,
                    additionalVerticalInsetsDp = context.getRawValueFromDimenResource(additionalVerticalInsetsRes),
                    horizontalInsetsDp = context.getRawValueFromDimenResource(horizontalInsetsRes),
                    colorsLight = colorsLight,
                    colorsDark = colorsDark,
                    disclosureIconRes = R.drawable.stripe_ic_chevron_right
                )

                @Parcelize
                @Poko
                class Colors(
                    /**
                     * The color of the separator line between rows.
                     */
                    @ColorInt
                    internal val separatorColor: Int,

                    /**
                     * The color of the disclosure icon.
                     */
                    @ColorInt
                    internal val disclosureColor: Int,
                ) : Parcelable

                override fun hasSeparators() = true
                override fun startSeparatorHasDefaultInset() = false
                internal fun getColors(isDark: Boolean): Colors = if (isDark) colorsDark else colorsLight

                internal companion object {
                    val default = FlatWithDisclosure(
                        separatorThicknessDp = StripeThemeDefaults.flat.separatorThickness,
                        startSeparatorInsetDp = StripeThemeDefaults.flat.separatorInsets,
                        endSeparatorInsetDp = StripeThemeDefaults.flat.separatorInsets,
                        topSeparatorEnabled = StripeThemeDefaults.flat.topSeparatorEnabled,
                        bottomSeparatorEnabled = StripeThemeDefaults.flat.bottomSeparatorEnabled,
                        additionalVerticalInsetsDp = StripeThemeDefaults.embeddedCommon.additionalVerticalInsetsDp,
                        horizontalInsetsDp = StripeThemeDefaults.embeddedCommon.horizontalInsetsDp,
                        colorsLight = Colors(
                            separatorColor = StripeThemeDefaults.disclosureColorsLight.separatorColor.toArgb(),
                            disclosureColor = StripeThemeDefaults.disclosureColorsLight.disclosureColor.toArgb()
                        ),
                        colorsDark = Colors(
                            separatorColor = StripeThemeDefaults.disclosureColorsDark.separatorColor.toArgb(),
                            disclosureColor = StripeThemeDefaults.disclosureColorsDark.disclosureColor.toArgb()

                        ),
                        disclosureIconRes = R.drawable.stripe_ic_chevron_right
                    )
                }

                @Suppress("TooManyFunctions")
                class Builder {
                    private var separatorThicknessDp = StripeThemeDefaults.flat.separatorThickness
                    private var startSeparatorInsetDp = StripeThemeDefaults.flat.separatorInsets
                    private var endSeparatorInsetDp = StripeThemeDefaults.flat.separatorInsets
                    private var topSeparatorEnabled = StripeThemeDefaults.flat.topSeparatorEnabled
                    private var bottomSeparatorEnabled = StripeThemeDefaults.flat.bottomSeparatorEnabled
                    private var additionalVerticalInsetsDp = StripeThemeDefaults.embeddedCommon
                        .additionalVerticalInsetsDp
                    private var horizontalInsetsDp = StripeThemeDefaults.embeddedCommon.horizontalInsetsDp
                    private var colorsLight = Colors(
                        separatorColor = StripeThemeDefaults.disclosureColorsLight.separatorColor.toArgb(),
                        disclosureColor = StripeThemeDefaults.disclosureColorsLight.disclosureColor.toArgb()
                    )
                    private var colorsDark = Colors(
                        separatorColor = StripeThemeDefaults.disclosureColorsDark.separatorColor.toArgb(),
                        disclosureColor = StripeThemeDefaults.disclosureColorsDark.disclosureColor.toArgb()
                    )
                    private var disclosureIconRes: Int = R.drawable.stripe_ic_chevron_right

                    /**
                     * The thickness of the separator line between rows.
                     */
                    fun separatorThicknessDp(thickness: Float) = apply {
                        this.separatorThicknessDp = thickness
                    }

                    /**
                     * The start inset of the separator line between rows.
                     */
                    fun startSeparatorInsetDp(inset: Float) = apply {
                        this.startSeparatorInsetDp = inset
                    }

                    /**
                     * The end inset of the separator line between rows.
                     */
                    fun endSeparatorInsetDp(inset: Float) = apply {
                        this.endSeparatorInsetDp = inset
                    }

                    /**
                     * Determines if the top separator is visible at the top of the Embedded Mobile Payment Element.
                     */
                    fun topSeparatorEnabled(enabled: Boolean) = apply {
                        this.topSeparatorEnabled = enabled
                    }

                    /**
                     * Determines if the bottom separator is visible at the bottom of the Embedded Mobile Payment
                     * Element.
                     */
                    fun bottomSeparatorEnabled(enabled: Boolean) = apply {
                        this.bottomSeparatorEnabled = enabled
                    }

                    /**
                     * Additional vertical insets applied to a payment method row.
                     * - Note: Increasing this value increases the height of each row.
                     */
                    fun additionalVerticalInsetsDp(insets: Float) = apply {
                        this.additionalVerticalInsetsDp = insets
                    }

                    /**
                     * Horizontal insets applied to a payment method row.
                     */
                    fun horizontalInsetsDp(insets: Float) = apply {
                        this.horizontalInsetsDp = insets
                    }

                    /**
                     * Describes the colors used while the system is in light mode.
                     */
                    fun colorsLight(colors: Colors) = apply {
                        this.colorsLight = colors
                    }

                    /**
                     * Describes the colors used while the system is in dark mode.
                     */
                    fun colorsDark(colors: Colors) = apply {
                        this.colorsDark = colors
                    }

                    /**
                     * The drawable displayed on the end of the row - typically, a chevron. This should be
                     * a resource ID value.
                     * - Note: If not set, uses a default chevron.
                     */
                    @AppearanceAPIAdditionsPreview
                    fun disclosureIconRes(@DrawableRes iconRes: Int) = apply {
                        this.disclosureIconRes = iconRes
                    }

                    fun build(): FlatWithDisclosure {
                        return FlatWithDisclosure(
                            separatorThicknessDp = separatorThicknessDp,
                            startSeparatorInsetDp = startSeparatorInsetDp,
                            endSeparatorInsetDp = endSeparatorInsetDp,
                            topSeparatorEnabled = topSeparatorEnabled,
                            bottomSeparatorEnabled = bottomSeparatorEnabled,
                            additionalVerticalInsetsDp = additionalVerticalInsetsDp,
                            horizontalInsetsDp = horizontalInsetsDp,
                            colorsLight = colorsLight,
                            colorsDark = colorsDark,
                            disclosureIconRes = disclosureIconRes
                        )
                    }
                }
            }
        }

        @OptIn(AppearanceAPIAdditionsPreview::class)
        class Builder {
            private var rowStyle: RowStyle = default.style
            private var paymentMethodIconMargins: Insets? = null
            private var titleFont: Typography.Font? = null
            private var subtitleFont: Typography.Font? = null

            fun rowStyle(rowStyle: RowStyle) = apply {
                this.rowStyle = rowStyle
            }

            @AppearanceAPIAdditionsPreview
            fun paymentMethodIconMargins(margins: Insets?) = apply {
                this.paymentMethodIconMargins = margins
            }

            @AppearanceAPIAdditionsPreview
            fun titleFont(font: Typography.Font?) = apply {
                this.titleFont = font
            }

            @AppearanceAPIAdditionsPreview
            fun subtitleFont(font: Typography.Font?) = apply {
                this.subtitleFont = font
            }

            fun build(): Embedded {
                return Embedded(
                    style = rowStyle,
                    paymentMethodIconMargins = paymentMethodIconMargins,
                    titleFont = titleFont,
                    subtitleFont = subtitleFont
                )
            }
        }
    }

    @Suppress("TooManyFunctions")
    class Builder {
        private var colorsLight = Colors.defaultLight
        private var colorsDark = Colors.defaultDark
        private var shapes = Shapes.default
        private var typography = Typography.default
        private var primaryButton: PrimaryButton = PrimaryButton()
        private var formInsetValues: Insets = Insets.defaultFormInsetValues

        @OptIn(AppearanceAPIAdditionsPreview::class)
        private var sectionSpacing: Spacing = Spacing.defaultSectionSpacing

        private var textFieldInsets: Insets = Insets.defaultTextFieldInsets

        @OptIn(AppearanceAPIAdditionsPreview::class)
        private var iconStyle: IconStyle = IconStyle.default

        private var verticalModeRowPadding: Float = StripeThemeDefaults.verticalModeRowPadding

        private var embeddedAppearance: Embedded =
            Embedded.default

        fun colorsLight(colors: Colors) = apply {
            this.colorsLight = colors
        }

        fun colorsDark(colors: Colors) = apply {
            this.colorsDark = colors
        }

        fun shapes(shapes: Shapes) = apply {
            this.shapes = shapes
        }

        fun typography(typography: Typography) = apply {
            this.typography = typography
        }

        fun primaryButton(primaryButton: PrimaryButton) = apply {
            this.primaryButton = primaryButton
        }

        fun embeddedAppearance(embeddedAppearance: Embedded) = apply {
            this.embeddedAppearance = embeddedAppearance
        }

        fun formInsetValues(insets: Insets) = apply {
            this.formInsetValues = insets
        }

        @AppearanceAPIAdditionsPreview
        fun sectionSpacing(sectionSpacing: Spacing) = apply {
            this.sectionSpacing = sectionSpacing
        }

        @AppearanceAPIAdditionsPreview
        fun textFieldInsets(textFieldInsets: Insets) = apply {
            this.textFieldInsets = textFieldInsets
        }

        @AppearanceAPIAdditionsPreview
        fun iconStyle(iconStyle: IconStyle) = apply {
            this.iconStyle = iconStyle
        }

        @AppearanceAPIAdditionsPreview
        fun verticalModeRowPadding(verticalModeRowPaddingDp: Float) = apply {
            this.verticalModeRowPadding = verticalModeRowPaddingDp
        }

        @OptIn(AppearanceAPIAdditionsPreview::class)
        fun build(): Appearance {
            return Appearance(
                colorsLight = colorsLight,
                colorsDark = colorsDark,
                shapes = shapes,
                typography = typography,
                primaryButton = primaryButton,
                embeddedAppearance = embeddedAppearance,
                formInsetValues = formInsetValues,
                sectionSpacing = sectionSpacing,
                textFieldInsets = textFieldInsets,
                iconStyle = iconStyle,
                verticalModeRowPadding = verticalModeRowPadding,
            )
        }
    }

    @Parcelize
    data class Colors(
        /**
         * A primary color used throughout PaymentSheet.
         */
        @ColorInt
        val primary: Int,

        /**
         * The color used for the surfaces (backgrounds) of PaymentSheet.
         */
        @ColorInt
        val surface: Int,

        /**
         * The color used for the background of inputs, tabs, and other components.
         */
        @ColorInt
        val component: Int,

        /**
         * The color used for borders of inputs, tabs, and other components.
         */
        @ColorInt
        val componentBorder: Int,

        /**
         * The color of the divider lines used inside inputs, tabs, and other components.
         */
        @ColorInt
        val componentDivider: Int,

        /**
         * The default color used for text and on other elements that live on components.
         */
        @ColorInt
        val onComponent: Int,

        /**
         * The color used for items appearing over the background in Payment Sheet.
         */
        @ColorInt
        val onSurface: Int,

        /**
         * The color used for text of secondary importance.
         * For example, this color is used for the label above input fields.
         */
        @ColorInt
        val subtitle: Int,

        /**
         * The color used for input placeholder text.
         */
        @ColorInt
        val placeholderText: Int,

        /**
         * The color used for icons in PaymentSheet, such as the close or back icons.
         */
        @ColorInt
        val appBarIcon: Int,

        /**
         * A color used to indicate errors or destructive actions in PaymentSheet.
         */
        @ColorInt
        val error: Int
    ) : Parcelable {
        constructor(
            primary: Color,
            surface: Color,
            component: Color,
            componentBorder: Color,
            componentDivider: Color,
            onComponent: Color,
            subtitle: Color,
            placeholderText: Color,
            onSurface: Color,
            appBarIcon: Color,
            error: Color
        ) : this(
            primary = primary.toArgb(),
            surface = surface.toArgb(),
            component = component.toArgb(),
            componentBorder = componentBorder.toArgb(),
            componentDivider = componentDivider.toArgb(),
            onComponent = onComponent.toArgb(),
            subtitle = subtitle.toArgb(),
            placeholderText = placeholderText.toArgb(),
            onSurface = onSurface.toArgb(),
            appBarIcon = appBarIcon.toArgb(),
            error = error.toArgb()
        )

        companion object {
            internal fun configureDefaultLight(
                primary: Color = StripeThemeDefaults.colorsLight.materialColors.primary,
                surface: Color = StripeThemeDefaults.colorsLight.materialColors.surface,
            ) = Colors(
                primary = primary,
                surface = surface,
                component = StripeThemeDefaults.colorsLight.component,
                componentBorder = StripeThemeDefaults.colorsLight.componentBorder,
                componentDivider = StripeThemeDefaults.colorsLight.componentDivider,
                onComponent = StripeThemeDefaults.colorsLight.onComponent,
                subtitle = StripeThemeDefaults.colorsLight.subtitle,
                placeholderText = StripeThemeDefaults.colorsLight.placeholderText,
                onSurface = StripeThemeDefaults.colorsLight.materialColors.onSurface,
                appBarIcon = StripeThemeDefaults.colorsLight.appBarIcon,
                error = StripeThemeDefaults.colorsLight.materialColors.error
            )

            val defaultLight = configureDefaultLight()

            internal fun configureDefaultDark(
                primary: Color = StripeThemeDefaults.colorsDark.materialColors.primary,
                surface: Color = StripeThemeDefaults.colorsDark.materialColors.surface,
            ) = Colors(
                primary = primary,
                surface = surface,
                component = StripeThemeDefaults.colorsDark.component,
                componentBorder = StripeThemeDefaults.colorsDark.componentBorder,
                componentDivider = StripeThemeDefaults.colorsDark.componentDivider,
                onComponent = StripeThemeDefaults.colorsDark.onComponent,
                subtitle = StripeThemeDefaults.colorsDark.subtitle,
                placeholderText = StripeThemeDefaults.colorsDark.placeholderText,
                onSurface = StripeThemeDefaults.colorsDark.materialColors.onSurface,
                appBarIcon = StripeThemeDefaults.colorsDark.appBarIcon,
                error = StripeThemeDefaults.colorsDark.materialColors.error
            )

            val defaultDark = configureDefaultDark()
        }
    }

    @Parcelize
    data class Shapes @AppearanceAPIAdditionsPreview constructor(
        /**
         * The corner radius used for tabs, inputs, buttons, and other components in PaymentSheet.
         */
        val cornerRadiusDp: Float,

        /**
         * The border used for inputs, tabs, and other components in PaymentSheet.
         */
        val borderStrokeWidthDp: Float,

        /**
         * The corner radius used for specifically for the sheets displayed by Payment Element. Be default, this is
         * set to the same value as [cornerRadiusDp].
         */
        val bottomSheetCornerRadiusDp: Float = cornerRadiusDp,
    ) : Parcelable {
        @OptIn(AppearanceAPIAdditionsPreview::class)
        constructor(
            /**
             * The corner radius used for tabs, inputs, buttons, and other components in PaymentSheet.
             */
            cornerRadiusDp: Float,

            /**
             * The border used for inputs, tabs, and other components in PaymentSheet.
             */
            borderStrokeWidthDp: Float,
        ) : this(
            cornerRadiusDp = cornerRadiusDp,
            borderStrokeWidthDp = borderStrokeWidthDp,
            bottomSheetCornerRadiusDp = cornerRadiusDp,
        )

        @OptIn(AppearanceAPIAdditionsPreview::class)
        constructor(
            context: Context,
            cornerRadiusDp: Int,
            borderStrokeWidthDp: Int,
        ) : this(
            cornerRadiusDp = context.getRawValueFromDimenResource(cornerRadiusDp),
            borderStrokeWidthDp = context.getRawValueFromDimenResource(borderStrokeWidthDp),
            bottomSheetCornerRadiusDp = context.getRawValueFromDimenResource(cornerRadiusDp),
        )

        companion object {
            val default = Shapes(
                cornerRadiusDp = StripeThemeDefaults.shapes.cornerRadius,
                borderStrokeWidthDp = StripeThemeDefaults.shapes.borderStrokeWidth
            )
        }
    }

    @Parcelize
    data class Typography @AppearanceAPIAdditionsPreview constructor(
        /**
         * The scale factor for all fonts in PaymentSheet, the default value is 1.0.
         * When this value increases fonts will increase in size and decrease when this value is lowered.
         */
        val sizeScaleFactor: Float,

        /**
         * The font used in text. This should be a resource ID value.
         */
        @FontRes
        val fontResId: Int?,

        /**
         * Custom font configuration for specific text styles
         * Note: When set, these fonts override the default font calculations for their respective text styles
         */
        val custom: Custom,
    ) : Parcelable {
        @OptIn(AppearanceAPIAdditionsPreview::class)
        constructor(
            /**
             * The scale factor for all fonts in PaymentSheet, the default value is 1.0.
             * When this value increases fonts will increase in size and decrease when this value is lowered.
             */
            sizeScaleFactor: Float,
            /**
             * The font used in text. This should be a resource ID value.
             */
            @FontRes
            fontResId: Int?
        ) : this(
            sizeScaleFactor = sizeScaleFactor,
            fontResId = fontResId,
            custom = Custom(),
        )

        @AppearanceAPIAdditionsPreview
        @Parcelize
        data class Custom(
            /**
             * The font used for headlines (e.g., "Add your payment information")
             *
             * Note: If `null`, uses the calculated font based on `base` and `sizeScaleFactor`
             */
            val h1: Font? = null,
        ) : Parcelable

        @AppearanceAPIAdditionsPreview
        @Parcelize
        data class Font(
            /**
             * The font used in text. This should be a resource ID value.
             */
            @FontRes
            val fontFamily: Int? = null,
            /**
             * The font size used for the text. This should represent a sp value.
             */
            val fontSizeSp: Float? = null,
            /**
             * The font weight used for the text.
             */
            val fontWeight: Int? = null,
            /**
             * The letter spacing used for the text. This should represent a sp value.
             */
            val letterSpacingSp: Float? = null,
        ) : Parcelable

        companion object {
            val default = Typography(
                sizeScaleFactor = StripeThemeDefaults.typography.fontSizeMultiplier,
                fontResId = StripeThemeDefaults.typography.fontFamily
            )
        }
    }

    @AppearanceAPIAdditionsPreview
    @Poko
    @Parcelize
    class Spacing(internal val spacingDp: Float) : Parcelable {
        internal companion object {
            val defaultSectionSpacing = Spacing(spacingDp = -1f)
        }
    }

    /**
     * Defines the visual style of icons in Payment Element
     */
    @AppearanceAPIAdditionsPreview
    enum class IconStyle {
        /**
         * Display icons with a filled appearance
         */
        Filled,

        /**
         * Display icons with an outlined appearance
         */
        Outlined;

        internal companion object {
            val default = Filled
        }
    }

    @Parcelize
    data class PrimaryButton(
        /**
         * Describes the colors used while the system is in light mode.
         */
        val colorsLight: Colors = Colors.defaultLight,
        /**
         * Describes the colors used while the system is in dark mode.
         */
        val colorsDark: Colors = Colors.defaultDark,
        /**
         * Describes the shape of the primary button.
         */
        val shape: Shape = Shape(),
        /**
         * Describes the typography of the primary button.
         */
        val typography: Typography = Typography()
    ) : Parcelable {
        @Parcelize
        data class Colors(
            /**
             * The background color of the primary button.
             * Note: If 'null', {@link Colors#primary} is used.
             */
            @ColorInt
            val background: Int?,
            /**
             * The color of the text and icon in the primary button.
             */
            @ColorInt
            val onBackground: Int,
            /**
             * The border color of the primary button.
             */
            @ColorInt
            val border: Int,
            /**
             * The background color for the primary button when in a success state. Defaults
             * to base green background color.
             */
            @ColorInt
            val successBackgroundColor: Int = PRIMARY_BUTTON_SUCCESS_BACKGROUND_COLOR.toArgb(),
            /**
             * The success color for the primary button text when in a success state. Defaults
             * to `onBackground`.
             */
            @ColorInt
            val onSuccessBackgroundColor: Int = onBackground,
        ) : Parcelable {
            constructor(
                background: Int?,
                onBackground: Int,
                border: Int
            ) : this(
                background = background,
                onBackground = onBackground,
                border = border,
                successBackgroundColor = PRIMARY_BUTTON_SUCCESS_BACKGROUND_COLOR.toArgb(),
                onSuccessBackgroundColor = onBackground,
            )

            constructor(
                background: Color?,
                onBackground: Color,
                border: Color
            ) : this(
                background = background?.toArgb(),
                onBackground = onBackground.toArgb(),
                border = border.toArgb(),
            )

            constructor(
                background: Color?,
                onBackground: Color,
                border: Color,
                successBackgroundColor: Color = PRIMARY_BUTTON_SUCCESS_BACKGROUND_COLOR,
                onSuccessBackgroundColor: Color = onBackground,
            ) : this(
                background = background?.toArgb(),
                onBackground = onBackground.toArgb(),
                border = border.toArgb(),
                successBackgroundColor = successBackgroundColor.toArgb(),
                onSuccessBackgroundColor = onSuccessBackgroundColor.toArgb(),
            )

            companion object {
                val defaultLight = Colors(
                    background = null,
                    onBackground = StripeThemeDefaults.primaryButtonStyle.colorsLight.onBackground.toArgb(),
                    border = StripeThemeDefaults.primaryButtonStyle.colorsLight.border.toArgb(),
                    successBackgroundColor =
                    StripeThemeDefaults.primaryButtonStyle.colorsLight.successBackground.toArgb(),
                    onSuccessBackgroundColor = StripeThemeDefaults.primaryButtonStyle.colorsLight.onBackground.toArgb(),
                )
                val defaultDark = Colors(
                    background = null,
                    onBackground = StripeThemeDefaults.primaryButtonStyle.colorsDark.onBackground.toArgb(),
                    border = StripeThemeDefaults.primaryButtonStyle.colorsDark.border.toArgb(),
                    successBackgroundColor =
                    StripeThemeDefaults.primaryButtonStyle.colorsDark.successBackground.toArgb(),
                    onSuccessBackgroundColor = StripeThemeDefaults.primaryButtonStyle.colorsDark.onBackground.toArgb(),
                )
            }
        }

        @Parcelize
        data class Shape(
            /**
             * The corner radius of the primary button.
             * Note: If 'null', {@link Shapes#cornerRadiusDp} is used.
             */
            val cornerRadiusDp: Float? = null,
            /**
             * The border width of the primary button.
             * Note: If 'null', {@link Shapes#borderStrokeWidthDp} is used.
             */
            val borderStrokeWidthDp: Float? = null,
            /**
             * The height of the primary button.
             * Note: If 'null', the default height is 48dp.
             */
            val heightDp: Float? = null
        ) : Parcelable {
            @Deprecated("Use @DimenRes constructor")
            constructor(
                context: Context,
                cornerRadiusDp: Int? = null,
                borderStrokeWidthDp: Int? = null
            ) : this(
                cornerRadiusDp = cornerRadiusDp?.let {
                    context.getRawValueFromDimenResource(it)
                },
                borderStrokeWidthDp = borderStrokeWidthDp?.let {
                    context.getRawValueFromDimenResource(it)
                }
            )

            constructor(
                context: Context,
                @DimenRes cornerRadiusRes: Int? = null,
                @DimenRes borderStrokeWidthRes: Int? = null,
                @DimenRes heightRes: Int? = null
            ) : this(
                cornerRadiusDp = cornerRadiusRes?.let {
                    context.getRawValueFromDimenResource(it)
                },
                borderStrokeWidthDp = borderStrokeWidthRes?.let {
                    context.getRawValueFromDimenResource(it)
                },
                heightDp = heightRes?.let {
                    context.getRawValueFromDimenResource(it)
                }
            )
        }

        @Parcelize
        data class Typography(
            /**
             * The font used in the primary button.
             * Note: If 'null', Appearance.Typography.fontResId is used.
             */
            @FontRes
            val fontResId: Int? = null,

            /**
             * The font size in the primary button.
             * Note: If 'null', {@link Typography#sizeScaleFactor} is used.
             */
            val fontSizeSp: Float? = null
        ) : Parcelable {
            constructor(
                context: Context,
                fontResId: Int? = null,
                fontSizeSp: Int
            ) : this(
                fontResId = fontResId,
                fontSizeSp = context.getRawValueFromDimenResource(fontSizeSp)
            )
        }
    }

    @Parcelize
    @Poko
    class Insets(
        val startDp: Float,
        val topDp: Float,
        val endDp: Float,
        val bottomDp: Float
    ) : Parcelable {
        constructor(
            context: Context,
            @DimenRes startRes: Int,
            @DimenRes topRes: Int,
            @DimenRes endRes: Int,
            @DimenRes bottomRes: Int
        ) : this(
            startDp = context.getRawValueFromDimenResource(startRes),
            topDp = context.getRawValueFromDimenResource(topRes),
            endDp = context.getRawValueFromDimenResource(endRes),
            bottomDp = context.getRawValueFromDimenResource(bottomRes)
        )

        constructor(
            horizontalDp: Float,
            verticalDp: Float
        ) : this(
            startDp = horizontalDp,
            topDp = verticalDp,
            endDp = horizontalDp,
            bottomDp = verticalDp
        )

        constructor(
            context: Context,
            @DimenRes horizontalRes: Int,
            @DimenRes verticalRes: Int
        ) : this(
            startDp = context.getRawValueFromDimenResource(horizontalRes),
            topDp = context.getRawValueFromDimenResource(verticalRes),
            endDp = context.getRawValueFromDimenResource(horizontalRes),
            bottomDp = context.getRawValueFromDimenResource(verticalRes)
        )

        companion object {
            internal val defaultFormInsetValues = Insets(
                startDp = 20f,
                topDp = 0f,
                endDp = 20f,
                bottomDp = 40f,
            )

            internal val defaultTextFieldInsets = Insets(
                startDp = StripeThemeDefaults.textFieldInsets.start,
                topDp = StripeThemeDefaults.textFieldInsets.top,
                endDp = StripeThemeDefaults.textFieldInsets.end,
                bottomDp = StripeThemeDefaults.textFieldInsets.bottom,
            )
        }
    }
}
