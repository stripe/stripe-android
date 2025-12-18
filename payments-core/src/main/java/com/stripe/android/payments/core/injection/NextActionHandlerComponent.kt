package com.stripe.android.payments.core.injection

import android.content.Context
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.UIContext
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.core.authentication.DefaultPaymentNextActionHandlerRegistry
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * [Component] for com.stripe.android.payments.core.authentication.
 *
 * It holds the dagger graph for [DefaultPaymentNextActionHandlerRegistry], with
 * more dependencies daggerized and a higher level [Component]s created, this class will be merged
 * into it.
 */
@Singleton
@Component(
    modules = [
        NextActionHandlerModule::class,
        Stripe3DSNextActionHandlerModule::class,
        WeChatPayNextActionHandlerModule::class,
        CoreCommonModule::class,
        StripeRepositoryModule::class,
    ]
)
internal interface NextActionHandlerComponent {
    val registry: DefaultPaymentNextActionHandlerRegistry

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance
            context: Context,
            @BindsInstance
            analyticsRequestFactory: PaymentAnalyticsRequestFactory,
            @BindsInstance
            @Named(ENABLE_LOGGING)
            enableLogging: Boolean,
            @BindsInstance
            @IOContext
            workContext: CoroutineContext,
            @BindsInstance
            @UIContext
            uiContext: CoroutineContext,
            @BindsInstance
            @Named(PUBLISHABLE_KEY)
            publishableKeyProvider: () -> String,
            @BindsInstance
            @Named(PRODUCT_USAGE)
            productUsage: Set<String>,
            @BindsInstance
            @Named(IS_INSTANT_APP)
            isInstantApp: Boolean,
            @BindsInstance
            @Named(INCLUDE_PAYMENT_SHEET_NEXT_ACTION_HANDLERS)
            includePaymentSheetNextActionHandlers: Boolean,
        ): NextActionHandlerComponent
    }
}
