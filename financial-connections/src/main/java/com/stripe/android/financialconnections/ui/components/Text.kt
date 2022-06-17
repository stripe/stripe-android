package com.stripe.android.financialconnections.ui.components

import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun SpannedBody(
    list: List<SpanSection>
) {
    val clickableTextSpanStyle = FinancialConnectionsTheme.typography.bodyEmphasized
        .toSpanStyle()
        .copy(color = FinancialConnectionsTheme.colors.textBrand)

    val annotatedString = buildAnnotatedString {
        list.forEach {
            when (it) {
                is SpanSection.Raw -> append(it.text)
                is SpanSection.Link -> {
                    pushStringAnnotation(tag = it.tag, annotation = it.tag)
                    withStyle(style = clickableTextSpanStyle) {
                        append(it.text)
                    }
                    pop()
                }
            }
        }
    }
    ClickableText(
        text = annotatedString,
        style = FinancialConnectionsTheme.typography.body.copy(
            textAlign = TextAlign.Center,
            color = FinancialConnectionsTheme.colors.textSecondary
        ),
        onClick = { offset ->
            list.filterIsInstance<SpanSection.Link>().forEach { linkText ->
                annotatedString.getStringAnnotations(
                    tag = linkText.tag,
                    start = offset,
                    end = offset,
                ).firstOrNull()?.let { linkText.onClick.invoke() }
            }
        }
    )
}

internal sealed interface SpanSection {
    data class Raw(val text: String) : SpanSection
    data class Link(
        val text: String,
        val tag: String,
        val onClick: () -> Unit
    ) : SpanSection
}
