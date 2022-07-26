package com.stripe.android.ui.core.forms.resources.injection

import android.content.Context
import android.content.res.Resources
import com.stripe.android.ui.core.address.AddressRepository
import com.stripe.android.ui.core.forms.resources.AsyncAddressResourceRepository
import com.stripe.android.ui.core.forms.resources.AsyncLpmResourceRepository
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
abstract class ResourceRepositoryModule {
    @Binds
    abstract fun bindsLpmRepository(lpm: AsyncLpmResourceRepository):
        ResourceRepository<LpmRepository>

    @Binds
    abstract fun bindsAddressRepository(address: AsyncAddressResourceRepository):
        ResourceRepository<AddressRepository>

    companion object {
        @Provides
        @Singleton
        fun provideResources(context: Context): Resources {
            return context.resources
        }
    }
}
