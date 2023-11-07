package com.stripe.elements_ui

import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

@Composable
fun XLargeText(text: String) {
    Text(
        text = text,
        color = LocalContentColor.current,
        style = ElementsTheme.typography.xLarge,
    )
}

@Composable
fun LargeText(text: String) {
    Text(
        text = text,
        color = LocalContentColor.current,
        style = ElementsTheme.typography.large,
    )
}

@Composable
fun MediumText(text: String) {
    Text(
        text = text,
        color = LocalContentColor.current,
        style = ElementsTheme.typography.medium,
    )
}

@Composable
fun SmallText(text: String) {
    Text(
        text = text,
        color = LocalContentColor.current,
        style = ElementsTheme.typography.small,
    )
}

@Composable
fun ErrorText(text: String) {
    Text(
        text = text,
        color = ElementsTheme.colors.error,
        style = ElementsTheme.typography.small.copy(
            fontWeight = FontWeight.Normal,
        ),
    )
}

@Composable
fun XSmallText(text: String) {
    Text(
        text = text,
        color = LocalContentColor.current,
        style = ElementsTheme.typography.xSmall,
    )
}

@Composable
fun XXSmallText(text: String) {
    Text(
        text = text,
        color = LocalContentColor.current,
        style = ElementsTheme.typography.xxSmall,
    )
}
