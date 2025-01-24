package com.stripe.android.connect.analytics

/**
 * Analytics event for the Connect SDK. One subclass per unique analytics event is expected.
 */
internal sealed class ConnectAnalyticsEvent(
    val eventName: String,
    val params: Map<String, Any?> = mapOf(),
) {
    /**
     * A component was instantiated via `create{ComponentType}`.
     *
     * The delta between this and `component.viewed` could tell us if apps are instantiating
     * components but never rendering them on screen.
     */
    data object ComponentCreated : ConnectAnalyticsEvent("component.created")

    /**
     * The component is viewed on screen (viewDidAppear lifecycle event on iOS)
     *
     * @param pageViewId The pageViewID from the web view. May be null if not yet sent from web
     */
    data class ComponentViewed(
        val pageViewId: String?
    ) : ConnectAnalyticsEvent(
        "component.viewed",
        mapOf("page_view_id" to pageViewId)
    )

    /**
     * The web page finished loading (didFinish navigation on iOS).
     *
     * Note: This should happen before `component_loaded`, so we won't yet have a `page_view_id`.
     *
     * @param timeToLoadMs Elapsed time in milliseconds it took the web page to load (starting when it first began
     *   loading).
     */
    data class WebPageLoaded(
        val timeToLoadMs: Long
    ) : ConnectAnalyticsEvent(
        "component.web.page_loaded",
        mapOf("time_to_load" to msToSecs(timeToLoadMs))
    )

    /**
     * The component is successfully loaded within the web view. Triggered from `pageDidLoad`
     * message handler from the web view.
     *
     * @param pageViewId The pageViewID from the web view
     * @param timeToLoadMs Elapsed time in milliseconds it took the web page to load (starting when it first began
     *   loading).
     * @param perceivedTimeToLoad Elapsed time in seconds in took between when the component was initially viewed
     *   on screen (`component.viewed`) to when the component finished loading. This value will be `0` if the
     *   component finished loading before being viewed on screen.
     */
    data class WebComponentLoaded(
        val pageViewId: String,
        val timeToLoadMs: Long,
        val perceivedTimeToLoad: Long
    ) : ConnectAnalyticsEvent(
        "component.web.component_loaded",
        mapOf(
            "page_view_id" to pageViewId,
            "time_to_load" to msToSecs(timeToLoadMs),
            "perceived_time_to_load" to perceivedTimeToLoad.toString()
        )
    )

    /**
     * The SDK receives a non-200 status code loading the web view, other than "Internet connectivity" errors.
     *
     * Intent is to alert if the URL the mobile client expects is suddenly unreachable.
     * The web view should always return a 200, even when displaying an error state.
     *
     * @param status HTTP status code. This will be null if the error is not an http status type of error
     * @param error Identifier of the error if it's not an http status error (e.g. CORS issue, SSL error, etc)
     * @param url The URL of the page, excluding hash params
     */
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

    /**
     * If the web view sends an `onLoadError` that can't be deserialized by the SDK.
     *
     * @param errorType The error `type` property from web
     * @param pageViewId The pageViewID from the web view. May be null if not yet sent from web
     */
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

    /**
     * If the web view calls `onSetterFunctionCalled` with a `setter` argument the SDK doesn't know how to handle.
     *
     * Note: It's expected to get this warning when web adds support for new setter functions not handled
     * in older SDK versions. But logging it could help us debug issues where we expect the SDK to handle
     * something it isn't.
     *
     * @param setter `setter` property sent from web
     * @param pageViewId The pageViewID from the web view. May be null if not yet sent from web
     */
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

    /**
     * An error occurred deserializing the JSON payload from a web message.
     *
     * @param message The name of the message. If this message has a setter, concatenate it using {message}.{setter}
     * @param error The error identifier
     * @param errorDescription The error's description, if there is one.
     * @param pageViewId The pageViewID from the web view. May be null if not yet sent from web
     */
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

    /**
     * A web view was opened when `openWebView` was called.
     *
     * @param pageViewId The pageViewID from the web view. May be null if not yet sent from web
     * @param authenticatedViewId The id for this secure web view session (sent in `openWebView` message)
     */
    data class AuthenticatedWebOpened(
        val pageViewId: String?,
        val authenticatedViewId: String
    ) : ConnectAnalyticsEvent(
        "component.authenticated_web.opened",
        mapOf(
            "page_view_id" to pageViewId,
            "authenticated_view_id" to authenticatedViewId
        )
    )

    /**
     * The web view successfully redirected back to the app.
     *
     * @param pageViewId The pageViewID from the web view. May be null if not yet sent from web
     * @param authenticatedViewId The id for this secure web view session (sent in `openWebView` message)
     */
    data class AuthenticatedWebRedirected(
        val pageViewId: String?,
        val authenticatedViewId: String
    ) : ConnectAnalyticsEvent(
        "component.authenticated_web.redirected",
        mapOf(
            "page_view_id" to pageViewId,
            "authenticated_view_id" to authenticatedViewId
        )
    )

    /**
     * The user closed the web view before getting redirected back to the app.
     *
     * @param pageViewId The pageViewID from the web view. May be null if not yet sent from web
     * @param authenticatedViewId The id for this secure web view session (sent in `openWebView` message)
     */
    data class AuthenticatedWebCanceled(
        val pageViewId: String?,
        val authenticatedViewId: String
    ) : ConnectAnalyticsEvent(
        "component.authenticated_web.canceled",
        mapOf(
            "page_view_id" to pageViewId,
            "authenticated_view_id" to authenticatedViewId
        )
    )

    /**
     * The web view threw an error and was not successfully redirected back to the app.
     *
     * @param error The error identifier
     * @param pageViewId The pageViewID from the web view. May be null if not yet sent from web
     * @param authenticatedViewId The id for this secure web view session (sent in `openWebView` message)
     */
    data class AuthenticatedWebError(
        val error: String,
        val pageViewId: String?,
        val authenticatedViewId: String
    ) : ConnectAnalyticsEvent(
        "component.authenticated_web.error",
        mapOf(
            "error" to error,
            "page_view_id" to pageViewId,
            "authenticated_view_id" to authenticatedViewId
        )
    )

    /**
     * The web page navigated somewhere other than the component wrapper URL
     * (e.g. https://connect-js.stripe.com/v1.0/android_webview.html)
     *
     * @param url The base URL that was navigated to. The url should have all query params and hash params removed
     * since these could potentially contain sensitive data
     */
    data class WebErrorUnexpectedNavigation(
        val url: String
    ) : ConnectAnalyticsEvent(
        "component.web.error.unexpected_navigation",
        mapOf("url" to url)
    )

    /**
     * Catch-all event for unexpected client-side errors.
     */
    data class ClientError(
        val error: String,
        val errorMessage: String? = null,
    ) : ConnectAnalyticsEvent(
        "client_error",
        mapOf(
            "error" to error,
            "error_message" to errorMessage,
        )
    )
}

private fun msToSecs(ms: Long): String = (ms / 1_000.0).toString()
