package com.stripe.android.paymentsheet.injection

import android.content.Context
import com.stripe.android.paymentsheet.specifications.BankRepositoryInstance
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Common module providing payment related dependencies.
 * In order to use this module, [Context] need to be provided elsewhere.
 */
@Module
internal class ResourceRepositoryModule {

    @Provides
    @Singleton
    fun provideBankRepository(
        appContext: Context,
    ) = BankRepositoryInstance(appContext.resources)

}
