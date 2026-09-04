package com.stripe.android.paymentelement.embedded.sheet

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.common.di.ElementsSessionClientParamsModule
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.googlepaylauncher.injection.GooglePayLauncherModule
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayPaymentDataUpdateNoOpModule
import com.stripe.android.paymentelement.confirmation.injection.ExtendedPaymentElementConfirmationModule
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgsHolder
import com.stripe.android.paymentelement.embedded.EmbeddedActivityModule
import com.stripe.android.paymentelement.embedded.EmbeddedCommonModule
import com.stripe.android.paymentelement.embedded.EmbeddedLinkExtrasModule
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.injection.PaymentMethodMessagePromotionsExperimentHandlerModule
import com.stripe.android.paymentsheet.injection.PaymentSheetAutocompleteModule
import dagger.BindsInstance
import dagger.Component
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Component(
    modules = [
        EmbeddedActivityModule::class,
        EmbeddedCommonModule::class,
        ElementsSessionClientParamsModule::class,
        ExtendedPaymentElementConfirmationModule::class,
        GooglePayPaymentDataUpdateNoOpModule::class,
        GooglePayLauncherModule::class,
        EmbeddedLinkExtrasModule::class,
        PaymentMethodMessagePromotionsExperimentHandlerModule::class,
        PaymentSheetAutocompleteModule::class,
    ],
)
@Singleton
internal interface EmbeddedSheetComponent {
    val selectionHolder: EmbeddedSelectionHolder
    val customerStateHolder: CustomerStateHolder
    val argsUpdater: EmbeddedActivityArgsUpdater

    fun inject(activity: EmbeddedSheetActivity)

    @Component.Factory
    interface Factory {
        fun build(
            @BindsInstance argsHolder: EmbeddedActivityArgsHolder,
            @BindsInstance application: Application,
            @BindsInstance savedStateHandle: SavedStateHandle,
            @BindsInstance
            @ViewModelScope
            viewModelScope: CoroutineScope,
        ): EmbeddedSheetComponent
    }
}
