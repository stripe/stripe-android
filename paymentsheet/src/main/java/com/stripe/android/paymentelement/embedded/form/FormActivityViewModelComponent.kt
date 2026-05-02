package com.stripe.android.paymentelement.embedded.form

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.common.di.ApplicationIdModule
import com.stripe.android.googlepaylauncher.injection.GooglePayLauncherModule
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.confirmation.injection.ExtendedPaymentElementConfirmationModule
import com.stripe.android.paymentelement.embedded.EmbeddedActivityModule
import com.stripe.android.paymentelement.embedded.EmbeddedCommonModule
import com.stripe.android.paymentelement.embedded.EmbeddedLinkExtrasModule
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.paymentsheet.CustomerStateHolder
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton

@Component(
    modules = [
        EmbeddedActivityModule::class,
        EmbeddedCommonModule::class,
        ApplicationIdModule::class,
        ExtendedPaymentElementConfirmationModule::class,
        GooglePayLauncherModule::class,
        EmbeddedLinkExtrasModule::class,
    ]
)
@Singleton
internal interface FormActivityViewModelComponent {
    val viewModel: FormActivityViewModel
    val selectionHolder: EmbeddedSelectionHolder
    val customerStateHolder: CustomerStateHolder
    fun inject(formActivity: FormActivity)

    @Component.Factory
    interface Factory {
        fun build(
            @BindsInstance paymentMethodMetadata: PaymentMethodMetadata,
            @BindsInstance selectedPaymentMethodCode: PaymentMethodCode,
            @BindsInstance hasSavedPaymentMethods: Boolean,
            @BindsInstance
            @Named(STATUS_BAR_COLOR)
            statusBarColor: Int?,
            @BindsInstance configuration: EmbeddedPaymentElement.Configuration,
            @BindsInstance
            @PaymentElementCallbackIdentifier
            paymentElementCallbackIdentifier: String,
            @BindsInstance application: Application,
            @BindsInstance savedStateHandle: SavedStateHandle,
            @BindsInstance promotions: List<PaymentMethodMessagePromotion>?,
        ): FormActivityViewModelComponent
    }
}
