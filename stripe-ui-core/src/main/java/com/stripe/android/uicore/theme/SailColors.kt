package com.stripe.android.uicore.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * This class reflects the colors used in the Sail theme.
 * They should be kept in sync with the color declaration in Figma:
 * https://www.figma.com/file/WAFvoybps5uk3FL4tftaee/Sail?type=design
 */
@Immutable
internal data class SailColors(
    val textDefault: Color,
    val textSubdued: Color,
    val textDisabled: Color,
    val textWhite: Color,
    val textBrand: Color,
    val textCritical: Color,
    val iconDefault: Color,
    val iconSubdued: Color,
    val iconWhite: Color,
    val iconBrand: Color,
    val buttonPrimary: Color,
    val buttonPrimaryHover: Color,
    val buttonPrimaryPressed: Color,
    val buttonSecondary: Color,
    val buttonSecondaryHover: Color,
    val buttonSecondaryPressed: Color,
    val backgroundSurface: Color,
    val background: Color,
    val backgroundOffset: Color,
    val backgroundBrand: Color,
    val border: Color,
    val borderBrand: Color
)

internal val sailColors = SailColors(
    textDefault = Color(0xFF353A44),
    textSubdued = Color(0xFF596171),
    textDisabled = Color(0xFF818DA0),
    textWhite = Color(0xFFFFFFFF),
    textBrand = Color(0xFF533AFD),
    textCritical = Color(0xFFC0123C),
    iconDefault = Color(0xFF474E5A),
    iconSubdued = Color(0xFF6C7688),
    iconWhite = Color(0xFFFFFFFF),
    iconBrand = Color(0xFF675DFF),
    buttonPrimary = Color(0xFF675DFF),
    buttonPrimaryHover = Color(0xFF857AFE),
    buttonPrimaryPressed = Color(0xFF533AFD),
    buttonSecondary = Color(0xFFF5F6F8),
    buttonSecondaryHover = Color(0xFFF5F6F8),
    buttonSecondaryPressed = Color(0xFFEBEEF1),
    background = Color(0xFFF5F6F8),
    backgroundSurface = Color(0xFFFFFFFF),
    backgroundOffset = Color(0xFFF6F8FA),
    backgroundBrand = Color(0xFFF5F6F8),
    border = Color(0xFFD8DEE4),
    borderBrand = Color(0xFF675DFF)
)
