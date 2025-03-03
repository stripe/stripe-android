package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.text.Html

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val MANDATE_TEST_TAG = "mandate_test_tag"

@Composable
internal fun MandateTextUI(
    element: MandateTextElement,
    modifier: Modifier = Modifier,
) {
    Mandate(stringResource(element.stringResId, *element.args.toTypedArray()), modifier)
}

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Mandate(
    mandateText: String?,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Left,
) {
    mandateText?.let {
        val caption = MaterialTheme.typography.caption
        val lineHeight = caption.calculateLineHeight(spacing = 3)

        Html(
            html = mandateText,
            color = MaterialTheme.stripeColors.subtitle,
            style = caption.copy(
                textAlign = textAlign,
                lineHeight = lineHeight,
                fontSize = 11.sp * StripeTheme.typographyMutable.fontSizeMultiplier,
                fontWeight = FontWeight.Normal
            ),
            modifier = modifier
                .semantics(mergeDescendants = true) {} // Makes it a separate accessible item.
                .testTag(MANDATE_TEST_TAG),
            urlSpanStyle = SpanStyle(
                color = MaterialTheme.colors.primary
            ),
        )
    }
}

private fun TextStyle.calculateLineHeight(spacing: Int): TextUnit {
    return if (fontSize.isSp) {
        (fontSize.value + spacing).sp
    } else {
        fontSize
    }
}
