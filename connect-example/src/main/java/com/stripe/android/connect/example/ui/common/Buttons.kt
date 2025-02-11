package com.stripe.android.connect.example.ui.common

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
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
fun CustomizeAppearanceIconButton(
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.ic_material_palette),
            contentDescription = stringResource(R.string.customize_appearance),
        )
    }
}
