package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.getBorderStroke
import com.stripe.android.uicore.stripeColors

/**
 * This is a simple section that holds content in a card view.  It has a label, content specified
 * by the caller, and an error string.
 */
@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Section(
    @StringRes title: Int?,
    error: String?,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    content: @Composable () -> Unit
) {
    Column(modifier) {
        SectionTitle(title)
        SectionCard(
            isSelected = isSelected,
            content = content,
        )

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
        H6Text(
            text = stringResource(titleText),
            modifier = Modifier
                .padding(bottom = 4.dp)
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
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    isSelected: Boolean = false,
    backgroundColor: Color = MaterialTheme.stripeColors.component,
    border: BorderStroke = MaterialTheme.getBorderStroke(isSelected),
    content: @Composable () -> Unit
) {
    Card(
        border = border,
        // TODO(skyler-stripe): this will change when we add shadow configurations.
        elevation = if (isSelected) 1.5.dp else 0.dp,
        backgroundColor = backgroundColor,
        shape = shape,
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun SectionError(
    error: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = error,
        color = MaterialTheme.colors.error,
        style = MaterialTheme.typography.h6,
        modifier = modifier
            .padding(top = 2.dp)
            .semantics(mergeDescendants = true) { }
    )
}
