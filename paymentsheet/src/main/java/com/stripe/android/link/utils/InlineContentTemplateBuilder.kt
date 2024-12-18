package com.stripe.android.link.utils

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em

/**
 * Wrapper for Text` Jetpack Compose component that provides a clean interface for building
 * inline content components.
 */
internal class InlineContentTemplateBuilder {
    private val inlineContent = mutableMapOf<String, InlineTextContent>()

    fun add(
        id: String,
        width: TextUnit,
        height: TextUnit,
        align: PlaceholderVerticalAlign = PlaceholderVerticalAlign.Center,
        content: @Composable () -> Unit
    ): InlineContentTemplateBuilder {
        inlineContent[id] = InlineTextContent(
            placeholder = Placeholder(
                width = width,
                height = height,
                placeholderVerticalAlign = align
            )
        ) {
            content()
        }

        return this
    }

    fun addSpacer(
        id: String,
        width: TextUnit,
        align: PlaceholderVerticalAlign = PlaceholderVerticalAlign.Center
    ): InlineContentTemplateBuilder {
        add(
            id = id,
            width = width,
            height = 0.em,
            align = align,
            content = {}
        )

        return this
    }

    fun build(): Map<String, InlineTextContent> {
        return inlineContent
    }
}
