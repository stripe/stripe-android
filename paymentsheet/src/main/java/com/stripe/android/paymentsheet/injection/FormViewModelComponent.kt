package com.stripe.android.paymentsheet.injection

import android.app.Application
import android.content.Context
import android.content.res.Resources
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.specifications.LayoutSpec
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        ResourceRepositoryModule::class,
        FormViewModelModule::class
    ]
)
internal interface FormViewModelComponent {
    val viewModel: FormViewModel

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun layout(layoutSpec: LayoutSpec): Builder

        @BindsInstance
        fun saveForFutureUseValue(
            @Named(SAVE_FOR_FUTURE_USE_INITIAL_VALUE) saveForFutureUseValue: Boolean
        ): Builder

        @BindsInstance
        fun saveForFutureUseVisibility(
            @Named(SAVE_FOR_FUTURE_USE_INITIAL_VISIBILITY) saveForFutureUseVisibility: Boolean
        ): Builder

        @BindsInstance
        fun merchantName(merchantName: String): Builder

        @BindsInstance
        fun resources(resources: Resources): Builder

        fun build(): FormViewModelComponent
    }
}

@Module
internal abstract class FormViewModelModule {

    @Binds
    abstract fun bindsApplicationForContext(application: Application): Context

    companion object {
    }
}
