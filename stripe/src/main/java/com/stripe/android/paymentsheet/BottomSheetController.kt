package com.stripe.android.paymentsheet

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.stripe.android.paymentsheet.ui.SheetMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class BottomSheetController(
    private val bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>,
    private val sheetModeLiveData: LiveData<SheetMode>,
    private val lifecycleScope: CoroutineScope
) {
    private val _shouldFinish = MutableLiveData(false)
    internal val shouldFinish = _shouldFinish.distinctUntilChanged()

    fun setup() {
        bottomSheetBehavior.peekHeight = BottomSheetBehavior.PEEK_HEIGHT_AUTO
        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.isDraggable = false
        // Start hidden and then animate in after delay
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        lifecycleScope.launch {
            delay(ANIMATE_IN_DELAY)
            bottomSheetBehavior.state = sheetModeLiveData.value?.behaviourState ?: BottomSheetBehavior.STATE_EXPANDED
            bottomSheetBehavior.addBottomSheetCallback(
                object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    }

                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                            _shouldFinish.value = true
                        }
                    }
                }
            )
        }
    }

    fun updateState(sheetMode: SheetMode) {
        if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetBehavior.state = sheetMode.behaviourState
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
