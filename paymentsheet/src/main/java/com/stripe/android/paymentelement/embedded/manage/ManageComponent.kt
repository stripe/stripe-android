package com.stripe.android.paymentelement.embedded.manage

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
    ],
)
@Singleton
internal interface ManageComponent {
    val viewModel: ManageViewModel
    val customerStateHolder: CustomerStateHolder
    val selectionHolder: EmbeddedSelectionHolder
    fun inject(activity: ManageActivity)

    @Component.Factory
    interface Factory {
        fun build(
            @BindsInstance savedStateHandle: SavedStateHandle,
            @BindsInstance paymentMethodMetadata: PaymentMethodMetadata,
            @BindsInstance application: Application,
            @BindsInstance @PaymentElementCallbackIdentifier
            paymentElementCallbackIdentifier: String,
            @BindsInstance selectedPaymentMethodCode: PaymentMethodCode,
            @BindsInstance hasSavedPaymentMethods: Boolean,
            @BindsInstance
            @Named(STATUS_BAR_COLOR)
            statusBarColor: Int?,
            @BindsInstance configuration: EmbeddedPaymentElement.Configuration,
            @BindsInstance promotion: PaymentMethodMessagePromotion?,
        ): ManageComponent
    }
}
