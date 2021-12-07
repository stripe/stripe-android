package com.stripe.android.ui.core.elements

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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

/**
 * This is the style for the section card
 */
internal data class CardStyle(
    private val isDarkTheme: Boolean,
    val cardBorderColor: Color = if (isDarkTheme) {
        Color(0xFF787880)
    } else {
        Color(0x14000000)
    },
    val cardBorderWidth: Dp = 1.dp,
    val cardElevation: Dp = 0.dp,
    val cardStyleBackground: Color = Color(0x20FFFFFF)
)

/**
 * This is the style for the section title.
 *
 * Once credit card is converted use one of the default material theme styles.
 */
internal data class SectionTitle constructor(
    val light: Color = Color.DarkGray,
    val dark: Color = Color.White,
    val fontWeight: FontWeight = FontWeight.Bold,
    val paddingBottom: Dp = 4.dp,
    val letterSpacing: TextUnit = (-0.01f).sp,
    val fontSize: TextUnit = 13.sp
)

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
    val sectionTitle = SectionTitle()
    titleText?.let {
        Text(
            text = stringResource(titleText),
            color = if (isSystemInDarkTheme()) {
                sectionTitle.dark
            } else {
                sectionTitle.light
            },
            style = MaterialTheme.typography.h6.copy(
                fontSize = sectionTitle.fontSize,
                fontWeight = sectionTitle.fontWeight,
                letterSpacing = sectionTitle.letterSpacing,
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
internal fun SectionCard(
    content: @Composable () -> Unit
) {
    val cardStyle = CardStyle(isSystemInDarkTheme())
    Card(
        border = BorderStroke(cardStyle.cardBorderWidth, cardStyle.cardBorderColor),
        elevation = cardStyle.cardElevation,
        backgroundColor = cardStyle.cardStyleBackground
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
        color = MaterialTheme.colors.error,
        modifier = Modifier.semantics(mergeDescendants = true) { }
    )
}
