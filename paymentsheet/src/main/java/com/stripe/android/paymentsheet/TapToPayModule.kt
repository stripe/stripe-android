package com.stripe.android.paymentsheet

import android.app.Application
import android.content.Context
import com.scottyab.rootbeer.RootBeer
import com.stripe.core.device.SdkInt
import com.stripe.core.device.dagger.BuildValuesModule
import com.stripe.core.device.dagger.DefaultSerialSupplierModule
import com.stripe.core.device.dagger.DeviceInfoModule
import com.stripe.cots.common.CotsClient
import com.stripe.cots.common.CotsClientInterface
import com.stripe.cots.common.CotsConnectionManager
import com.stripe.cots.common.compatibility.PreFlightChecks
import com.stripe.jvmcore.dagger.ForApplication
import com.stripe.jvmcore.dagger.IO
import com.stripe.jvmcore.logging.terminal.log.DefaultSimpleLogger
import com.stripe.jvmcore.logging.terminal.log.LoggerFactory
import com.stripe.logwriter.ConsoleLogWriter
import com.stripe.logwriter.LogWriter
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.internal.common.appinfo.SdkApplicationInformationFactory
import com.stripe.stripeterminal.internal.common.dagger.AppInfoModule
import com.stripe.terminal.appinfo.ApplicationInformation
import com.stripe.terminal.appinfo.ApplicationInformationProvider
import com.stripe.transaction.dagger.SettingsModule
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module(
    includes = [
        AppInfoModule::class,
        SettingsModule::class,
        DeviceInfoModule::class,
        BuildValuesModule::class,
        DefaultSerialSupplierModule::class,
    ]
)
interface TapToPayClientModule {
    @Binds
    fun bindsCotsClient(cotsClient: CotsClient): CotsClientInterface

    @Binds
    fun bindsConnectionTokenProvider(provider: StripeApiTokenProvider): ConnectionTokenProvider

    @Binds
    fun bindsLogWriter(logger: ConsoleLogWriter): LogWriter

    companion object {
        @Singleton
        @ForApplication
        @Provides
        fun providesContext(application: Application): Context = application.applicationContext

        @Singleton
        @IO
        @Provides
        fun providesIoDispatcher() = Dispatchers.IO

        @Singleton
        @Provides
        fun providesLoggerFactory(
            logWriter: LogWriter
        ) = LoggerFactory { className ->
            DefaultSimpleLogger(
                className = className.simpleName ?: "StripeAndroidTapToPay",
                logWriter = logWriter,
            )
        }

        @Singleton
        @Provides
        fun providesApplicationInformationProvider(
            lazySdkApplicationInformationFactory: Lazy<SdkApplicationInformationFactory>,
        ): ApplicationInformationProvider {
            return object : ApplicationInformationProvider {
                private val sdkApplicationInformation by lazy {
                    lazySdkApplicationInformationFactory.get().create()
                }

                override fun get(): ApplicationInformation = sdkApplicationInformation
            }
        }

        @Provides
        @Singleton
        fun providesLogWriter() = ConsoleLogWriter

        @Singleton
        @Provides
        fun providesCotsClient(
            @ForApplication context: Context,
            @IO ioDispatcher: CoroutineDispatcher,
            loggerFactory: LoggerFactory,
            sdkInt: SdkInt,
        ): CotsClient {
            return CotsClient(
                context = context,
                connectionManager = CotsConnectionManager(
                    context = context,
                    connectionOwner = "StripeAndroid",
                    isSimulated = false,
                    dispatcher = ioDispatcher,
                    loggerFactory = loggerFactory,
                ),
                preFlightChecks = PreFlightChecks(
                    context = context,
                    rootBeer = RootBeer(context),
                    sdkInt = sdkInt,
                ),
                logger = loggerFactory.create(CotsClient::class)
            )
        }

        @Singleton
        @Provides
        fun providesStripeApiTokenProvider(): StripeApiTokenProvider = StripeApiTokenProvider
    }
}
