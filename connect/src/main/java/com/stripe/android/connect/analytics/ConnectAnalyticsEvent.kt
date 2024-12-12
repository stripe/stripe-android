package com.stripe.android.connect.analytics

internal sealed class ConnectAnalyticsEvent(
    val eventName: String,
    val params: Map<String, String?>? = null,
) {

    data class PaneLaunched(
        val pane: String,
        val referrer: Int?
    ) : ConnectAnalyticsEvent(
        "pane.launched",
        mapOf(
            "referrer_pane" to "String",
            "pane" to "String",
        )
    )

    data object ComponentCreated : ConnectAnalyticsEvent("component.created")

    data class ComponentViewed(
        val pageViewId: String?
    ) : ConnectAnalyticsEvent(
        "component.viewed",
        mapOf("page_view_id" to pageViewId)
    )

    data class WebPageLoaded(
        val timeToLoad: Double
    ) : ConnectAnalyticsEvent(
        "component.web.page_loaded",
        mapOf("time_to_load" to timeToLoad.toString())
    )

    data class WebComponentLoaded(
        val pageViewId: String,
        val timeToLoad: Double,
        val perceivedTimeToLoad: Double
    ) : ConnectAnalyticsEvent(
        "component.web.component_loaded",
        mapOf(
            "page_view_id" to pageViewId,
            "time_to_load" to timeToLoad.toString(),
            "perceived_time_to_load" to perceivedTimeToLoad.toString()
        )
    )

    data class WebErrorPageLoad(
        val status: Int?,
        val error: String?,
        val url: String
    ) : ConnectAnalyticsEvent(
        "component.web.error.page_load",
        mapOf(
            "status" to status?.toString(),
            "error" to error,
            "url" to url
        )
    )

    data class WebWarnUnexpectedLoad(
        val errorType: String,
        val pageViewId: String?
    ) : ConnectAnalyticsEvent(
        "component.web.warn.unexpected_load_error_type",
        mapOf(
            "error_type" to errorType,
            "page_view_id" to pageViewId
        )
    )

    data class WebWarnUnrecognizedSetter(
        val setter: String,
        val pageViewId: String?
    ) : ConnectAnalyticsEvent(
        "component.web.warn.unrecognized_setter_function",
        mapOf(
            "setter" to setter,
            "page_view_id" to pageViewId
        )
    )

    data class WebErrorDeserializeMessage(
        val message: String,
        val error: String,
        val errorDescription: String?,
        val pageViewId: String?
    ) : ConnectAnalyticsEvent(
        "component.web.error.deserialize_message",
        mapOf(
            "message" to message,
            "error" to error,
            "error_description" to errorDescription,
            "page_view_id" to pageViewId
        )
    )

    data class SecureWebOpened(
        val pageViewId: String?,
        val authenticatedSecureViewId: String
    ) : ConnectAnalyticsEvent(
        "component.authenticated_websecureweb.opened",
        mapOf(
            "page_view_id" to pageViewId,
            "authenticatedsecure_view_id" to authenticatedSecureViewId
        )
    )

    data class SecureWebRedirected(
        val pageViewId: String?,
        val authenticatedSecureViewId: String
    ) : ConnectAnalyticsEvent(
        "component.authenticated_websecureweb.redirected",
        mapOf(
            "page_view_id" to pageViewId,
            "authenticatedsecure_view_id" to authenticatedSecureViewId
        )
    )

    data class SecureWebCanceled(
        val pageViewId: String?,
        val secureViewId: String
    ) : ConnectAnalyticsEvent(
        "component.authenticated_websecureweb.canceled",
        mapOf(
            "page_view_id" to pageViewId,
            "secure_view_id" to secureViewId
        )
    )

    data class SecureWebError(
        val error: String,
        val pageViewId: String?,
        val secureViewId: String
    ) : ConnectAnalyticsEvent(
        "component.authenticated_websecureweb.error",
        mapOf(
            "error" to error,
            "page_view_id" to pageViewId,
            "secure_view_id" to secureViewId
        )
    )

    data class WebErrorUnexpectedNavigation(
        val url: String
    ) : ConnectAnalyticsEvent(
        "component.web.error.unexpected_navigation",
        mapOf("url" to url)
    )

    data class ClientError(
        val domain: String,
        val code: Int,
        val file: String,
        val line: Int
    ) : ConnectAnalyticsEvent(
        "client_error",
        mapOf(
            "domain" to domain,
            "code" to code.toString(),
            "file" to file,
            "line" to line.toString()
        )
    )
}
