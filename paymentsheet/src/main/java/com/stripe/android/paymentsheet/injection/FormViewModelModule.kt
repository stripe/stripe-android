package com.stripe.android.paymentsheet.injection

import android.content.res.Resources
import com.stripe.android.paymentsheet.elements.ResourceRepository
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
internal abstract class FormViewModelModule {

    companion object {

        @Provides
        @Singleton
        fun provideResourceRepositoryFactory(
            resource: Resources,
        ) = ResourceRepository.getInstance(resource)
    }
}
