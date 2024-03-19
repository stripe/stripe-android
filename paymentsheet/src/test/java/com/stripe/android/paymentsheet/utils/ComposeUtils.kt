package com.stripe.android.paymentsheet.utils

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsMatcher
import com.stripe.android.uicore.image.DrawableId

fun doesNotHaveDrawable(@DrawableRes id: Int): SemanticsMatcher =
    SemanticsMatcher.expectValue(DrawableId, id).not()

fun hasBackground(
    color: Color,
    shape: Shape,
): SemanticsMatcher = SemanticsMatcher(
    description = "Node has a background color of $color"
) { node ->
    node.hasBackground(color, shape)
}

private fun SemanticsNode.hasBackground(color: Color, shape: Shape): Boolean {
    return layoutInfo.getModifierInfo().any { info ->
        info.modifier == Modifier.background(color, shape)
    } || children.any { node ->
        node.hasBackground(color, shape)
    }
}
