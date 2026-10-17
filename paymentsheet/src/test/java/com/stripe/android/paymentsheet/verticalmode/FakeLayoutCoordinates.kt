package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntSize

class FakeLayoutCoordinates(
    override val isAttached: Boolean = true,
    override val parentCoordinates: LayoutCoordinates? = null,
    override val parentLayoutCoordinates: LayoutCoordinates? = null,
    override val providedAlignmentLines: Set<AlignmentLine> = emptySet(),
    override val size: IntSize,
    private val bounds: Rect,
    private val position: Offset = Offset.Zero
) : LayoutCoordinates {

    override fun get(alignmentLine: AlignmentLine): Int {
        return AlignmentLine.Unspecified
    }

    override fun localBoundingBoxOf(sourceCoordinates: LayoutCoordinates, clipBounds: Boolean): Rect {
        return bounds
    }

    override fun localPositionOf(sourceCoordinates: LayoutCoordinates, relativeToSource: Offset): Offset {
        return position
    }

    override fun localToRoot(relativeToLocal: Offset): Offset {
        return position + relativeToLocal
    }

    override fun localToWindow(relativeToLocal: Offset): Offset {
        return position + relativeToLocal
    }

    override fun windowToLocal(relativeToWindow: Offset): Offset {
        return relativeToWindow - position
    }

    companion object {
        fun create(
            size: IntSize,
            position: Offset = Offset.Zero,
            bounds: Rect, // the bounds of the window the coordinate is in
        ): LayoutCoordinates {
            return FakeLayoutCoordinates(
                size = size,
                bounds = bounds,
                position = position
            )
        }
    }
}
