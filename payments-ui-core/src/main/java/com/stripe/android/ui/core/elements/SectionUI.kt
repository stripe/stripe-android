package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.ui.core.PaymentsTheme

/**
 * This is the style for the section card
 */

internal object CardStyle {
    val cardBorderColor: Color
        @Composable
        @ReadOnlyComposable
        get() = PaymentsTheme.colors.colorComponentBorder

    val cardDividerColor: Color
        @Composable
        @ReadOnlyComposable
        get() = PaymentsTheme.colors.colorComponentDivider

    val cardBorderWidth: Dp
        @Composable
        @ReadOnlyComposable
        get() = PaymentsTheme.shapes.borderStrokeWidth

    val cardElevation: Dp = 0.dp
}

/**
 * This is the style for the section title.
 *
 * Once credit card is converted use one of the default material theme styles.
 */
internal object SectionTitle {
    val color: Color
        @Composable
        @ReadOnlyComposable
        get() = PaymentsTheme.colors.colorTextSecondary

    val fontWeight: FontWeight = FontWeight.Bold
    val letterSpacing: TextUnit = (-0.01f).sp
    val fontSize: TextUnit = 13.sp
}

/**
 * This is a simple section that holds content in a card view.  It has a label, content specified
 * by the caller, and an error string.
 */
@Composable
internal fun Section(
    @StringRes title: Int?,
    error: String?,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        SectionTitle(title)
        SectionCard(content)
        if (error != null) {
            SectionError(error)
        }
    }
}

/**
 * This is the layout for the section title
 */
@Composable
internal fun SectionTitle(@StringRes titleText: Int?) {
    titleText?.let {
        Text(
            text = stringResource(titleText),
            color = SectionTitle.color,
            style = MaterialTheme.typography.h6.copy(
                fontSize = SectionTitle.fontSize,
                fontWeight = SectionTitle.fontWeight,
                letterSpacing = SectionTitle.letterSpacing,
            ),
            modifier = Modifier
                .padding(vertical = 4.dp)
                .semantics(mergeDescendants = true) { // Need to prevent form as focusable accessibility
                    heading()
                }
        )
    }
}

/**
 * This is the layout for the section card.
 */
@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun SectionCard(
    content: @Composable () -> Unit
) {
    Card(
        border = BorderStroke(CardStyle.cardBorderWidth, CardStyle.cardBorderColor),
        elevation = CardStyle.cardElevation,
    ) {
        Column {
            content()
        }
    }
}

/**
 * This is how error string for the section are displayed.
 */
@Composable
internal fun SectionError(error: String) {
    Text(
        text = error,
        color = PaymentsTheme.colors.material.error,
        modifier = Modifier.semantics(mergeDescendants = true) { }
    )
}
