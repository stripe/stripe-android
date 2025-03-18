package com.stripe.android.connect

import android.app.Application
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.annotation.RestrictTo
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.MutableCreationExtras
import com.stripe.android.connect.di.StripeConnectComponent
import com.stripe.android.connect.util.AndroidClock
import com.stripe.android.connect.webview.MobileInput
import com.stripe.android.connect.webview.StripeConnectWebView
import com.stripe.android.connect.webview.StripeConnectWebViewContainer
import com.stripe.android.connect.webview.StripeConnectWebViewContainerState
import com.stripe.android.connect.webview.StripeConnectWebViewContainerViewModel
import com.stripe.android.core.Logger
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
) : FrameLayout(context, attrs, defStyleAttr),
    StripeConnectWebViewContainer<Listener, Props>
    where Listener : StripeEmbeddedComponentListener,
          Props : ComponentProps {

    private val loggerTag = javaClass.simpleName

    private var viewModel: StripeConnectWebViewContainerViewModel? = null

    private var progressBar: ProgressBar? = null

    // See StripeConnectWebViewContainerViewModel for why we're getting a WebView from a ViewModel.
    internal val webView: StripeConnectWebView? get() = viewModel?.webView
    private var webViewCacheKey: String? = null

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
                propsJson = props?.toJsonObject()
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
            propsJson = props.toJsonObject()
        )
    }

    internal fun mobileInputReceived(input: MobileInput, resultCallback: ValueCallback<String>) {
        webView?.mobileInputReceived(input, resultCallback)
    }

    private fun initializeInternal(
        embeddedComponentManager: EmbeddedComponentManager,
        listener: Listener?,
        propsJson: JsonObject?,
    ) {
        this.embeddedComponentManager = embeddedComponentManager
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
            val containerView = v as ViewGroup

            val viewModel = checkNotNull(getViewModelFromViewModelStoreOwner(containerView))
                .also { this@StripeComponentView.viewModel = it }
            populateContainerView(
                containerView = containerView,
                viewModel = viewModel
            )

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

    private fun populateContainerView(containerView: ViewGroup, viewModel: StripeConnectWebViewContainerViewModel) {
        // Sync props to VM.
        viewModel.propsJson = this.propsJson

        // Start from a clean slate.
        containerView.removeAllViews()

        // Add VM's WebView.
        val webView = viewModel.webView
            .apply {
                layoutParams = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        containerView.addView(webView)

        // Add progress bar on top of WebView.
        val progressBar = ProgressBar(containerView.context)
            .apply {
                layoutParams = LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER,
                )
            }
        this.progressBar = progressBar
        containerView.addView(progressBar)
    }

    internal fun setPropsFromXml(props: Props) {
        // Only set props if uninitialized.
        if (this.embeddedComponentManager == null) {
            this.propsJson = props.toJsonObject()
        }
    }

    private fun bindViewModelState(state: StripeConnectWebViewContainerState) {
        val webView = this.webView ?: return
        val progressBar = this.progressBar ?: return

        logger.debug("($loggerTag) Binding view state: $state")
        setBackgroundColor(state.backgroundColor)
        webView.setBackgroundColor(state.backgroundColor)
        progressBar.isVisible = state.isNativeLoadingIndicatorVisible
        webView.isVisible = !state.isNativeLoadingIndicatorVisible
        if (state.isNativeLoadingIndicatorVisible) {
            progressBar.indeterminateTintList = ColorStateList.valueOf(state.nativeLoadingIndicatorColor)
        }
    }

    private fun handleEvent(event: ComponentEvent) {
        listener?.let { listenerDelegate.delegate(it, event) }
    }
}
