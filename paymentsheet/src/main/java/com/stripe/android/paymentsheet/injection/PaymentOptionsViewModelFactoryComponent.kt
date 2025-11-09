package com.stripe.android.paymentsheet.injection

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.common.di.ApplicationIdModule
import com.stripe.android.common.di.MobileSessionIdModule
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.googlepaylauncher.injection.GooglePayLauncherModule
import com.stripe.android.networking.PaymentElementRequestSurfaceModule
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.confirmation.cvc.CvcRecollectionConfirmationModule
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.ui.core.forms.resources.injection.ResourceRepositoryModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        StripeRepositoryModule::class,
        PaymentSheetCommonModule::class,
        PaymentElementRequestSurfaceModule::class,
        PaymentOptionsViewModelModule::class,
        CoroutineContextModule::class,
        CoreCommonModule::class,
        ResourceRepositoryModule::class,
        CvcRecollectionConfirmationModule::class,
        ApplicationIdModule::class,
        MobileSessionIdModule::class,
        LinkHoldbackExposureModule::class,
        GooglePayLauncherModule::class,
    ]
)
internal interface PaymentOptionsViewModelFactoryComponent {
    val paymentOptionsViewModelSubcomponentBuilder: PaymentOptionsViewModelSubcomponent.Builder

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun savedStateHandle(handle: SavedStateHandle): Builder

        @BindsInstance
        fun productUsage(@Named(PRODUCT_USAGE) productUsage: Set<String>): Builder

        @BindsInstance
        fun paymentElementCallbackIdentifier(
            @PaymentElementCallbackIdentifier paymentElementCallbackIdentifier: String
        ): Builder

        fun build(): PaymentOptionsViewModelFactoryComponent
    }
}
