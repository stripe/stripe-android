package com.stripe.android.paymentsheet.injection

import android.content.Context
import android.content.res.Resources
import com.stripe.android.paymentsheet.address.AddressFieldElementRepository
import com.stripe.android.paymentsheet.specifications.BankRepository
import com.stripe.android.paymentsheet.specifications.ResourceRepository
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
        resources: Resources,
    ) = BankRepository(resources)

    @Provides
    @Singleton
    fun provideAddressFieldRepository(
        resources: Resources,
    ) = AddressFieldElementRepository(resources)

    @Provides
    @Singleton
    fun provideResourceRepository(
        bankRepository: BankRepository,
        addressFieldElementRepository: AddressFieldElementRepository
    ) = ResourceRepository(bankRepository, addressFieldElementRepository)
}
