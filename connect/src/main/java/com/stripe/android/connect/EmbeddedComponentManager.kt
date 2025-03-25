package com.stripe.android.connect

import android.content.Context
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.annotation.RestrictTo
import androidx.fragment.app.FragmentActivity
import com.stripe.android.connect.appearance.Appearance
import com.stripe.android.connect.appearance.fonts.CustomFontSource
import com.stripe.android.connect.di.StripeConnectComponent
import com.stripe.android.connect.manager.EmbeddedComponentCoordinator
import kotlinx.parcelize.Parcelize

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class EmbeddedComponentManager @JvmOverloads constructor(
    configuration: Configuration,
    fetchClientSecretCallback: FetchClientSecretCallback,
    appearance: Appearance = Appearance.default(),
    customFonts: List<CustomFontSource> = emptyList(),
) {
    internal val coordinator: EmbeddedComponentCoordinator =
        StripeConnectComponent.instance
            .coordinatorComponentProvider
            .get()
            .build(
                configuration = configuration,
                fetchClientSecretCallback = fetchClientSecretCallback,
                customFonts = customFonts,
                appearance = appearance,
            )
            .coordinator

    /**
     * Returns a controller for presenting the Account Onboarding component full screen.
     *
     * @param activity The [FragmentActivity] to present the component in.
     * @param title Optional title to display in the toolbar.
     * @param props Optional props to use for configuring the component.
     */
    @JvmOverloads
    fun createAccountOnboardingController(
        activity: FragmentActivity,
        title: String? = null,
        props: AccountOnboardingProps? = null,
    ): AccountOnboardingController {
        return AccountOnboardingController(
            activity = activity,
            embeddedComponentManager = this,
            title = title,
            props = props,
        )
    }

    /**
     * Create a new [AccountOnboardingView] for inclusion in the view hierarchy.
     *
     * @param context The [Context] to use for creating the view.
     * @param listener Optional listener to use for handling events from the view.
     * @param props Optional props to use for configuring the view.
     * @param cacheKey Key to use for caching the internal WebView across configuration changes.
     */
    internal fun createAccountOnboardingView(
        context: Context,
        listener: AccountOnboardingListener? = null,
        props: AccountOnboardingProps? = null,
        cacheKey: String? = null,
    ): AccountOnboardingView {
        return AccountOnboardingView(
            context = context,
            embeddedComponentManager = this,
            listener = listener,
            props = props,
            cacheKey = cacheKey,
        )
    }

    /**
     * Create a new [PayoutsView] for inclusion in the view hierarchy.
     *
     * @param context The [Context] to use for creating the view.
     * @param listener Optional [PayoutsListener] to use for handling events from the view.
     * @param cacheKey Key to use for caching the internal WebView within an Activity across configuration changes.
     */
    internal fun createPayoutsView(
        context: Context,
        listener: PayoutsListener? = null,
        cacheKey: String? = null,
    ): PayoutsView {
        return PayoutsView(
            context = context,
            embeddedComponentManager = this,
            listener = listener,
            cacheKey = cacheKey,
        )
    }

    fun update(appearance: Appearance) {
        coordinator.update(appearance)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class Configuration(
        val publishableKey: String,
    ) : Parcelable

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        /**
         * Hooks the [EmbeddedComponentManager] into this activity's lifecycle.
         *
         * Must be called in [ComponentActivity.onCreate], passing in the instance of the
         * activity as [activity]. This must be called in all activities where an EmbeddedComponent
         * view is used.
         */
        @JvmStatic
        fun onActivityCreate(activity: ComponentActivity) {
            EmbeddedComponentCoordinator.onActivityCreate(activity)
        }
    }
}
