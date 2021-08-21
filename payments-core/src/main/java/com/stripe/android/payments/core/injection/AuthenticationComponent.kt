package com.stripe.android.payments.core.injection

import android.content.Context
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.core.authentication.DefaultPaymentAuthenticatorRegistry
import com.stripe.android.payments.core.authentication.threeds2.Stripe3ds2TransactionViewModelFactory
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * [Component] for com.stripe.android.payments.core.authentication.
 *
 * It holds the dagger graph for [DefaultPaymentAuthenticatorRegistry], with
 * more dependencies daggerized and a higher level [Component]s created, this class will be merged
 * into it.
 */
@Singleton
@Component(
    modules = [
        AuthenticationModule::class,
        Stripe3DSAuthenticatorModule::class,
        WeChatPayAuthenticatorModule::class
    ]
)
internal interface AuthenticationComponent {
    val registry: DefaultPaymentAuthenticatorRegistry

    fun inject(stripe3ds2TransactionViewModelFactory: Stripe3ds2TransactionViewModelFactory)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        fun stripeRepository(stripeRepository: StripeRepository): Builder

        @BindsInstance
        fun analyticsRequestExecutor(analyticsRequestExecutor: AnalyticsRequestExecutor): Builder

        @BindsInstance
        fun analyticsRequestFactory(analyticsRequestFactory: AnalyticsRequestFactory): Builder

        @BindsInstance
        fun enableLogging(@Named(ENABLE_LOGGING) enableLogging: Boolean): Builder

        @BindsInstance
        fun workContext(@IOContext workContext: CoroutineContext): Builder

        @BindsInstance
        fun uiContext(@UIContext uiContext: CoroutineContext): Builder

        @BindsInstance
        fun threeDs1IntentReturnUrlMap(
            threeDs1IntentReturnUrlMap: MutableMap<String, String>
        ): Builder

        @BindsInstance
        fun injectorKey(@InjectorKey id: Int): Builder

        fun build(): AuthenticationComponent
    }
}
