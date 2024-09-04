package com.stripe.android.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.compose.material.Text
import androidx.compose.ui.platform.ComposeView
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
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

class TestComposeView(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): LinearLayout(context, attrs, defStyleAttr) {

    private val lifecycleDelegate = TestLifecycleOwnerDelegate()

    init {
        val textView = ComposeView(context)
        textView.setContent {
            Text("Hello React Native")
        }
        this.addView(textView)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lifecycleDelegate.initLifecycle(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        lifecycleDelegate.destroyLifecycle(this)
    }
}

internal class TestLifecycleOwnerDelegate : LifecycleOwner, SavedStateRegistryOwner {

    fun initLifecycle(owner: View) {
        owner.findViewTreeLifecycleOwner() ?: run {
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            attachToParent(owner)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
    }

    fun destroyLifecycle(owner: View) {
        owner.findViewTreeLifecycleOwner() ?: run {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
    }

    private fun attachToParent(owner: View) {
        owner.setViewTreeLifecycleOwner(this)
        owner.setViewTreeSavedStateRegistryOwner(this)
    }

    // LifecycleOwner methods
    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

    // SavedStateRegistry methods
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
}

//internal class TestLifecycleOwnerDelegate : LifecycleOwner {
//
//    fun initLifecycle(owner: View) {
//        owner.findViewTreeLifecycleOwner() ?: run {
//            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
//            attachToParent(owner)
//            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
//        }
//    }
//
//    fun destroyLifecycle(owner: View) {
//        owner.findViewTreeLifecycleOwner() ?: run {
//            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
//            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
//        }
//    }
//
//    private fun attachToParent(owner: View) {
//        owner.setViewTreeLifecycleOwner(this)
//    }
//
//    // LifecycleOwner methods
//    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
//
//    override val lifecycle: Lifecycle
//        get() = lifecycleRegistry
//}
