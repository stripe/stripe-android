package com.stripe.android.link.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.stripe.android.link.R

@Preview
@Composable
fun LinkTerms(
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center
) {
    Text(
        text = stringResource(R.string.sign_up_terms),
        modifier = modifier,
        textAlign = textAlign,
        style = MaterialTheme.typography.caption
    )
}
