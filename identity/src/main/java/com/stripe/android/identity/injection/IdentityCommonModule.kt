package com.stripe.android.identity.injection

import android.app.Application
import android.content.Context
import android.content.res.Resources
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.AnalyticsRequestV2Executor
import com.stripe.android.core.networking.DefaultAnalyticsRequestV2Executor
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.utils.IsWorkManagerAvailable
import com.stripe.android.core.utils.RealIsWorkManagerAvailable
import com.stripe.android.identity.BuildConfig
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import javax.inject.Named
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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

    @Binds
    @Singleton
    abstract fun bindsAnalyticsRequestV2Executor(impl: DefaultAnalyticsRequestV2Executor): AnalyticsRequestV2Executor

    companion object {

        const val GLOBAL_SCOPE = "GlobalScope"

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

        @Provides
        @Singleton
        fun provideLogger(): Logger = Logger.getInstance(BuildConfig.DEBUG)

        @Provides
        @Singleton
        fun provideApplication(context: Context): Application {
            return context.applicationContext as Application
        }

        @Provides
        @Singleton
        internal fun providesIsWorkManagerAvailable(): IsWorkManagerAvailable {
            return RealIsWorkManagerAvailable
        }

        @Provides
        @Singleton
        internal fun providesIoDispatcher(): CoroutineDispatcher {
            return Dispatchers.IO
        }
    }
}
