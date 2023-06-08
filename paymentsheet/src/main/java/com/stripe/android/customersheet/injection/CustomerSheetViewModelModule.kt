package com.stripe.android.customersheet.injection

import android.app.Application
import android.content.res.Resources
import com.stripe.android.ui.core.forms.resources.LpmRepository
import dagger.Module
import dagger.Provides

@Module
internal class CustomerSheetViewModelModule {

    @Provides
    fun resources(application: Application): Resources {
        return application.resources
    }

    @Provides
    fun provideLpmRepository(resources: Resources): LpmRepository {
        return LpmRepository.getInstance(
            LpmRepository.LpmRepositoryArguments(resources)
        )
    }
}
