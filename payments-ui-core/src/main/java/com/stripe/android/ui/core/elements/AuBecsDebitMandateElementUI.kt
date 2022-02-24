package com.stripe.android.ui.core.elements

import android.util.Log
import androidx.annotation.RestrictTo
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
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

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun AuBecsDebitMandateElementUI(
    element: AuBecsDebitMandateTextElement
) {

    val annotatedText = buildAnnotatedString {
        val nonLinkTextStyle = SpanStyle(
            color = when {
                element.color != null -> {
                    colorResource(element.color)
                }
                isSystemInDarkTheme() -> {
                    Color.LightGray
                }
                else -> {
                    Color.Black
                }
            },
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
            color = Color.Blue,
            fontWeight = FontWeight.Bold,
            fontSize = element.fontSizeSp.sp,
            letterSpacing = element.letterSpacingSp.sp,
            textDecoration = TextDecoration.Underline
        )
        ) {
            append(" Direct Debit Request service agreement")
        }
        pop()

        withStyle(style = nonLinkTextStyle) {
            append(stringResource(R.string.au_becs_mandate_post_link, element.merchantName ?: ""))
        }
    }

    ClickableText(
        text = annotatedText,
        onClick = { offset ->
            // We check if there is an *URL* annotation attached to the text
            // at the clicked position
            annotatedText.getStringAnnotations(
                tag = "URL", start = offset,
                end = offset
            )
                .firstOrNull()?.let { annotation ->
//                    val openURL = Intent(Intent.ACTION_VIEW)
//                    openURL.data = Uri.parse(annotation.item)
//                    startActivity(openURL)

                    // If yes, we log its value
                    Log.d("Clicked URL", annotation.item)
                }
        },

        modifier = Modifier
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) {}, // makes it a separate accessibile item
    )
}
