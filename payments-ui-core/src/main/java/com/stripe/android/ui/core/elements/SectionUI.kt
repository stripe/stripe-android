package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stripe.android.ui.core.getBorderStroke
import com.stripe.android.ui.core.paymentsColors

/**
 * This is a simple section that holds content in a card view.  It has a label, content specified
 * by the caller, and an error string.
 */
@Composable
internal fun Section(
    @StringRes title: Int?,
    error: String?,
    contentOutsideCard: @Composable () -> Unit = {},
    contentInCard: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        SectionTitle(title)
        SectionCard(content = contentInCard)
        if (error != null) {
            SectionError(error)
        }
        contentOutsideCard()
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
    isSelected: Boolean = false,
    content: @Composable () -> Unit
) {
    Card(
        border = MaterialTheme.getBorderStroke(isSelected),
        // TODO(skyler-stripe): this will change when we add shadow configurations.
        elevation = if (isSelected) 1.5.dp else 0.dp,
        backgroundColor = MaterialTheme.paymentsColors.component,
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
internal fun SectionError(error: String) {
    Text(
        text = error,
        color = MaterialTheme.colors.error,
        style = MaterialTheme.typography.h6,
        modifier = Modifier.semantics(mergeDescendants = true) { }
    )
}
