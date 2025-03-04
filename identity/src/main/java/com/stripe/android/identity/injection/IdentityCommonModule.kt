package com.stripe.android.identity.injection

import android.content.Context
import android.content.res.Resources
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.identity.networking.DefaultIdentityRepository
import com.stripe.android.identity.networking.IdentityRepository
import com.stripe.android.mlcore.base.InterpreterInitializer
import com.stripe.android.mlcore.impl.InterpreterInitializerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import javax.inject.Named
import javax.inject.Singleton

@Module(
    subcomponents = [IdentityActivitySubcomponent::class]
)
internal abstract class IdentityCommonModule {
    @Binds
    @Singleton
    abstract fun bindRepository(defaultIdentityRepository: DefaultIdentityRepository): IdentityRepository

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

        @OptIn(DelicateCoroutinesApi::class)
        @Provides
        @Singleton
        @Named(GLOBAL_SCOPE)
        fun provideGlobalScope(): CoroutineScope = GlobalScope

        const val GLOBAL_SCOPE = "GlobalScope"
    }
}
