package com.stripe.android.paymentsheet.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.text.Html

@Composable
internal fun Mandate(
    mandateText: String?,
    modifier: Modifier = Modifier,
) {
    mandateText?.let { text ->
        val body = MaterialTheme.typography.body1
        val lineHeight = body.calculateLineHeight(spacing = 4)

        Html(
            html = text,
            color = MaterialTheme.stripeColors.subtitle,
            style = body.copy(
                textAlign = TextAlign.Left,
                lineHeight = lineHeight,
                fontSize = 11.sp,
            ),
            modifier = modifier,
        )
    }
}

@Preview(widthDp = 400)
@Composable
internal fun MandatePreview() {
    StripeTheme {
        Mandate("A super long mandate that just keeps going and going, and it just won't freaking end ever")
    }
}

private fun TextStyle.calculateLineHeight(spacing: Int): TextUnit {
    return if (fontSize.isSp) {
        (fontSize.value + spacing).sp
    } else {
        fontSize
    }
}
