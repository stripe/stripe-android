package com.stripe.android.paymentsheet.elements.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * This is a simple section that holds content in a card view.  It has a label, content specified
 * by the caller, and an error string.
 */
@ExperimentalAnimationApi
@Composable
internal fun Section(error: String?, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Card {
            content()
        }
        AnimatedVisibility(error != null) {
            SectionError(error!!)
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
        color = MaterialTheme.colors.error
    )
}