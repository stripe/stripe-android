package com.stripe.android.link.injection

import com.stripe.android.link.ui.forms.FormController
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import dagger.BindsInstance
import dagger.Subcomponent
import kotlinx.coroutines.CoroutineScope

/**
 * Subcomponent used to instantiate [FormController].
 */
@Subcomponent(
    modules = [FormControllerModule::class]
)
internal interface FormControllerSubcomponent {
    val formController: FormController

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun formSpec(formSpec: LayoutSpec): Builder

        @BindsInstance
        fun initialValues(initialValues: Map<IdentifierSpec, String?>): Builder

        @BindsInstance
        fun viewOnlyFields(viewOnlyFields: Set<IdentifierSpec>): Builder

        @BindsInstance
        fun viewModelScope(viewModelScope: CoroutineScope): Builder

        fun build(): FormControllerSubcomponent
    }
}
