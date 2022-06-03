package com.stripe.android.link.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.link.R
import com.stripe.android.link.theme.linkColors

/**
 * A Composable that is shown in the ModalBottomSheetLayout.
 */
typealias BottomSheetContent = @Composable ColumnScope.() -> Unit

@Composable
internal fun ScrollableTopLevelColumn(
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()
        }
    }
}

@Preview
@Composable
private fun ErrorTextPreview() {
    ErrorText(text = "Test error message")
}

@Composable
internal fun ErrorText(
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.linkColors.errorComponentBackground,
                shape = MaterialTheme.shapes.small
            )
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_link_error),
            contentDescription = null,
            modifier = Modifier
                .padding(12.dp),
            tint = MaterialTheme.linkColors.errorText
        )
        Text(
            text = text,
            modifier = Modifier.padding(top = 12.dp, end = 12.dp, bottom = 12.dp),
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.linkColors.errorText
        )
    }
}
