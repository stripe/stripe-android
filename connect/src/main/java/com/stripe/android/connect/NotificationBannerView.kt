package com.stripe.android.connect

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import androidx.core.content.withStyledAttributes
import com.stripe.android.connect.webview.StripeConnectWebViewContainer
import com.stripe.android.connect.webview.serialization.SetOnNotificationsChange
import com.stripe.android.connect.webview.serialization.SetterFunctionCalledMessage
import kotlinx.parcelize.Parcelize

@PrivatePreviewConnectSDK
internal class NotificationBannerView internal constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    embeddedComponentManager: EmbeddedComponentManager?,
    listener: NotificationBannerListener?,
    props: NotificationBannerProps?,
    cacheKey: String?,
) :
    StripeComponentView<NotificationBannerListener, NotificationBannerProps.State>(
        context = context,
        attrs = attrs,
        defStyleAttr = defStyleAttr,
        embeddedComponent = StripeEmbeddedComponent.NOTIFICATION_BANNER,
        embeddedComponentManager = embeddedComponentManager,
        listener = listener,
        listenerDelegate = NotificationBannerListenerDelegate,
        props = props?.build(),
    ),
    StripeConnectWebViewContainer<NotificationBannerListener, NotificationBannerProps.State> {

    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : this(
        context = context,
        attrs = attrs,
        defStyleAttr = defStyleAttr,
        embeddedComponentManager = null,
        listener = null,
        props = null,
        cacheKey = null,
    )

    init {
        var xmlCacheKey: String? = null
        context.withStyledAttributes(attrs, R.styleable.StripeConnectWebViewContainer, defStyleAttr, 0) {
            xmlCacheKey = getString(R.styleable.StripeConnectWebViewContainer_stripeWebViewCacheKey)
        }
        initializeView(cacheKey ?: xmlCacheKey)
    }
}

@PrivatePreviewConnectSDK
class NotificationBannerProps {
    private var collectionOptions: AccountOnboardingProps.CollectionOptions? = null

    /**
     * Customizes which requirements the banner shows.
     */
    fun collectionOptions(collectionOptions: AccountOnboardingProps.CollectionOptions?) = apply {
        this.collectionOptions = collectionOptions
    }

    @Parcelize
    internal data class State(
        val collectionOptions: AccountOnboardingProps.CollectionOptions?,
    ) : Parcelable

    internal fun build(): State = State(collectionOptions = collectionOptions)
}

@PrivatePreviewConnectSDK
interface NotificationBannerListener : StripeEmbeddedComponentListener {
    /**
     * Called when the total number of notifications, or the number that require action, changes.
     *
     * @param total The total number of notifications in the banner.
     * @param actionRequired The number of notifications that require user action.
     */
    fun onNotificationsChange(total: Int, actionRequired: Int) {}

    /**
     * Called when the banner's content height changes, before the next layout pass. Reports 0 when
     * there's nothing to show.
     *
     * @param height The banner's new content height, in dp.
     */
    fun onContentHeightChange(height: Float) {}
}

@PrivatePreviewConnectSDK
internal object NotificationBannerListenerDelegate : ComponentListenerDelegate<NotificationBannerListener>() {
    override fun delegate(listener: NotificationBannerListener, message: SetterFunctionCalledMessage) {
        when (val value = message.value) {
            is SetOnNotificationsChange -> listener.onNotificationsChange(
                total = value.total,
                actionRequired = value.actionRequired,
            )
            else -> {
                // Ignore.
            }
        }
    }

    override fun onContentHeightChanged(listener: NotificationBannerListener, height: Float) {
        listener.onContentHeightChange(height)
    }
}
