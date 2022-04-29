package com.stripe.android.identity.injection

import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.identity.networking.DefaultIDDetectorFetcher
import com.stripe.android.identity.networking.DefaultIdentityRepository
import com.stripe.android.identity.networking.IDDetectorFetcher
import com.stripe.android.identity.networking.IdentityRepository
import com.stripe.android.identity.utils.DefaultIdentityIO
import com.stripe.android.identity.utils.IdentityIO
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(
    subcomponents = [IdentityViewModelSubcomponent::class]
)
internal abstract class IdentityCommonModule {
    @Binds
    @Singleton
    abstract fun bindIdentityIO(defaultIdentityIO: DefaultIdentityIO): IdentityIO

    @Binds
    @Singleton
    abstract fun bindRepository(defaultIdentityRepository: DefaultIdentityRepository): IdentityRepository

    @Binds
    @Singleton
    abstract fun bindIDDetectorFetcher(defaultIDDetectorFetcher: DefaultIDDetectorFetcher): IDDetectorFetcher

    companion object {
        @Provides
        @Singleton
        fun provideStripeNetworkClient(): StripeNetworkClient = DefaultStripeNetworkClient()
    }
}
