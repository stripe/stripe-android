package com.stripe.android.paymentsheet

import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

internal class BottomSheetController(
    private val bottomSheetBehavior: BottomSheetBehavior<ViewGroup>
) {
    private val _shouldFinish = MutableSharedFlow<Boolean>(replay = 1)
    internal val shouldFinish: Flow<Boolean> = _shouldFinish

    fun setup() {
        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.isDraggable = false
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetBehavior.saveFlags = BottomSheetBehavior.SAVE_ALL
        bottomSheetBehavior.isFitToContents = false

        bottomSheetBehavior.addBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                }

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_HIDDEN -> {
                            // finish the activity only after the bottom sheet's state has
                            // transitioned to `BottomSheetBehavior.STATE_HIDDEN`
                            _shouldFinish.tryEmit(true)
                        }
                        else -> {
                        }
                    }
                }
            }
        )
    }

    fun hide() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
            // If the state is already hidden, setting the state to hidden is a no-op,
            // so we need to finish it now.
            _shouldFinish.tryEmit(true)
        } else {
            // When the bottom sheet finishes animating to its new state,
            // the callback will finish the activity.
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }
}
