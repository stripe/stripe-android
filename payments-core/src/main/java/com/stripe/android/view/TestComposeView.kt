package com.stripe.android.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.PausableMonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlin.coroutines.EmptyCoroutineContext

class TestComposeView(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): LinearLayout(context, attrs, defStyleAttr) {

    private val lifecycleDelegate = TestLifecycleOwnerDelegate()

    init {
        val view = MyComposeView(context = context) {
            Column {
                Text(text = "Hello")
                Text(text = "Does this work?")
            }
        }
        this.addView(view)
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

class MyComposeView(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val content: @Composable () -> Unit
) : AbstractComposeView(context, attrs, defStyleAttr) {

    private val lifecycleDelegate = TestLifecycleOwnerDelegate()

    @Composable
    override fun Content() {
        content()
    }

    init {
        val currentThreadContext = AndroidUiDispatcher.CurrentThread
        val pausableClock = currentThreadContext[MonotonicFrameClock]?.let {
            PausableMonotonicFrameClock(it).apply { pause() }
        }
        val contextWithClock = currentThreadContext + (pausableClock ?: EmptyCoroutineContext)
        val recomposer = Recomposer(contextWithClock)
        setParentCompositionContext(recomposer)
    }

    override fun onAttachedToWindow() {
        lifecycleDelegate.initLifecycle(this)
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        lifecycleDelegate.destroyLifecycle(this)
        super.onDetachedFromWindow()
    }

    override val shouldCreateCompositionOnAttachedToWindow: Boolean
        get() = false
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
