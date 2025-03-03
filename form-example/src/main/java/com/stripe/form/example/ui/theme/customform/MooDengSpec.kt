package com.stripe.form.example.ui.theme.customform

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.stripe.android.form.example.R
import com.stripe.form.ContentSpec

class MooDengSpec: ContentSpec {
    @Composable
    override fun Content(modifier: Modifier) {
        Image(
            modifier = modifier,
            painter = painterResource(R.drawable.moo_deng),
            contentDescription = null,
            contentScale = ContentScale.Fit
        )
    }
}