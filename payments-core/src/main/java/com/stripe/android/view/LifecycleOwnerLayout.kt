package com.stripe.android.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

open class LifecycleOwnerLayout(
    context: Context,
    attributeSet: AttributeSet?,
    defStyleAttr: Int
) : LinearLayout(context, attributeSet, defStyleAttr), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        (parent as? View)?.findViewTreeLifecycleOwner() ?: run {
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            enableSavedStateHandles()
            attachToParent((parent as? View))
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        (parent as? View)?.findViewTreeLifecycleOwner() ?: run {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            store.clear()
        }
    }

    private fun attachToParent(parent: View?) {
        if (parent == null) {
            return
        }

        parent.setViewTreeLifecycleOwner(this)
        parent.setViewTreeViewModelStoreOwner(this)
        parent.setViewTreeSavedStateRegistryOwner(this)
    }

    // LifecycleOwner methods
    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

    // ViewModelStore methods
    private val store = ViewModelStore()

    // SavedStateRegistry methods
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore
        get() = store
}
