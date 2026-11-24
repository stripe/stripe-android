package com.stripe.android.connect

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Color
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
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.MutableCreationExtras
import com.stripe.android.connect.di.StripeConnectComponent
import com.stripe.android.connect.util.AndroidClock
import com.stripe.android.connect.util.Clock
import com.stripe.android.connect.util.findActivity
import com.stripe.android.connect.util.getContrastingColor
import com.stripe.android.connect.webview.InlineSheet
import com.stripe.android.connect.webview.MobileInput
import com.stripe.android.connect.webview.StripeConnectSheetDialog
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlin.math.ceil

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("TooManyFunctions")
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

    private val isInline = embeddedComponent.isInline

    private var contentHeight = 0

    private val sheetDialogs = mutableMapOf<String, StripeConnectSheetDialog>()

    // Dialogs must be dismissed before their activity's window goes away.
    private val releaseSheetsOnDestroy = object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            releaseSheetDialogs()
        }
    }

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
                if (isInline) {
                    launch {
                        viewModel.componentHeightFlow.filterNotNull().collect(::onContentHeightChanged)
                    }
                    launch {
                        viewModel.sheetsFlow.collect(::bindSheets)
                    }
                }
            }
            if (isInline) {
                lifecycleOwner.lifecycle.addObserver(releaseSheetsOnDestroy)
            }

            viewModel.onViewAttached()
        }

        override fun onViewDetachedFromWindow(v: View) {
            val containerView = v as ViewGroup

            // Clean up.
            releaseSheetDialogs()
            containerView.findViewTreeLifecycleOwner()?.lifecycle?.removeObserver(releaseSheetsOnDestroy)
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

        // Inline components show Connect.js's loading UI at the height it reports.
        if (!isInline) {
            // Add progress bar on top of WebView.
            addProgressBar()
        }
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
        setBackgroundColor(viewBackgroundColor(state))

        webView?.let { bindWebView(it, state) }
        progressBar?.let { bindProgressBar(it, state) }
        sheetDialogs.values.forEach { it.updateColors(state.backgroundColor, state.sheetTextColor) }
    }

    // An inline component shows the app's screen around what Connect.js draws, like its rounded corners.
    private fun viewBackgroundColor(state: StripeConnectWebViewContainerState): Int =
        if (isInline) Color.TRANSPARENT else state.backgroundColor

    private fun bindWebView(
        webView: StripeConnectWebView,
        state: StripeConnectWebViewContainerState
    ) {
        webView.setBackgroundColor(viewBackgroundColor(state))
        webView.isVisible = isInline || !state.isNativeLoadingIndicatorVisible
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (!isInline) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        val height = resolveSize(contentHeight, heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))

        // Chromium doesn't render content outside the web view's viewport, so it's never shorter than the content.
        val webView = webView?.takeIf { it.parent == this } ?: return
        val webViewHeight = maxOf(height, contentHeight, 1)
        if (webViewHeight != webView.measuredHeight) {
            webView.measure(
                MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(webViewHeight, MeasureSpec.EXACTLY),
            )
        }
    }

    private fun onContentHeightChanged(height: Float) {
        handleEvent(ComponentEvent.ContentHeightChanged(height))
        val heightPx = ceil(height * resources.displayMetrics.density).toInt()
        if (heightPx != contentHeight) {
            contentHeight = heightPx
            requestLayout()
        }
    }

    private fun bindSheets(sheets: List<InlineSheet>) {
        val viewModel = viewModel ?: return
        val activity = findActivity() ?: return

        val sheetIds = sheets.map { it.id }.toSet()
        (sheetDialogs.keys - sheetIds).forEach { sheetDialogs.remove(it)?.release() }
        // In order, so sheets opened later end up on top.
        sheets.forEach { bindSheet(it, viewModel, activity) }
    }

    private fun bindSheet(sheet: InlineSheet, viewModel: StripeConnectWebViewContainerViewModel, activity: Activity) {
        when (sheet.state) {
            InlineSheet.State.STAGED, InlineSheet.State.SHOWING -> {
                val dialog = sheetDialogs[sheet.id]
                    ?: createSheetDialog(sheet.id, viewModel, activity)
                    ?: return
                val state = viewModel.stateFlow.value
                dialog.bind(sheet.height, sheet.isDismissible, state.backgroundColor, state.sheetTextColor)
                if (sheet.state == InlineSheet.State.SHOWING) {
                    dialog.reveal()
                }
            }
            InlineSheet.State.CLOSING -> {
                val dialog = sheetDialogs[sheet.id]
                if (dialog == null) {
                    viewModel.onSheetReleased(sheet.id)
                } else {
                    dialog.close {
                        sheetDialogs.remove(sheet.id)
                        viewModel.onSheetReleased(sheet.id)
                    }
                }
            }
        }
    }

    private fun createSheetDialog(
        sheetId: String,
        viewModel: StripeConnectWebViewContainerViewModel,
        activity: Activity,
    ): StripeConnectSheetDialog? {
        if (activity.isFinishing || activity.isDestroyed) return null
        val sheetWebView = viewModel.sheetWebView(sheetId) ?: return null
        return StripeConnectSheetDialog(
            activity = activity,
            webView = sheetWebView,
            onDismissed = {
                sheetDialogs.remove(sheetId)
                viewModel.onSheetDismissed(sheetId)
            },
            onCloseRequested = { viewModel.onSheetCloseRequested(sheetId) },
        ).also {
            sheetDialogs[sheetId] = it
            it.show()
        }
    }

    private fun releaseSheetDialogs() {
        if (!isInline) return
        sheetDialogs.values.forEach { it.release() }
        sheetDialogs.clear()
        if (findActivity()?.isChangingConfigurations != true) {
            viewModel?.onHostDismissed()
        }
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
                // Fade out to reduce visual impact from minor UI discrepancies.
                .setDuration(100L)
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

private val StripeConnectWebViewContainerState.sheetTextColor: Int
    get() = appearance?.colors?.text ?: getContrastingColor(backgroundColor)
