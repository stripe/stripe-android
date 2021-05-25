package com.stripe.android.compose.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp


private val SECTION_CARD_VERTICAL_PADDING = 2.dp
private val SECTION_CARD_ELEVATION = 8.dp

/**
 * This is a simple section that holds several fields in a card view.  It has a label, fields specified
 * by the caller, and an error string.
 */
@Composable
internal fun Section(label: Int, error: Int?, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(top = 2.dp)) {
        Text(
            text = stringResource(id = label)
        )
        Card(
            Modifier
                .fillMaxWidth()
                .padding(vertical = SECTION_CARD_VERTICAL_PADDING),
            elevation = SECTION_CARD_ELEVATION
        ) {
            Column {
                content()
            }
        }
        SectionError(error?.let{stringResource(error, stringResource(label))} ?:"")
    }
}

/**
 * This is how error string for the section are displayed.
 */
@Composable
internal fun SectionError(error: String?) {
    Text(
        text = error ?: "",
        color = MaterialTheme.colors.error
    )
}