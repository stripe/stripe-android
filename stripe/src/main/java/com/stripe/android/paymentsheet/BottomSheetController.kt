package com.stripe.android.paymentsheet

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class BottomSheetController(
    private val bottomSheetBehavior: BottomSheetBehavior<ViewGroup>,
    private val lifecycleScope: CoroutineScope
) {
    private val _shouldFinish = MutableLiveData(false)
    internal val shouldFinish = _shouldFinish.distinctUntilChanged()

    fun setup() {
        bottomSheetBehavior.isFitToContents = false
        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.isDraggable = false

        lifecycleScope.launch {
            delay(ANIMATE_IN_DELAY)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            bottomSheetBehavior.addBottomSheetCallback(
                object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    }

                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                            // finish the activity only after the bottom sheet's state has
                            // transitioned to `BottomSheetBehavior.STATE_HIDDEN`
                            _shouldFinish.value = true
                        }
                    }
                }
            )
        }
    }

    fun hide() {
        // When the bottom sheet finishes animating to its new state,
        // the callback will finish the activity
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    internal companion object {
        const val ANIMATE_IN_DELAY = 300L
    }
}
