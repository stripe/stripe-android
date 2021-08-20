package com.stripe.android.paymentsheet.injection

import android.content.res.Resources
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.paymentdatacollection.ComposeFragmentArguments
import com.stripe.android.paymentsheet.specifications.LayoutSpec
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component
internal interface FormViewModelComponent {
    val viewModel: FormViewModel

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun layout(layoutSpec: LayoutSpec): Builder

        @BindsInstance
        fun config(
            config: ComposeFragmentArguments
        ): Builder

        @BindsInstance
        fun resources(resources: Resources): Builder

        fun build(): FormViewModelComponent
    }
}
