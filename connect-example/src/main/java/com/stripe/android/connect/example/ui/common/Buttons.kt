package com.stripe.android.connect.example.ui.common

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.stripe.android.connect.example.R

@Composable
fun BackIconButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.back)
        )
    }
}

@Composable
fun MoreIconButton(
    onClick: () -> Unit,
    contentDescription: String = stringResource(R.string.more),
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = contentDescription,
        )
    }
}
