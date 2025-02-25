package com.stripe.android.connect.webview

import android.app.Application
import android.content.Context
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.annotation.RestrictTo
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.MutableCreationExtras
import com.stripe.android.connect.BuildConfig
import com.stripe.android.connect.ComponentEvent
import com.stripe.android.connect.ComponentListenerDelegate
import com.stripe.android.connect.ComponentProps
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.StripeEmbeddedComponent
import com.stripe.android.connect.StripeEmbeddedComponentListener
import com.stripe.android.connect.toJsonObject
import com.stripe.android.connect.util.AndroidClock
import com.stripe.android.connect.util.findActivity
import com.stripe.android.core.Logger
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface StripeConnectWebViewContainer<Listener, Props>
    where Props : ComponentProps,
          Listener : StripeEmbeddedComponentListener {

    /**
     * Initializes the view. Must be called exactly once if and only if this view was created
     * through XML layout inflation.
     */
    fun initialize(
        embeddedComponentManager: EmbeddedComponentManager,
        listener: Listener?,
        props: Props,
    )
}

@OptIn(PrivateBetaConnectSDK::class)
internal class StripeConnectWebViewContainerImpl<Listener, Props>(
    private val context: Context,
    private val embeddedComponent: StripeEmbeddedComponent,
    embeddedComponentManager: EmbeddedComponentManager?,
    private var listener: Listener?,
    props: Props?,
    private val listenerDelegate: ComponentListenerDelegate<Listener> = ComponentListenerDelegate(),
    private val logger: Logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG),
) : StripeConnectWebViewContainer<Listener, Props>
    where Props : ComponentProps,
          Listener : StripeEmbeddedComponentListener {

    private val loggerTag = javaClass.simpleName

    private var viewModel: StripeConnectWebViewContainerViewModel? = null

    private var containerView: FrameLayout? = null
    private val webView: WebView? get() = viewModel?.webView
    private var cacheKey: String? = null
    private var progressBar: ProgressBar? = null

    /* Notes on initialization
     * -----------------------
     * An embedded component view can be instantiated in two ways:
     *  1. Calling one of the create methods in EmbeddedComponentManager
     *  2. XML layout inflation
     * In both cases, we need to initialize the view with the manager, listener, and props exactly
     * once.
     *
     * For (1), this is trivial since we can require the dependencies in the function signature
     * and initialize immediately. The user doesn't need to worry about initialization. In this
     * case, `embeddedComponentManager`, `props`, and `listener` from the constructor are all that
     * we use.
     *
     * For (2), we require the user to call initialize() after inflation. The values of the
     * constructor params will all be null, and we will only use the values passed to
     * `initialize()`. The one exception is `props`, which will first be set by internal function
     * `setPropsFromXml()` after which the user may merge in more props through `initialize()`
     * (if the user doesn't want to use the XML props, they shouldn't be specifying them).
     */

    private var propsJson: JsonObject? = null

    init {
        if (embeddedComponentManager != null) {
            initializeInternal(
                embeddedComponentManager = embeddedComponentManager,
                listener = listener,
                propsJson = props?.toJsonObject()
            )
        }
    }

    internal fun initializeView(view: FrameLayout, cacheKey: String?) {
        this.containerView = view
        this.cacheKey = cacheKey

        bindViewModel()
    }

    override fun initialize(
        embeddedComponentManager: EmbeddedComponentManager,
        listener: Listener?,
        props: Props,
    ) {
        initializeInternal(
            embeddedComponentManager = embeddedComponentManager,
            listener = listener,
            propsJson = props.toJsonObject()
        )
    }

    private fun initializeInternal(
        embeddedComponentManager: EmbeddedComponentManager,
        listener: Listener?,
        propsJson: JsonObject?,
    ) {
        if (this.viewModel != null) {
            throw IllegalStateException("Already initialized")
        }
        this.listener = listener
        val oldProps = this.propsJson
        this.propsJson =
            when {
                propsJson == null -> oldProps
                oldProps == null -> propsJson
                else -> {
                    buildJsonObject {
                        (oldProps.entries + propsJson.entries).forEach { (k, v) ->
                            put(k, v)
                        }
                    }
                }
            }

        val viewModelStoreOwner = requireNotNull(context.findActivity() as? ViewModelStoreOwner)
        val viewModelKey = buildString {
            append(embeddedComponent.name)
            if (cacheKey != null) {
                append("-")
                append(cacheKey)
            }
        }
        val viewModelProvider = ViewModelProvider.create(
            owner = viewModelStoreOwner,
            factory = StripeConnectWebViewContainerViewModel.Factory,
            extras = MutableCreationExtras().apply {
                set(APPLICATION_KEY, context.applicationContext as Application)
                set(
                    StripeConnectWebViewContainerViewModel.BASE_DEPENDENCIES_KEY,
                    StripeConnectWebViewContainerViewModel.BaseDependencies(
                        clock = AndroidClock(),
                        embeddedComponentManager = embeddedComponentManager,
                        embeddedComponent = embeddedComponent,
                    )
                )
            }
        )
        this.viewModel = viewModelProvider[viewModelKey, StripeConnectWebViewContainerViewModel::class]
        bindViewModel()
    }

    private fun bindViewModel() {
        val containerView = this.containerView ?: return
        val viewModel = this.viewModel ?: return

        // Sync props to VM.
        viewModel.propsJson = this.propsJson

        // Add VM's WebView.
        val webView = viewModel.webView
            .apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        containerView.addView(webView)

        // Add progress bar on top of WebView.
        val progressBar = ProgressBar(containerView.context)
            .apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER,
                )
            }
        this.progressBar = progressBar
        containerView.addView(progressBar)

        containerView.addOnAttachStateChangeListener(
            object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    viewModel.onViewAttached()
                    val owner = containerView.findViewTreeLifecycleOwner()
                        ?: return

                    // Bind VM's lifecycle to container view's lifecycle.
                    owner.lifecycle.addObserver(viewModel)

                    // Handle VM state and events.
                    owner.lifecycleScope.launch {
                        viewModel.stateFlow.collectLatest(::bindViewModelState)
                        viewModel.eventFlow.collectLatest(::handleEvent)
                    }
                }

                override fun onViewDetachedFromWindow(v: View) {
                    // Clean up.
                    containerView.findViewTreeLifecycleOwner()?.lifecycle?.removeObserver(viewModel)
                    logger.debug("($loggerTag) Removing WebView from container view")
                    containerView.removeView(webView)
                }
            }
        )
    }

    internal fun setPropsFromXml(props: Props) {
        // Only set props if uninitialized.
        if (viewModel == null) {
            this.propsJson = props.toJsonObject()
        }
    }

    private fun bindViewModelState(state: StripeConnectWebViewContainerState) {
        val containerView = this.containerView ?: return
        val webView = this.webView ?: return
        val progressBar = this.progressBar ?: return

        logger.debug("($loggerTag) Binding view state: $state")
        containerView.setBackgroundColor(state.backgroundColor)
        webView.setBackgroundColor(state.backgroundColor)
        progressBar.isVisible = state.isNativeLoadingIndicatorVisible
        webView.isVisible = !state.isNativeLoadingIndicatorVisible
        if (state.isNativeLoadingIndicatorVisible) {
            progressBar.indeterminateTintList = ColorStateList.valueOf(state.nativeLoadingIndicatorColor)
        }
    }

    private fun handleEvent(event: ComponentEvent) {
        with(listenerDelegate) {
            listener?.delegate(event)
        }
    }
}
