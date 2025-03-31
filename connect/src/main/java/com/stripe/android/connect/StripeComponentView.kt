package com.stripe.android.connect

import android.app.Application
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.widget.FrameLayout
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.core.view.isVisible
import androidx.core.view.updateMargins
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.MutableCreationExtras
import com.stripe.android.connect.di.StripeConnectComponent
import com.stripe.android.connect.util.AndroidClock
import com.stripe.android.connect.util.Clock
import com.stripe.android.connect.webview.MobileInput
import com.stripe.android.connect.webview.StripeConnectWebView
import com.stripe.android.connect.webview.StripeConnectWebViewContainer
import com.stripe.android.connect.webview.StripeConnectWebViewContainerState
import com.stripe.android.connect.webview.StripeConnectWebViewContainerViewModel
import com.stripe.android.connect.webview.StripeWebViewSpinner
import com.stripe.android.core.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class StripeComponentView<Listener, Props> internal constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    private val embeddedComponent: StripeEmbeddedComponent,
    private var embeddedComponentManager: EmbeddedComponentManager?,
    listener: Listener?,
    props: Props?,
    private val listenerDelegate: ComponentListenerDelegate<Listener> = ComponentListenerDelegate(),
    private val logger: Logger = StripeConnectComponent.instance.logger,
    private val clock: Clock = AndroidClock(),
) : FrameLayout(context, attrs, defStyleAttr),
    StripeConnectWebViewContainer<Listener, Props>
    where Listener : StripeEmbeddedComponentListener {

    private val loggerTag = javaClass.simpleName

    private var viewModel: StripeConnectWebViewContainerViewModel? = null

    private var progressBar: StripeWebViewSpinner? = null

    // See StripeConnectWebViewContainerViewModel for why we're getting a WebView from a ViewModel.
    private val webView: StripeConnectWebView? get() = viewModel?.webView
    private var webViewCacheKey: String? = null

    private val _receivedCloseWebView = MutableStateFlow(false)
    internal val receivedCloseWebView: StateFlow<Boolean> = _receivedCloseWebView.asStateFlow()

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

    @Suppress("CanBePrimaryConstructorProperty") // Used in `init`.
    override var listener: Listener? = listener

    init {
        val embeddedComponentManager = this.embeddedComponentManager
        if (embeddedComponentManager != null) {
            initializeInternal(
                embeddedComponentManager = embeddedComponentManager,
                listener = listener,
                propsJson = props?.toComponentPropsJsonObject()
            )
        }
    }

    internal fun initializeView(cacheKey: String?) {
        this.webViewCacheKey = cacheKey

        addOnAttachStateChangeListener(onAttachStateChangeListener)
    }

    override fun initialize(
        embeddedComponentManager: EmbeddedComponentManager,
        listener: Listener?,
        props: Props,
    ) {
        initializeInternal(
            embeddedComponentManager = embeddedComponentManager,
            listener = listener,
            propsJson = props?.toComponentPropsJsonObject()
        )
    }

    private fun initializeInternal(
        embeddedComponentManager: EmbeddedComponentManager,
        listener: Listener?,
        propsJson: JsonObject?,
    ) {
        this.embeddedComponentManager = embeddedComponentManager
            .apply { coordinator.checkContextDuringCreate(context) }
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
    }

    private val onAttachStateChangeListener = object : OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            val containerView = this@StripeComponentView

            val viewModel = checkNotNull(getViewModelFromViewModelStoreOwner(containerView))
                .also { this@StripeComponentView.viewModel = it }
            populateContainerView(viewModel = viewModel)

            val lifecycleOwner = checkNotNull(containerView.findViewTreeLifecycleOwner())

            // Bind VM's lifecycle to container view's lifecycle.
            lifecycleOwner.lifecycle.addObserver(viewModel)

            // Handle VM state and events.
            lifecycleOwner.lifecycleScope.launch {
                launch {
                    viewModel.stateFlow.collectLatest(::bindViewModelState)
                }
                launch {
                    viewModel.eventFlow.collectLatest(::handleEvent)
                }
            }

            viewModel.onViewAttached()
        }

        override fun onViewDetachedFromWindow(v: View) {
            val containerView = v as ViewGroup

            // Clean up.
            viewModel?.let { containerView.findViewTreeLifecycleOwner()?.lifecycle?.removeObserver(it) }
            logger.debug("($loggerTag) Removing WebView from container view")
            containerView.removeView(webView)
        }
    }

    private fun getViewModelFromViewModelStoreOwner(view: View): StripeConnectWebViewContainerViewModel? {
        val embeddedComponentManager = this.embeddedComponentManager ?: return null
        val viewModelStoreOwner = view.findViewTreeViewModelStoreOwner() ?: return null

        val viewModelKey = buildString {
            append(embeddedComponent.name)
            append("-")
            append(embeddedComponentManager)
            if (webViewCacheKey != null) {
                append("-")
                append(webViewCacheKey)
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
        return viewModelProvider[viewModelKey, StripeConnectWebViewContainerViewModel::class]
    }

    private fun populateContainerView(viewModel: StripeConnectWebViewContainerViewModel) {
        // Sync props to VM.
        viewModel.propsJson = this.propsJson

        // Start from a clean slate.
        removeAllViews()

        // Add VM's WebView.
        val webView = viewModel.webView
            .apply {
                layoutParams = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        addView(webView)

        // Add progress bar on top of WebView.
        addProgressBar()
    }

    internal fun setPropsFromXml(props: Props) {
        // Only set props if uninitialized.
        if (this.embeddedComponentManager == null) {
            this.propsJson = props?.toComponentPropsJsonObject()
        }
    }

    @VisibleForTesting
    internal fun addProgressBar() {
        val progressBar = StripeWebViewSpinner(context, clock = clock)
            .apply {
                // Try to match size and position with web spinner.
                val size = context.resources.getDimensionPixelSize(R.dimen.stripe_web_view_spinner_size)
                val topMargin = context.resources.getDimensionPixelSize(R.dimen.stripe_web_view_spinner_top_margin)
                layoutParams = LayoutParams(size, size, Gravity.TOP or Gravity.CENTER_HORIZONTAL)
                    .apply { updateMargins(top = topMargin) }
                // Hide until explicitly shown. This is important for configuration changes where
                // this view is recreated but the WebView is reused and has already loaded.
                isVisible = false
            }
        this.progressBar = progressBar
        addView(progressBar)
    }

    @VisibleForTesting
    internal fun bindViewModelState(state: StripeConnectWebViewContainerState) {
        logger.debug("($loggerTag) Binding view state: $state")

        _receivedCloseWebView.value = state.receivedCloseWebView
        setBackgroundColor(state.backgroundColor)

        webView?.let { bindWebView(it, state) }
        progressBar?.let { bindProgressBar(it, state) }
    }

    private fun bindWebView(
        webView: StripeConnectWebView,
        state: StripeConnectWebViewContainerState
    ) {
        webView.setBackgroundColor(state.backgroundColor)
        webView.isVisible = !state.isNativeLoadingIndicatorVisible
    }

    private fun bindProgressBar(progressBar: StripeWebViewSpinner, state: StripeConnectWebViewContainerState) {
        progressBar.setColor(state.nativeLoadingIndicatorColor)
        if (state.isNativeLoadingIndicatorVisible) {
            progressBar.clearAnimation()
            progressBar.alpha = 1f
            progressBar.isVisible = true
        } else if (progressBar.isVisible) {
            @Suppress("MagicNumber")
            progressBar.animate()
                // Delay a bit to allow the web spinner to appear.
                .setStartDelay(200L)
                // Fade out to reduce visual impact from minor UI discrepancies.
                .setDuration(200L)
                .alpha(0f)
                .withStartAction { progressBar.alpha = 1f }
                .withEndAction { progressBar.isVisible = false }
                .start()
        }
    }

    internal fun mobileInputReceived(input: MobileInput, resultCallback: ValueCallback<Result<String>>) {
        webView?.mobileInputReceived(input, resultCallback)
    }

    private fun handleEvent(event: ComponentEvent) {
        listener?.let { listenerDelegate.delegate(it, event) }
    }
}
