package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.getBorderStroke
import com.stripe.android.uicore.stripeColors
import androidx.compose.material.Card as MaterialCard

/**
 * This is a simple section that holds content in a card view.  It has a label, content specified
 * by the caller, and an error string.
 */
@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Section(
    title: String?,
    error: String?,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    footer: @Composable () -> Unit = {},
) {
    Column(modifier) {
        if (title != null) {
            SectionTitle(title)
            Spacer(modifier = Modifier.requiredHeight(4.dp))
        }

        Card(content = content)

        if (error != null) {
            Spacer(modifier = Modifier.requiredHeight(8.dp))
            SectionError(error)
        }

        footer()
    }
}

/**
 * This is the layout for the section title
 */
@Composable
private fun SectionTitle(text: String) {
    H6Text(
        text = text,
        modifier = Modifier.semantics(mergeDescendants = true) { // Need to prevent form as focusable accessibility
            heading()
        },
    )
}

/**
 * This is the layout for the section card.
 */
@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Card(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    backgroundColor: Color = MaterialTheme.stripeColors.component,
    border: BorderStroke = MaterialTheme.getBorderStroke(isSelected),
    content: @Composable () -> Unit
) {
    MaterialCard(
        border = border,
        // TODO(skyler-stripe): this will change when we add shadow configurations.
        elevation = if (isSelected) 1.5.dp else 0.dp,
        backgroundColor = backgroundColor,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
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
private fun SectionError(error: String) {
    Text(
        text = error,
        color = MaterialTheme.colors.error,
        style = MaterialTheme.typography.h6,
        modifier = Modifier.semantics(mergeDescendants = true) { }
    )
}
