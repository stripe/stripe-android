@file:JvmName("LoadingButtonKt")

package com.stripe.android.identity.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal enum class LoadingButtonState {
    Idle, Loading, Disabled
}

@Composable
internal fun LoadingButton(
    modifier: Modifier = Modifier,
    text: String,
    state: LoadingButtonState,
    onClick: () -> Unit
) {
    Box(modifier = modifier) {
        Button(
            onClick = {
                onClick()
            },
            enabled = state == LoadingButtonState.Idle,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
        ) {
            Text(text = text)
        }
        if (state == LoadingButtonState.Loading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(24.dp)
                    .padding(top = 4.dp, end = 8.dp),
                strokeWidth = 3.dp
            )
        }
    }
}

@Composable
internal fun LoadingTextButton(
    modifier: Modifier = Modifier,
    text: String,
    state: LoadingButtonState,
    onClick: () -> Unit
) {
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = {
                onClick()
            },
            enabled = state == LoadingButtonState.Idle,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
        ) {
            Text(text = text)
        }
        if (state == LoadingButtonState.Loading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(24.dp)
                    .padding(top = 4.dp, end = 8.dp),
                strokeWidth = 3.dp
            )
        }
    }
}
