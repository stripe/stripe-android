package com.stripe.android.connect.webview.serialization

import com.stripe.android.connect.NotificationBannerProps
import com.stripe.android.connect.PrivatePreviewConnectSDK
import kotlinx.serialization.Serializable

@Serializable
internal data class NotificationBannerPropsJs(
    val setCollectionOptions: AccountOnboardingPropsJs.CollectionOptionsJs?,
)

@OptIn(PrivatePreviewConnectSDK::class)
internal fun NotificationBannerProps.State.toJs(): NotificationBannerPropsJs {
    return NotificationBannerPropsJs(
        setCollectionOptions = collectionOptions?.toJs(),
    )
}
