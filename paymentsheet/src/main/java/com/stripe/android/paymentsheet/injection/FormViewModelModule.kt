package com.stripe.android.paymentsheet.injection

import androidx.core.os.LocaleListCompat
import com.stripe.android.ui.core.forms.resources.AsyncResourceRepository
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(
    subcomponents = [FormViewModelSubcomponent::class]
)
internal abstract class FormViewModelModule {

    @Binds
    abstract fun bindsResourceRepository(asyncResourceRepository: AsyncResourceRepository):
        ResourceRepository

    companion object {
        @Provides
        @Singleton
        fun provideLocale() =
            LocaleListCompat.getAdjustedDefault().takeUnless { it.isEmpty }?.get(0)
    }
}
