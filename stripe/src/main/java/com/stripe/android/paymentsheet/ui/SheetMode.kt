package com.stripe.android.paymentsheet.ui

import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior

internal enum class SheetMode(
    val height: Int,
    @BottomSheetBehavior.State val behaviourState: Int
) {
    Full(
        ViewGroup.LayoutParams.MATCH_PARENT,
        BottomSheetBehavior.STATE_EXPANDED
    ),

    FullCollapsed(
        ViewGroup.LayoutParams.MATCH_PARENT,
        BottomSheetBehavior.STATE_COLLAPSED
    ),

    Wrapped(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        BottomSheetBehavior.STATE_COLLAPSED
    )
}
