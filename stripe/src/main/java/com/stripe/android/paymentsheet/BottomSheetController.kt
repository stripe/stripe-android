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
        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.isDraggable = false
        // Start hidden and then animate in after delay
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        lifecycleScope.launch {
            delay(ANIMATE_IN_DELAY)

            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            bottomSheetBehavior.addBottomSheetCallback(
                object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    }

                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        when (newState) {
                            BottomSheetBehavior.STATE_EXPANDED -> {
                                // Because we change the content of the sheet and its height at
                                // runtime, make sure it's properly laid out once it settles
                                bottomSheet.requestLayout()
                            }
                            BottomSheetBehavior.STATE_HIDDEN -> {
                                // finish the activity only after the bottom sheet's state has
                                // transitioned to `BottomSheetBehavior.STATE_HIDDEN`
                                _shouldFinish.value = true
                            }
                            else -> {
                            }
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
