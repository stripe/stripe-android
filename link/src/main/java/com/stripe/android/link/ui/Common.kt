package com.stripe.android.link.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A Composable that is shown in the ModalBottomSheetLayout.
 */
typealias BottomSheetContent = @Composable ColumnScope.() -> Unit

@Composable
internal fun ScrollableTopLevelColumn(
    omitTopPadding: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val padding = remember(omitTopPadding) {
        if (omitTopPadding) {
            PaddingValues(
                start = 20.dp,
                top = 0.dp,
                end = 20.dp,
                bottom = 20.dp
            )
        } else {
            PaddingValues(all = 20.dp)
        }
    }

    Box(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()
        }
    }
}
