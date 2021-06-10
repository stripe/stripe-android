package com.stripe.android.paymentsheet.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * This is a simple section that holds several fields in a card view.  It has a label, fields specified
 * by the caller, and an error string.
 */
@Composable
internal fun Section(label: String, error: Int?, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Card {
            content()
        }
        SectionError(error?.let { stringResource(error, label) } ?: "")
    }
}

/**
 * This is how error string for the section are displayed.
 */
@Composable
internal fun SectionError(error: String) {
    Text(
        text = error,
        color = MaterialTheme.colors.error
    )
}