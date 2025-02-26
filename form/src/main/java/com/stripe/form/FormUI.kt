package com.stripe.form

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FormUI(
    modifier: Modifier = Modifier,
    form: Form
) {
    Column(modifier = modifier) {
        form.content.forEach { spec ->
            spec.Content(Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}