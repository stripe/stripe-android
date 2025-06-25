package com.stripe.android.shoppay.di

import android.content.Context
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.shoppay.ShopPayViewModel
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named

@Component(
    modules = [
        ShopPayModule::class,
        ShopPayViewModelModule::class
    ]
)
internal interface ShopPayComponent {
    val viewModel: ShopPayViewModel

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun configuration(configuration: PaymentSheet.ShopPayConfiguration): Builder

        @BindsInstance
        fun publishableKey(@Named(PUBLISHABLE_KEY) publishableKey: String): Builder

        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        fun statusBarColor(@Named(STATUS_BAR_COLOR) statusBarColor: Int?): Builder

        fun build(): ShopPayComponent
    }
}
