package com.stripe.android.connect

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.fragment.app.FragmentActivity
import com.stripe.android.connect.util.findActivity
import com.stripe.android.connect.webview.StripeConnectWebViewContainer
import com.stripe.android.connect.webview.StripeConnectWebViewLayout
import com.stripe.android.connect.webview.serialization.SetOnLoadError
import com.stripe.android.connect.webview.serialization.SetOnNotificationsChange
import com.stripe.android.connect.webview.serialization.SetterFunctionCalledMessage
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class NotificationBannerView internal constructor(
    context: Context,
    private val notificationBannerManager: EmbeddedComponentManager,
    listener: NotificationBannerListener?,
    private val collectionOptions: AccountOnboardingProps.CollectionOptions?,
    cacheKey: String?,
) : StripeComponentView<NotificationBannerListener, NotificationBannerProps>(
    context = context,
    attrs = null,
    defStyleAttr = 0,
    embeddedComponent = StripeEmbeddedComponent.NOTIFICATION_BANNER,
    webViewLayout = StripeConnectWebViewLayout.SIZES_TO_CONTENT,
    embeddedComponentManager = notificationBannerManager,
    listener = listener,
    listenerDelegate = NotificationBannerListenerDelegate,
    props = NotificationBannerProps(collectionOptions),
),
    StripeConnectWebViewContainer<NotificationBannerListener, NotificationBannerProps> {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class InitialLoadState {
        /** The banner is fetching and rendering its initial content. */
        LOADING,

        /** Initial rendering finished, including when there is nothing to display. */
        LOADED,

        /** Initial loading failed. */
        FAILED,
    }

    /** The title shown by full-screen tasks opened from this banner. */
    var taskTitle: String? = null

    /** The banner's current initial-load state. */
    var initialLoadState: InitialLoadState = InitialLoadState.LOADING
        private set

    private var didReceiveInitialNotifications = false
    private var pendingContentHeight = 0
    private var lastPublishedContentHeight: Int? = null
    private var notificationBannerTaskController: NotificationBannerTaskController? = null

    private val finishLoading = Runnable {
        if (initialLoadState == InitialLoadState.LOADING && didReceiveInitialNotifications) {
            publishSettledContentHeight()
            revealWebContent()
            transitionInitialLoadState(InitialLoadState.LOADED)
        }
    }

    init {
        initializeView(cacheKey)
    }

    override fun onComponentEvent(event: ComponentEvent) {
        when (event) {
            is ComponentEvent.ContentHeightChanged -> contentHeightDidChange(event.height)
            is ComponentEvent.LoadError -> transitionInitialLoadState(InitialLoadState.FAILED)
            is ComponentEvent.Message -> handleMessage(event.message)
            is ComponentEvent.OpenNotificationBannerTask -> presentTask(event.task)
        }
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(finishLoading)
        super.onDetachedFromWindow()
    }

    private fun handleMessage(message: SetterFunctionCalledMessage) {
        when (message.value) {
            is SetOnLoadError -> transitionInitialLoadState(InitialLoadState.FAILED)
            is SetOnNotificationsChange -> {
                if (initialLoadState == InitialLoadState.LOADING) {
                    didReceiveInitialNotifications = true
                    removeCallbacks(finishLoading)
                    requestContentHeightUpdate()
                }
            }
            else -> Unit
        }
    }

    private fun contentHeightDidChange(height: Int) {
        pendingContentHeight = height
        when (initialLoadState) {
            InitialLoadState.LOADING -> {
                if (didReceiveInitialNotifications) {
                    removeCallbacks(finishLoading)
                    postDelayed(finishLoading, SETTLE_DELAY_MILLIS)
                }
            }
            InitialLoadState.LOADED -> publishSettledContentHeight()
            InitialLoadState.FAILED -> Unit
        }
    }

    private fun publishSettledContentHeight() {
        publishContentHeight(pendingContentHeight)
        if (lastPublishedContentHeight != pendingContentHeight) {
            lastPublishedContentHeight = pendingContentHeight
            listener?.onContentHeightChanged(pendingContentHeight)
        }
    }

    private fun transitionInitialLoadState(state: InitialLoadState) {
        if (initialLoadState != InitialLoadState.LOADING || state == InitialLoadState.LOADING) {
            return
        }
        removeCallbacks(finishLoading)
        initialLoadState = state
        listener?.onInitialLoadStateChanged(state)
    }

    private fun presentTask(task: JsonObject) {
        if (notificationBannerTaskController != null) {
            return
        }
        val activity = context.findActivity() as? FragmentActivity ?: return
        val controller = NotificationBannerTaskController(
            activity = activity,
            embeddedComponentManager = notificationBannerManager,
            title = taskTitle,
            props = NotificationBannerTaskProps(
                collectionOptions = collectionOptions,
                task = task.toString(),
            ),
        )
        controller.listener = object : StripeEmbeddedComponentListener {
            override fun onLoadError(error: Throwable) {
                listener?.onLoadError(error)
            }
        }
        controller.onDismissListener = StripeComponentController.OnDismissListener {
            notificationBannerTaskController = null
            callSetterWithSerializableValue(
                setter = "setMobileNotificationRefreshToken",
                value = JsonPrimitive(UUID.randomUUID().toString()),
            )
        }
        notificationBannerTaskController = controller
        controller.show()
    }

    private companion object {
        private const val SETTLE_DELAY_MILLIS = 200L
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface NotificationBannerListener : StripeEmbeddedComponentListener {
    /** Called when the total and action-required notification counts change. */
    fun onNotificationsChanged(total: Int, actionRequired: Int) {}

    /** Called after the banner publishes a new content height, in pixels. */
    fun onContentHeightChanged(height: Int) {}

    /** Called when the banner finishes its one-time initial-load lifecycle. */
    fun onInitialLoadStateChanged(state: NotificationBannerView.InitialLoadState) {}
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class NotificationBannerProps internal constructor(
    internal val collectionOptions: AccountOnboardingProps.CollectionOptions?,
)

internal object NotificationBannerListenerDelegate : ComponentListenerDelegate<NotificationBannerListener>() {
    override fun delegate(listener: NotificationBannerListener, message: SetterFunctionCalledMessage) {
        val value = message.value
        if (value is SetOnNotificationsChange) {
            listener.onNotificationsChanged(
                total = value.total,
                actionRequired = value.actionRequired,
            )
        }
    }
}
