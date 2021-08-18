package com.stripe.android.payments.core.injection

import android.content.Context
import androidx.annotation.Nullable
import com.stripe.android.payments.paymentlauncher.PaymentLauncherViewModel
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        PaymentLauncherModule::class
    ]
)
internal interface PaymentLauncherComponent {
    fun inject(factory: PaymentLauncherViewModel.Factory)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        fun publishableKey(@Named(PUBLISHABLE_KEY) publishableKey: String): Builder

        @BindsInstance
        fun stripeAccountId(@Nullable @Named(STRIPE_ACCOUNT_ID) stripeAccountId: String?): Builder

        fun build(): PaymentLauncherComponent
    }
}
