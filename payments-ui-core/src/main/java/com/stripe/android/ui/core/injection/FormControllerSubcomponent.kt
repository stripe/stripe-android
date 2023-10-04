package com.stripe.android.ui.core.injection

import androidx.annotation.RestrictTo
import com.stripe.android.core.injection.INITIAL_VALUES
import com.stripe.android.core.injection.SHIPPING_VALUES
import com.stripe.android.model.StripeIntent
import com.stripe.android.ui.core.FormController
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.uicore.elements.IdentifierSpec
import dagger.BindsInstance
import dagger.Subcomponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Named

/**
 * Subcomponent used to instantiate [FormController].
 */
@Subcomponent(
    modules = [FormControllerModule::class]
)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface FormControllerSubcomponent {
    val formController: FormController

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun formSpec(formSpec: LayoutSpec): Builder

        @BindsInstance
        fun initialValues(
            @Named(INITIAL_VALUES) initialValues: Map<IdentifierSpec, String?>
        ): Builder

        @BindsInstance
        fun shippingValues(
            @Named(SHIPPING_VALUES) shippingAddress: Map<IdentifierSpec, String?>?
        ): Builder

        @BindsInstance
        fun viewModelScope(viewModelScope: CoroutineScope): Builder

        @BindsInstance
        fun stripeIntent(stripeIntent: StripeIntent?): Builder

        @BindsInstance
        fun merchantName(merchantName: String): Builder

        fun build(): FormControllerSubcomponent
    }
}
