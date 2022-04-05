package com.stripe.android.camera.scanui.util

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet

/**
 * Add constraints to a view.
 */
inline fun <T : View> T.addConstraints(
    parent: ConstraintLayout,
    block: ConstraintSet.(view: T) -> Unit
) {
    ConstraintSet().apply {
        clone(parent)
        block(this, this@addConstraints)
        applyTo(parent)
    }
}

/**
 * Constrain a view to the top, bottom, start, and end of its parent.
 */
fun <T : View> T.constrainToParent(parent: ConstraintLayout) {
    addConstraints(parent) {
        connect(it.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        connect(it.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        connect(it.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        connect(it.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
    }
}
