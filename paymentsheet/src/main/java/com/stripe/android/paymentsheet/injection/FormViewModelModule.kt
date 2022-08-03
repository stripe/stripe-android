package com.stripe.android.paymentsheet.injection

import com.stripe.android.ui.core.forms.resources.AsyncResourceRepository
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import dagger.Binds
import dagger.Module

@Module(
    subcomponents = [FormViewModelSubcomponent::class]
)
internal abstract class FormViewModelModule {
    @Binds
    abstract fun bindsResourceRepository(asyncResourceRepository: AsyncResourceRepository):
        ResourceRepository
}
