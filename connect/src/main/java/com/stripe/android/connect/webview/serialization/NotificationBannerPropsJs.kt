package com.stripe.android.connect.webview.serialization

import com.stripe.android.connect.NotificationBannerProps
import com.stripe.android.connect.NotificationBannerTaskProps
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

@Serializable
internal data class NotificationBannerPropsJs(
    val setCollectionOptions: AccountOnboardingPropsJs.CollectionOptionsJs?,
    val setMobileNotificationBannerForm: JsonObject?,
)

internal fun NotificationBannerProps.toJs(): NotificationBannerPropsJs {
    return NotificationBannerPropsJs(
        setCollectionOptions = collectionOptions?.toJs(),
        setMobileNotificationBannerForm = null,
    )
}

internal fun NotificationBannerTaskProps.toJs(): NotificationBannerPropsJs {
    return NotificationBannerPropsJs(
        setCollectionOptions = collectionOptions?.toJs(),
        setMobileNotificationBannerForm = ConnectJson.parseToJsonElement(task).jsonObject,
    )
}
