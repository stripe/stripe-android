package com.stripe.android.ui.core.elements

import android.content.Intent
import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.StripeTheme

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun AuBecsDebitMandateElementUI(
    element: AuBecsDebitMandateTextElement
) {

    val annotatedText = buildAnnotatedString {
        val nonLinkTextStyle = SpanStyle(
            color = StripeTheme.colors.colorTextSecondary,
            fontSize = element.fontSizeSp.sp,
            letterSpacing = element.letterSpacingSp.sp,
        )

        withStyle(style = nonLinkTextStyle) {
            append(stringResource(R.string.au_becs_mandate_pre_link))
        }

        // We attach this *URL* annotation to the following content
        // until `pop()` is called
        pushStringAnnotation(
            tag = "URL",
            annotation = "https://stripe.com/au-becs-dd-service-agreement/legal"
        )
        withStyle(
            style = SpanStyle(
                color = StripeTheme.colors.colorTextSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = element.fontSizeSp.sp,
                letterSpacing = element.letterSpacingSp.sp,
                textDecoration = TextDecoration.Underline
            )
        ) {
            append(" ")
            append(stringResource(R.string.au_becs_mandate_link))
        }
        pop()

        withStyle(style = nonLinkTextStyle) {
            append(stringResource(R.string.au_becs_mandate_post_link, element.merchantName ?: ""))
        }
    }

    val context = LocalContext.current
    ClickableText(
        text = annotatedText,
        onClick = { offset ->
            annotatedText.getStringAnnotations(
                tag = "URL", start = offset,
                end = offset
            )
                .firstOrNull()?.let { annotation ->
                    val openURL = Intent(Intent.ACTION_VIEW)
                    openURL.data = Uri.parse(annotation.item)
                    context.startActivity(openURL)
                }
        },

        modifier = Modifier
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) {}, // makes it a separate accessibile item
    )
}
