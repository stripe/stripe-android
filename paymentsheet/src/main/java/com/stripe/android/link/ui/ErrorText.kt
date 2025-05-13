package com.stripe.android.link.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.elements.HyperlinkedText

internal sealed class ErrorTextStyle {
    abstract val shape: Shape
    abstract val iconModifier: Modifier
    abstract val textModifier: Modifier
    abstract val textStyle: TextStyle

    internal object Small : ErrorTextStyle() {
        override val shape = RoundedCornerShape(4.dp)
        override val iconModifier = Modifier
            .padding(top = 4.dp, bottom = 4.dp, end = 4.dp)
            .size(12.dp)
        override val textModifier = Modifier
            .padding(top = 4.dp, end = 4.dp, bottom = 4.dp)
        override val textStyle = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }

    internal object Medium : ErrorTextStyle() {
        override val shape = RoundedCornerShape(8.dp)
        override val iconModifier = Modifier
            .padding(end = 6.dp, top = 12.dp, bottom = 12.dp)
            .size(20.dp)
        override val textModifier = Modifier
            .padding(top = 12.dp, end = 12.dp, bottom = 12.dp)
        override val textStyle = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Preview
@Composable
private fun ErrorTextPreview() {
    DefaultLinkTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ErrorText(
                text = "Test error message",
                style = ErrorTextStyle.Small
            )
            ErrorText(
                text = "Test error message",
                style = ErrorTextStyle.Medium
            )
        }
    }
}

@Composable
internal fun ErrorText(
    text: String,
    modifier: Modifier = Modifier,
    style: ErrorTextStyle = ErrorTextStyle.Medium
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.stripe_ic_sail_warning_circle),
            contentDescription = null,
            modifier = style.iconModifier,
            tint = LinkTheme.colors.iconCritical
        )
        HyperlinkedText(
            text = text,
            modifier = style.textModifier,
            color = LinkTheme.colors.textCritical,
            style = style.textStyle
        )
    }
}
