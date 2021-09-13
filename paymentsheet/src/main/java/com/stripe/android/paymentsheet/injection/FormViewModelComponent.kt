package com.stripe.android.paymentsheet.injection

import android.content.res.Resources
import com.stripe.android.paymentsheet.elements.LayoutSpec
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        FormViewModelModule::class,
    ]
)
internal interface FormViewModelComponent {
    val viewModel: FormViewModel

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun layout(layoutSpec: LayoutSpec): Builder

        @BindsInstance
        fun formFragmentArguments(
            config: FormFragmentArguments
        ): Builder

        @BindsInstance
        fun resources(resources: Resources): Builder

        fun build(): FormViewModelComponent
    }
}
