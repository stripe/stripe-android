package com.stripe.android.connect

import android.content.Context
import android.os.Parcelable
import androidx.fragment.app.FragmentActivity
import com.stripe.android.connect.webview.StripeConnectWebViewContainer
import com.stripe.android.connect.webview.StripeConnectWebViewLayout
import kotlinx.parcelize.Parcelize

internal class NotificationBannerTaskController(
    activity: FragmentActivity,
    embeddedComponentManager: EmbeddedComponentManager,
    title: String?,
    props: NotificationBannerTaskProps,
) : StripeComponentController<StripeEmbeddedComponentListener, NotificationBannerTaskProps>(
    dfClass = NotificationBannerTaskDialogFragment::class.java,
    activity = activity,
    embeddedComponentManager = embeddedComponentManager,
    title = title,
    props = props,
)

internal class NotificationBannerTaskDialogFragment :
    StripeComponentDialogFragment<
        NotificationBannerTaskView,
        StripeEmbeddedComponentListener,
        NotificationBannerTaskProps,
        >() {

    override fun createComponentView(
        embeddedComponentManager: EmbeddedComponentManager
    ): NotificationBannerTaskView {
        return NotificationBannerTaskView(
            context = requireContext(),
            embeddedComponentManager = embeddedComponentManager,
            listener = listener,
            props = checkNotNull(props),
            cacheKey = javaClass.simpleName,
        )
    }
}

internal class NotificationBannerTaskView(
    context: Context,
    embeddedComponentManager: EmbeddedComponentManager,
    listener: StripeEmbeddedComponentListener?,
    props: NotificationBannerTaskProps,
    cacheKey: String,
) : StripeComponentView<StripeEmbeddedComponentListener, NotificationBannerTaskProps>(
    context = context,
    attrs = null,
    defStyleAttr = 0,
    embeddedComponent = StripeEmbeddedComponent.NOTIFICATION_BANNER,
    webViewLayout = StripeConnectWebViewLayout.FILLS_AVAILABLE_SPACE,
    embeddedComponentManager = embeddedComponentManager,
    listener = listener,
    props = props,
),
    StripeConnectWebViewContainer<StripeEmbeddedComponentListener, NotificationBannerTaskProps> {
    init {
        initializeView(cacheKey)
    }
}

@Parcelize
internal data class NotificationBannerTaskProps(
    val collectionOptions: AccountOnboardingProps.CollectionOptions?,
    val task: String,
) : Parcelable
