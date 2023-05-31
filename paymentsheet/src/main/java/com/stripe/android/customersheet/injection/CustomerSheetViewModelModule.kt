package com.stripe.android.customersheet.injection

import android.content.Context
import com.stripe.android.ui.core.forms.resources.LpmRepository
import dagger.Module
import dagger.Provides

@Module
internal class CustomerSheetViewModelModule {

    @Provides
    fun provideLpmRepository(context: Context): LpmRepository {
        return LpmRepository.getInstance(LpmRepository.LpmRepositoryArguments(context.resources))
    }
}
