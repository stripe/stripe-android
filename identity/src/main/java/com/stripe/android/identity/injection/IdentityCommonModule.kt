package com.stripe.android.identity.injection

import android.content.Context
import android.content.res.Resources
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.identity.networking.DefaultIdentityModelFetcher
import com.stripe.android.identity.networking.DefaultIdentityRepository
import com.stripe.android.identity.networking.IdentityModelFetcher
import com.stripe.android.identity.networking.IdentityRepository
import com.stripe.android.identity.utils.DefaultIdentityIO
import com.stripe.android.identity.utils.IdentityIO
import com.stripe.android.mlcore.base.InterpreterInitializer
import com.stripe.android.mlcore.impl.InterpreterInitializerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(
    subcomponents = [IdentityActivitySubcomponent::class]
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
    abstract fun bindIDDetectorFetcher(defaultIDDetectorFetcher: DefaultIdentityModelFetcher): IdentityModelFetcher

    companion object {
        @Provides
        @Singleton
        fun provideStripeNetworkClient(): StripeNetworkClient = DefaultStripeNetworkClient()

        @Provides
        @Singleton
        fun provideResources(context: Context): Resources = context.resources

        @Provides
        @Singleton
        fun provideInterpreterInitializer(): InterpreterInitializer = InterpreterInitializerImpl
    }
}
