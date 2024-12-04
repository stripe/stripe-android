package com.stripe.android.connect

import android.app.Activity
import android.content.Context
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RestrictTo
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.stripe.android.connect.appearance.Appearance
import com.stripe.android.connect.appearance.fonts.CustomFontSource
import com.stripe.android.connect.webview.serialization.ConnectInstanceJs
import com.stripe.android.connect.webview.serialization.toJs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.parcelize.Parcelize
import kotlin.coroutines.resume

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class EmbeddedComponentManager(
    activity: AppCompatActivity,
    private val configuration: Configuration,
    private val fetchClientSecretCallback: FetchClientSecretCallback,
    appearance: Appearance = Appearance(),
    private val customFonts: List<CustomFontSource> = emptyList(),
) {
    private val _appearanceFlow = MutableStateFlow(appearance)
    internal val appearanceFlow: StateFlow<Appearance> get() = _appearanceFlow.asStateFlow()

    private val permissionsFlow: MutableSharedFlow<Boolean> = MutableSharedFlow()
    private val requestPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        MainScope().launch {
            permissionsFlow.emit(isGranted)
        }
    }

    internal suspend fun requestCameraPermission(): Boolean {
        requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        return permissionsFlow.first()
    }

    /**
     * Create a new [AccountOnboardingView] for inclusion in the view hierarchy.
     */
    fun createAccountOnboardingView(
        activity: Activity,
        listener: AccountOnboardingListener? = null
    ): AccountOnboardingView {
        return AccountOnboardingView(
            context = activity,
            embeddedComponentManager = this,
            listener = listener
        )
    }

    /**
     * Create a new [PayoutsView] for inclusion in the view hierarchy.
     */
    fun createPayoutsView(
        activity: Activity,
        listener: PayoutsListener? = null,
    ): PayoutsView {
        return PayoutsView(
            context = activity,
            embeddedComponentManager = this,
            listener = listener,
        )
    }

    internal fun getInitialParams(context: Context): ConnectInstanceJs {
        return ConnectInstanceJs(
            appearance = _appearanceFlow.value.toJs(),
            fonts = customFonts.map { it.toJs(context) },
        )
    }

    /**
     * Returns the Connect Embedded Component URL for the given [StripeEmbeddedComponent].
     */
    internal fun getStripeURL(component: StripeEmbeddedComponent): String {
        return buildString {
            append("https://connect-js.stripe.com/v1.0/android_webview.html")
            append("#component=${component.componentName}")
            append("&publicKey=${configuration.publishableKey}")
        }
    }

    /**
     * Fetch the client secret from the consumer of the SDK.
     */
    internal suspend fun fetchClientSecret(): String? {
        return suspendCancellableCoroutine { continuation ->
            val resultCallback = object : FetchClientSecretCallback.ClientSecretResultCallback {
                override fun onResult(secret: String?) {
                    continuation.resume(secret)
                }
            }
            fetchClientSecretCallback.fetchClientSecret(resultCallback)
        }
    }

    @PrivateBetaConnectSDK
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun update(appearance: Appearance) {
        _appearanceFlow.value = appearance
    }

    @PrivateBetaConnectSDK
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun logout() {
        throw NotImplementedError("Logout functionality is not yet implemented")
    }

    // Configuration

    @PrivateBetaConnectSDK
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class Configuration(
        val publishableKey: String,
    ) : Parcelable
}
