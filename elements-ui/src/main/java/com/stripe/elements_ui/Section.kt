package com.stripe.elements_ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material.Card as MaterialCard

@Composable
fun Section(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable (ColumnScope.() -> Unit)? = null,
    footer: @Composable (ColumnScope.() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth()) {
        if (header != null) {
            header()
            Spacer(modifier = Modifier.requiredHeight(8.dp))
        }

        MaterialCard(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(
                width = 1.dp,
                color = ElementsTheme.colors.componentBorder,
            )
        ) {
            content()
        }

        if (footer != null) {
            Spacer(modifier = Modifier.requiredHeight(8.dp))
            footer()
        }
    }
}

@Composable
@Preview
private fun SectionPreview() {
    ElementsTheme {
        Section(
            content = {
                Box(modifier = Modifier.padding(16.dp)) {
                    MediumText(
                        text = "Content",
                    )
                }
            },
            header = {
                XLargeText("Header")
            },
            footer = {
                ErrorText(text = "Something went wrong")
            }
        )
    }
}
