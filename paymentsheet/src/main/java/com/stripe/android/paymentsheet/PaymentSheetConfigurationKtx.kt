package com.stripe.android.paymentsheet

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.ui.core.PaymentsComposeColors
import com.stripe.android.ui.core.PaymentsComposeShapes
import com.stripe.android.ui.core.PaymentsTheme
import java.security.InvalidParameterException

internal fun PaymentSheet.Configuration.validate() {
    // These are not localized as they are not intended to be displayed to a user.
    when {
        merchantDisplayName.isBlank() -> {
            throw InvalidParameterException(
                "When a Configuration is passed to PaymentSheet," +
                    " the Merchant display name cannot be an empty string."
            )
        }
        customer?.id?.isBlank() == true -> {
            throw InvalidParameterException(
                "When a CustomerConfiguration is passed to PaymentSheet," +
                    " the Customer ID cannot be an empty string."
            )
        }
        customer?.ephemeralKeySecret?.isBlank() == true -> {
            throw InvalidParameterException(
                "When a CustomerConfiguration is passed to PaymentSheet, " +
                    "the ephemeralKeySecret cannot be an empty string."
            )
        }
    }
}

internal fun PaymentSheet.Configuration.parseAppearance() {
    PaymentsTheme.colorsLight = PaymentsComposeColors(
        colorComponentBackground = Color(appearance.colorsLight.componentBackground),
        colorComponentBorder = Color(appearance.colorsLight.componentBorder),
        colorComponentDivider = Color(appearance.colorsLight.componentDivider),
        colorTextSecondary = Color(appearance.colorsLight.textSecondary),
        colorTextCursor = Color.White,
        placeholderText = Color(appearance.colorsLight.placeholderText),

        material = lightColors(
            primary = Color(appearance.colorsLight.primary),
            onPrimary = Color(appearance.colorsLight.onPrimary),
            surface = Color(appearance.colorsLight.surface),
            onBackground = Color(appearance.colorsLight.onBackground),
            error = Color(appearance.colorsLight.error),
        )
    )

    PaymentsTheme.colorsDark = PaymentsComposeColors(
        colorComponentBackground = Color(appearance.colorsDark.componentBackground),
        colorComponentBorder = Color(appearance.colorsDark.componentBorder),
        colorComponentDivider = Color(appearance.colorsDark.componentDivider),
        colorTextSecondary = Color(appearance.colorsDark.textSecondary),
        colorTextCursor = Color.Black,
        placeholderText = Color(appearance.colorsDark.placeholderText),

        material = darkColors(
            primary = Color(appearance.colorsDark.primary),
            onPrimary = Color(appearance.colorsDark.onPrimary),
            surface = Color(appearance.colorsDark.surface),
            onBackground = Color(appearance.colorsDark.onBackground),
            error = Color(appearance.colorsDark.error),
        )
    )

    PaymentsTheme.shapes = PaymentsComposeShapes(
        borderStrokeWidth = appearance.shapes.borderStrokeWidthDp.dp,
        borderStrokeWidthSelected = appearance.shapes.borderStrokeWidthSelected.dp,
        material = Shapes(
            small = RoundedCornerShape(appearance.shapes.cornerRadiusDp.dp),
            medium = RoundedCornerShape(appearance.shapes.cornerRadiusDp.dp)
        )
    )

    // h4 is our largest headline. It is used for the most important labels in our UI
    // ex: "Select your payment method" in Payment Sheet.
    val h4 = TextStyle.Default.copy(
        fontFamily = FontFamily(Font(appearance.typography.fontResId)),
        fontSize = (PaymentsTheme.extraLargeFont * appearance.typography.sizeScaleFactor).sp,
        fontWeight = FontWeight(appearance.typography.boldWeight),
    )

    // h5 is our medium headline label.
    // ex: "Pay $50.99" in Payment Sheet's buy button.
    val h5 = TextStyle.Default.copy(
        fontFamily = FontFamily(Font(appearance.typography.fontResId)),
        fontSize = (PaymentsTheme.largeFont * appearance.typography.sizeScaleFactor).sp,
        fontWeight = FontWeight(appearance.typography.mediumWeight),
        letterSpacing = (-0.32).sp
    )

    // h6 is our smallest headline label.
    // ex: Section labels in Payment Sheet
    val h6 = TextStyle.Default.copy(
        fontFamily = FontFamily(Font(appearance.typography.fontResId)),
        fontSize = (PaymentsTheme.smallFont * appearance.typography.sizeScaleFactor).sp,
        fontWeight = FontWeight(appearance.typography.mediumWeight),
        letterSpacing = (-0.15).sp
    )

    // body1 is our larger body text. Used for the bulk of our elements and forms.
    // ex: the text used in Payment Sheet's text form elements.
    val body1 = TextStyle.Default.copy(
        fontFamily = FontFamily(Font(appearance.typography.fontResId)),
        fontSize = (PaymentsTheme.mediumFont * appearance.typography.sizeScaleFactor).sp,
        fontWeight = FontWeight(appearance.typography.normalWeight),
    )

    // subtitle1 is our only subtitle size. Used for labeling fields.
    // ex: the placeholder texts that appear when you type in Payment Sheet's forms.
    val subtitle1 = TextStyle.Default.copy(
        fontFamily = FontFamily(Font(appearance.typography.fontResId)),
        fontSize = (PaymentsTheme.mediumFont * appearance.typography.sizeScaleFactor).sp,
        fontWeight = FontWeight(appearance.typography.normalWeight),
        letterSpacing = (-0.15).sp
    )

    // caption is used to label images in payment sheet.
    // ex: the labels under our payment method selectors in Payment Sheet.
    val caption = TextStyle.Default.copy(
        fontFamily = FontFamily(Font(appearance.typography.fontResId)),
        fontSize = (PaymentsTheme.xsmallFont * appearance.typography.sizeScaleFactor).sp,
        fontWeight = FontWeight(appearance.typography.mediumWeight)
    )

    // body2 is our smaller body text. Used for less important fields that are not required to
    // read. Ex: our mandate texts in Payment Sheet.
    val body2 = TextStyle.Default.copy(
        fontFamily = FontFamily(Font(appearance.typography.fontResId)),
        fontSize = (PaymentsTheme.xxsmallFont * appearance.typography.sizeScaleFactor).sp,
        fontWeight = FontWeight(appearance.typography.normalWeight),
        letterSpacing = (-0.15).sp
    )

    PaymentsTheme.typography = Typography(
        body1 = body1,
        body2 = body2,
        h4 = h4,
        h5 = h5,
        h6 = h6,
        subtitle1 = subtitle1,
        caption = caption
    )
}
