/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stripe.android.uicore.elements.compat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutIdParentData
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastFirstOrNull
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Composable responsible for measuring and laying out leading and trailing icons, label,
 * placeholder and the input field.
 */
@Composable
internal fun TextFieldLayout(
    modifier: Modifier,
    textField: @Composable () -> Unit,
    label: @Composable (() -> Unit)?,
    placeholder: @Composable ((Modifier) -> Unit)?,
    leading: @Composable (() -> Unit)?,
    trailing: @Composable (() -> Unit)?,
    singleLine: Boolean,
    animationProgress: Float,
    paddingValues: PaddingValues
) {
    val measurePolicy = remember(singleLine, animationProgress, paddingValues) {
        TextFieldMeasurePolicy(singleLine, animationProgress, paddingValues)
    }
    val layoutDirection = LocalLayoutDirection.current
    Layout(
        modifier = modifier,
        content = {
            if (leading != null) {
                Box(
                    modifier = Modifier.layoutId(LeadingId).then(IconDefaultSizeModifier),
                    contentAlignment = Alignment.Center
                ) {
                    leading()
                }
            }
            if (trailing != null) {
                Box(
                    modifier = Modifier.layoutId(TrailingId).then(IconDefaultSizeModifier),
                    contentAlignment = Alignment.Center
                ) {
                    trailing()
                }
            }

            val startTextFieldPadding = paddingValues.calculateStartPadding(layoutDirection)
            val endTextFieldPadding = paddingValues.calculateEndPadding(layoutDirection)
            val padding = Modifier.padding(
                start = if (leading != null) {
                    (startTextFieldPadding - HorizontalIconPadding).coerceAtLeast(
                        0.dp
                    )
                } else {
                    startTextFieldPadding
                },
                end = if (trailing != null) {
                    (endTextFieldPadding - HorizontalIconPadding).coerceAtLeast(0.dp)
                } else {
                    endTextFieldPadding
                }
            )
            if (placeholder != null) {
                placeholder(Modifier.layoutId(PlaceholderId).then(padding))
            }
            if (label != null) {
                Box(Modifier.layoutId(LabelId).then(padding)) { label() }
            }
            Box(
                modifier = Modifier.layoutId(TextFieldId).then(padding),
                propagateMinConstraints = true,
            ) {
                textField()
            }
        },
        measurePolicy = measurePolicy
    )
}

private class TextFieldMeasurePolicy(
    private val singleLine: Boolean,
    private val animationProgress: Float,
    private val paddingValues: PaddingValues
) : MeasurePolicy {
    @Suppress("LongMethod")
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        val topPaddingValue = paddingValues.calculateTopPadding().roundToPx()
        val bottomPaddingValue = paddingValues.calculateBottomPadding().roundToPx()

        // padding between label and input text
        val topPadding = TextFieldTopPadding.roundToPx()
        var occupiedSpaceHorizontally = 0

        // measure leading icon
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val leadingPlaceable =
            measurables.fastFirstOrNull { it.layoutId == LeadingId }?.measure(looseConstraints)
        occupiedSpaceHorizontally += widthOrZero(
            leadingPlaceable
        )

        // measure trailing icon
        val trailingPlaceable = measurables.fastFirstOrNull { it.layoutId == TrailingId }
            ?.measure(looseConstraints.offset(horizontal = -occupiedSpaceHorizontally))
        occupiedSpaceHorizontally += widthOrZero(
            trailingPlaceable
        )

        // measure label
        val labelConstraints = looseConstraints
            .offset(
                vertical = -bottomPaddingValue,
                horizontal = -occupiedSpaceHorizontally
            )
        val labelPlaceable =
            measurables.fastFirstOrNull { it.layoutId == LabelId }?.measure(labelConstraints)
        val lastBaseline = labelPlaceable?.get(LastBaseline)?.let {
            if (it != AlignmentLine.Unspecified) it else labelPlaceable.height
        } ?: 0
        val effectiveLabelBaseline = max(lastBaseline, topPaddingValue)

        // measure input field
        // input field is laid out differently depending on whether the label is present or not
        val verticalConstraintOffset = if (labelPlaceable != null) {
            -bottomPaddingValue - topPadding - effectiveLabelBaseline
        } else {
            -topPaddingValue - bottomPaddingValue
        }
        val textFieldConstraints = constraints
            .copy(minHeight = 0)
            .offset(
                vertical = verticalConstraintOffset,
                horizontal = -occupiedSpaceHorizontally
            )
        val textFieldPlaceable = measurables
            .fastFirst { it.layoutId == TextFieldId }
            .measure(textFieldConstraints)

        // measure placeholder
        val placeholderConstraints = textFieldConstraints.copy(minWidth = 0)
        val placeholderPlaceable = measurables
            .fastFirstOrNull { it.layoutId == PlaceholderId }
            ?.measure(placeholderConstraints)

        val width = calculateWidth(
            widthOrZero(leadingPlaceable),
            widthOrZero(trailingPlaceable),
            textFieldPlaceable.width,
            widthOrZero(labelPlaceable),
            widthOrZero(placeholderPlaceable),
            constraints
        )
        val height = calculateHeight(
            textFieldPlaceable.height,
            labelPlaceable != null,
            effectiveLabelBaseline,
            heightOrZero(leadingPlaceable),
            heightOrZero(trailingPlaceable),
            heightOrZero(placeholderPlaceable),
            constraints,
            density,
            paddingValues
        )

        return layout(width, height) {
            if (labelPlaceable != null) {
                // label's final position is always relative to the baseline
                val labelEndPosition = (topPaddingValue - lastBaseline).coerceAtLeast(0)
                placeWithLabel(
                    width,
                    height,
                    textFieldPlaceable,
                    labelPlaceable,
                    placeholderPlaceable,
                    leadingPlaceable,
                    trailingPlaceable,
                    singleLine,
                    labelEndPosition,
                    effectiveLabelBaseline + topPadding,
                    animationProgress,
                    density
                )
            } else {
                placeWithoutLabel(
                    width,
                    height,
                    textFieldPlaceable,
                    placeholderPlaceable,
                    leadingPlaceable,
                    trailingPlaceable,
                    singleLine,
                    density,
                    paddingValues
                )
            }
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int
    ): Int {
        return intrinsicHeight(measurables, width) { intrinsicMeasurable, w ->
            intrinsicMeasurable.maxIntrinsicHeight(w)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int
    ): Int {
        return intrinsicHeight(measurables, width) { intrinsicMeasurable, w ->
            intrinsicMeasurable.minIntrinsicHeight(w)
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int
    ): Int {
        return intrinsicWidth(measurables, height) { intrinsicMeasurable, h ->
            intrinsicMeasurable.maxIntrinsicWidth(h)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int
    ): Int {
        return intrinsicWidth(measurables, height) { intrinsicMeasurable, h ->
            intrinsicMeasurable.minIntrinsicWidth(h)
        }
    }

    private fun intrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
        intrinsicMeasurer: (IntrinsicMeasurable, Int) -> Int
    ): Int {
        val textFieldWidth =
            intrinsicMeasurer(measurables.fastFirst { it.layoutId == TextFieldId }, height)
        val labelWidth = measurables.fastFirstOrNull { it.layoutId == LabelId }?.let {
            intrinsicMeasurer(it, height)
        } ?: 0
        val trailingWidth = measurables.fastFirstOrNull { it.layoutId == TrailingId }?.let {
            intrinsicMeasurer(it, height)
        } ?: 0
        val leadingWidth = measurables.fastFirstOrNull { it.layoutId == LeadingId }?.let {
            intrinsicMeasurer(it, height)
        } ?: 0
        val placeholderWidth = measurables.fastFirstOrNull { it.layoutId == PlaceholderId }?.let {
            intrinsicMeasurer(it, height)
        } ?: 0
        return calculateWidth(
            leadingWidth = leadingWidth,
            trailingWidth = trailingWidth,
            textFieldWidth = textFieldWidth,
            labelWidth = labelWidth,
            placeholderWidth = placeholderWidth,
            constraints = ZeroConstraints
        )
    }

    private fun IntrinsicMeasureScope.intrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
        intrinsicMeasurer: (IntrinsicMeasurable, Int) -> Int
    ): Int {
        var remainingWidth = width
        val leadingHeight = measurables.fastFirstOrNull { it.layoutId == LeadingId }?.let {
            remainingWidth = remainingWidth.substractConstraintSafely(
                it.maxIntrinsicWidth(Constraints.Infinity)
            )
            intrinsicMeasurer(it, width)
        } ?: 0
        val trailingHeight = measurables.fastFirstOrNull { it.layoutId == TrailingId }?.let {
            remainingWidth = remainingWidth.substractConstraintSafely(
                it.maxIntrinsicWidth(Constraints.Infinity)
            )
            intrinsicMeasurer(it, width)
        } ?: 0

        val labelHeight = measurables.fastFirstOrNull { it.layoutId == LabelId }?.let {
            intrinsicMeasurer(it, remainingWidth)
        } ?: 0

        val textFieldHeight =
            intrinsicMeasurer(measurables.fastFirst { it.layoutId == TextFieldId }, remainingWidth)
        val placeholderHeight = measurables.fastFirstOrNull { it.layoutId == PlaceholderId }?.let {
            intrinsicMeasurer(it, remainingWidth)
        } ?: 0

        return calculateHeight(
            textFieldHeight = textFieldHeight,
            hasLabel = labelHeight > 0,
            labelBaseline = labelHeight,
            leadingHeight = leadingHeight,
            trailingHeight = trailingHeight,
            placeholderHeight = placeholderHeight,
            constraints = ZeroConstraints,
            density = density,
            paddingValues = paddingValues
        )
    }
}

private fun Int.substractConstraintSafely(from: Int): Int {
    if (this == Constraints.Infinity) {
        return this
    }
    return this - from
}

private fun calculateWidth(
    leadingWidth: Int,
    trailingWidth: Int,
    textFieldWidth: Int,
    labelWidth: Int,
    placeholderWidth: Int,
    constraints: Constraints
): Int {
    val middleSection = maxOf(
        textFieldWidth,
        labelWidth,
        placeholderWidth
    )
    val wrappedWidth = leadingWidth + middleSection + trailingWidth
    return max(wrappedWidth, constraints.minWidth)
}

private fun calculateHeight(
    textFieldHeight: Int,
    hasLabel: Boolean,
    labelBaseline: Int,
    leadingHeight: Int,
    trailingHeight: Int,
    placeholderHeight: Int,
    constraints: Constraints,
    density: Float,
    paddingValues: PaddingValues
): Int {
    val paddingToLabel = TextFieldTopPadding.value * density
    val topPaddingValue = paddingValues.calculateTopPadding().value * density
    val bottomPaddingValue = paddingValues.calculateBottomPadding().value * density

    val inputFieldHeight = max(textFieldHeight, placeholderHeight)
    val middleSectionHeight = if (hasLabel) {
        labelBaseline + paddingToLabel + inputFieldHeight + bottomPaddingValue
    } else {
        topPaddingValue + inputFieldHeight + bottomPaddingValue
    }
    return maxOf(
        middleSectionHeight.roundToInt(),
        max(leadingHeight, trailingHeight),
        constraints.minHeight
    )
}

/**
 * Places the provided text field, placeholder and label with respect to the baseline offsets in
 * [TextField] when there is a label. When there is no label, [placeWithoutLabel] is used.
 */
private fun Placeable.PlacementScope.placeWithLabel(
    width: Int,
    height: Int,
    textfieldPlaceable: Placeable,
    labelPlaceable: Placeable?,
    placeholderPlaceable: Placeable?,
    leadingPlaceable: Placeable?,
    trailingPlaceable: Placeable?,
    singleLine: Boolean,
    labelEndPosition: Int,
    textPosition: Int,
    animationProgress: Float,
    density: Float
) {
    leadingPlaceable?.placeRelative(
        0,
        Alignment.CenterVertically.align(leadingPlaceable.height, height)
    )
    trailingPlaceable?.placeRelative(
        width - trailingPlaceable.width,
        Alignment.CenterVertically.align(trailingPlaceable.height, height)
    )
    labelPlaceable?.let {
        // if it's a single line, the label's start position is in the center of the
        // container. When it's a multiline text field, the label's start position is at the
        // top with padding
        val startPosition = if (singleLine) {
            Alignment.CenterVertically.align(it.height, height)
        } else {
            // even though the padding is defined by developer, it only affects text field when animation
            // progress == 1, which is when text field is focused or non-empty input text. The start position of the
            // label is always 16.dp.
            (TextFieldPadding.value * density).roundToInt()
        }
        val distance = startPosition - labelEndPosition
        val positionY = startPosition - (distance * animationProgress).roundToInt()
        it.placeRelative(widthOrZero(leadingPlaceable), positionY)
    }
    textfieldPlaceable.placeRelative(widthOrZero(leadingPlaceable), textPosition)
    placeholderPlaceable?.placeRelative(widthOrZero(leadingPlaceable), textPosition)
}

/**
 * Places the provided text field and placeholder in [TextField] when there is no label. When
 * there is a label, [placeWithLabel] is used
 */
private fun Placeable.PlacementScope.placeWithoutLabel(
    width: Int,
    height: Int,
    textPlaceable: Placeable,
    placeholderPlaceable: Placeable?,
    leadingPlaceable: Placeable?,
    trailingPlaceable: Placeable?,
    singleLine: Boolean,
    density: Float,
    paddingValues: PaddingValues
) {
    val topPadding = (paddingValues.calculateTopPadding().value * density).roundToInt()

    leadingPlaceable?.placeRelative(
        0,
        Alignment.CenterVertically.align(leadingPlaceable.height, height)
    )
    trailingPlaceable?.placeRelative(
        width - trailingPlaceable.width,
        Alignment.CenterVertically.align(trailingPlaceable.height, height)
    )

    // Single line text field without label places its input center vertically. Multiline text
    // field without label places its input at the top with padding
    val textVerticalPosition = if (singleLine) {
        Alignment.CenterVertically.align(textPlaceable.height, height)
    } else {
        topPadding
    }
    textPlaceable.placeRelative(
        widthOrZero(leadingPlaceable),
        textVerticalPosition
    )

    // placeholder is placed similar to the text input above
    placeholderPlaceable?.let {
        val placeholderVerticalPosition = if (singleLine) {
            Alignment.CenterVertically.align(placeholderPlaceable.height, height)
        } else {
            topPadding
        }
        it.placeRelative(
            widthOrZero(leadingPlaceable),
            placeholderVerticalPosition
        )
    }
}

private val IntrinsicMeasurable.layoutId: Any?
    get() = (parentData as? LayoutIdParentData)?.layoutId

private fun widthOrZero(placeable: Placeable?) = placeable?.width ?: 0
private fun heightOrZero(placeable: Placeable?) = placeable?.height ?: 0

private val TextFieldTopPadding = 4.dp
