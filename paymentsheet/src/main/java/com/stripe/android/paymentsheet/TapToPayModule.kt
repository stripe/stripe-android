package com.stripe.android.paymentsheet

import android.app.Application
import android.content.Context
import android.os.Build
import com.scottyab.rootbeer.RootBeer
import com.stripe.core.device.BuildValues
import com.stripe.core.device.SdkInt
import com.stripe.core.device.dagger.BuildValuesModule
import com.stripe.core.device.dagger.DefaultSerialSupplierModule
import com.stripe.core.device.dagger.DeviceInfoModule
import com.stripe.core.readerupdate.EmbeddedSoftwareVersionProvider
import com.stripe.core.storage.dagger.AndroidStorageModule
import com.stripe.cots.common.CotsClient
import com.stripe.cots.common.CotsClientInterface
import com.stripe.cots.common.CotsConnectionManager
import com.stripe.cots.common.compatibility.PreFlightChecks
import com.stripe.device.DeviceInfoRepository
import com.stripe.device.PlatformDeviceInfo
import com.stripe.jvmcore.client.OkHttpClientProvider
import com.stripe.jvmcore.client.dagger.HttpClientModule
import com.stripe.jvmcore.dagger.AppScope
import com.stripe.jvmcore.dagger.Debug
import com.stripe.jvmcore.dagger.Files
import com.stripe.jvmcore.dagger.ForApplication
import com.stripe.jvmcore.dagger.IO
import com.stripe.jvmcore.dagger.IsCotsIncluded
import com.stripe.jvmcore.dagger.Mainland
import com.stripe.jvmcore.environment.Environment
import com.stripe.jvmcore.environment.UserAgentProvider
import com.stripe.jvmcore.logging.terminal.log.DefaultSimpleLogger
import com.stripe.jvmcore.logging.terminal.log.LoggerFactory
import com.stripe.jvmcore.redaction.terminal.TerminalMessageRedactor
import com.stripe.jvmcore.restclient.IdempotencyGenerator
import com.stripe.jvmcore.restclient.IdempotencyHeader
import com.stripe.jvmcore.restclient.IdempotencyRetryInterceptor
import com.stripe.jvmcore.restclient.MainlandIdempotencyGenerator
import com.stripe.jvmcore.restclient.RestClient
import com.stripe.jvmcore.terminal.requestfactories.dagger.TerminalRestRequestFactoryModule
import com.stripe.logwriter.ConsoleLogWriter
import com.stripe.logwriter.LogWriter
import com.stripe.offlinemode.OfflineReaderSetup
import com.stripe.proto.model.common.VersionInfoPb
import com.stripe.proto.model.rest.UserAgent
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.callable.TerminalListener
import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.internal.common.Adapter
import com.stripe.stripeterminal.internal.common.LocationHandler
import com.stripe.stripeterminal.internal.common.TerminalStatusManager
import com.stripe.stripeterminal.internal.common.adapter.AdapterRepository
import com.stripe.stripeterminal.internal.common.adapter.ProxyAdapter
import com.stripe.stripeterminal.internal.common.api.GeolocationUpdateListener
import com.stripe.stripeterminal.internal.common.appinfo.SdkApplicationInformationFactory
import com.stripe.stripeterminal.internal.common.dagger.AppInfoModule
import com.stripe.stripeterminal.internal.common.dagger.CommonModule
import com.stripe.stripeterminal.internal.common.deviceinfo.AndroidSdkCotsHardwareProvider
import com.stripe.stripeterminal.internal.common.deviceinfo.CotsHardwareProvider
import com.stripe.stripeterminal.internal.common.deviceinfo.ReaderPlatformDeviceInfo
import com.stripe.stripeterminal.internal.common.remotereadercontrollers.ReaderActivator
import com.stripe.stripeterminal.internal.common.resourcerepository.ReaderActivationListener
import com.stripe.stripeterminal.internal.common.resourcerepository.ResourceRepository
import com.stripe.stripeterminal.internal.common.terminalsession.SessionTokenInterceptor
import com.stripe.stripeterminal.internal.common.terminalsession.activate.BackgroundReaderActivator
import com.stripe.stripeterminal.internal.common.terminalsession.activate.DefaultBackgroundReaderActivator
import com.stripe.stripeterminal.internal.common.terminalsession.activate.DefaultIpReaderActivator
import com.stripe.stripeterminal.internal.common.terminalsession.activate.DefaultTerminalSessionReaderActivator
import com.stripe.stripeterminal.internal.common.terminalsession.activate.TerminalSessionReaderActivator
import com.stripe.terminal.api.DefaultPosInfoFactory
import com.stripe.terminal.api.PosInfoFactory
import com.stripe.terminal.appinfo.ApplicationInformation
import com.stripe.terminal.appinfo.ApplicationInformationProvider
import com.stripe.terminal.tokenrepositories.SessionTokenRepository
import com.stripe.time.Clock
import com.stripe.time.DefaultClock
import com.stripe.transaction.CancelableOperationEventListener
import com.stripe.transaction.CancelableOperationType
import com.stripe.transaction.TransactionRepository
import com.stripe.transaction.dagger.CoreTransactionModule
import com.stripe.transaction.dagger.SettingsModule
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.EventListener
import java.util.Optional
import javax.inject.Singleton
import kotlin.random.Random

@Module(
    includes = [
        AppInfoModule::class,
        SettingsModule::class,
        CommonModule::class,
        CoreTransactionModule::class,
        TerminalRestRequestFactoryModule::class,
        DefaultSerialSupplierModule::class,
        DeviceInfoModule::class,
        BuildValuesModule::class,
        AndroidStorageModule::class,
        SdkPlatformDeviceInfoModule::class,
        HttpClientModule::class,
    ]
)
interface TapToPayClientModule {
    @Binds
    fun bindsCotsClient(cotsClient: CotsClient): CotsClientInterface

    @Binds
    fun bindsConnectionTokenProvider(provider: StripeApiTokenProvider): ConnectionTokenProvider

    @Binds
    fun bindsPosInfoFactory(provider: DefaultPosInfoFactory): PosInfoFactory

    @Binds
    fun bindIdempotencyGenerator(
        mainlandIdempotencyGenerator: MainlandIdempotencyGenerator
    ): IdempotencyGenerator

    @Binds
    fun bindsClock(defaultClock: DefaultClock): Clock

    @Binds
    fun bindsRandom(random: Random.Default): Random

    @Binds
    fun bindsAdapter(adapter: ProxyAdapter): Adapter

    @Binds
    fun bindsLogWriter(logger: ConsoleLogWriter): LogWriter

    @Binds
    fun bindsGeolocationUpdateListener(handler: LocationHandler): GeolocationUpdateListener

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
        @Debug
        @Provides
        fun providesDebug() = BuildConfig.DEBUG

        @Singleton
        @IsCotsIncluded
        @Provides
        fun isCotsIncluded() = true

        @Singleton
        @Provides
        fun okHttpListenerFactory(): Optional<EventListener.Factory> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Optional.ofNullable(null)
        } else {
            throw Exception("Not supported")
        }

        @Provides
        @AppScope
        fun provideAppScope(
            @IO ioDispatcher: CoroutineDispatcher
        ): CoroutineScope = CoroutineScope(
            SupervisorJob() + ioDispatcher + CoroutineName("AppScope")
        )

        @Provides
        fun provideTerminalListener() = object : TerminalListener {}

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

        @Singleton
        @Provides
        fun providesUserAgentProvider(
            deviceInfoRepository: DeviceInfoRepository
        ) = object : UserAgentProvider {
            private val value: UserAgent by lazy {
                val platformDeviceInfo = deviceInfoRepository.getPlatformDeviceInfo()
                UserAgent(
                    client_type = VersionInfoPb.ClientType.ANDROID_READER.name,
                    client_version = "29.32.0",
                    device_uuid = platformDeviceInfo.getSerialNumber()
                )
            }

            override fun get(): UserAgent = value
        }

        @Singleton
        @Provides
        fun providesDefaultPosInfoFactory(
            applicationInformationProvider: ApplicationInformationProvider,
            loggerFactory: LoggerFactory,
        ) = DefaultPosInfoFactory(applicationInformationProvider, loggerFactory.create(PosInfoFactory::class))

        @Provides
        @Singleton
        fun provideTerminalStatusManager(
            listener: TerminalListener,
            transactionRepository: TransactionRepository,
            @AppScope scope: CoroutineScope,
            loggerFactory: LoggerFactory,
        ): TerminalStatusManager {
            return TerminalStatusManager(
                listener = listener,
                transactionRepository = transactionRepository,
                scope = scope,
                loggerFactory = loggerFactory
            )
        }

        @Provides
        @Singleton
        fun providesLogWriter() = ConsoleLogWriter

        @Provides
        @Singleton
        fun providesClock() = DefaultClock

        @Provides
        @Singleton
        fun providesRandom() = Random.Default

        @Singleton
        @Provides
        fun providesCancelableOperationListener() = object : CancelableOperationEventListener {
            override fun onCancelableOperationCanceled(cancelableOperationType: CancelableOperationType) {
                // Nothing
            }

            override fun onCancelableOperationSet(cancelableOperationType: CancelableOperationType) {
                // Nothing
            }
        }

        @Singleton
        @Provides
        fun providesAdapter(
            loggerFactory: LoggerFactory
        ) = ProxyAdapter(
            repository = object : AdapterRepository {
                override val currentAdapter: Adapter? = null

                override fun getAdapterByDiscoveryConfiguration(
                    discoveryConfiguration: DiscoveryConfiguration
                ): Adapter? {
                    return null
                }

                override fun setAdapterByDiscoveryConfiguration(
                    discoveryConfiguration: DiscoveryConfiguration
                ): Adapter? {
                    return null
                }

                override fun setExternalUsbChannelAdapter() {
                    // Adapter
                }
            },
            logger = loggerFactory.create(ProxyAdapter::class)
        )

        @Provides
        @Singleton
        fun provideIdempotencyRetryInterceptor(
            clock: Clock,
            random: Random,
            deviceInfoRepository: DeviceInfoRepository,
        ): IdempotencyRetryInterceptor {
            return IdempotencyRetryInterceptor(
                IdempotencyHeader(
                    MainlandIdempotencyGenerator(
                        clock = clock,
                        random = random,
                        deviceInfoRepository = deviceInfoRepository,
                    )
                )
            )
        }

        @Provides
        @Files
        fun provideFilesRestClient(
            okHttpClientProvider: OkHttpClientProvider,
            idempotencyRetryInterceptor: IdempotencyRetryInterceptor,
            logWriter: LogWriter,
        ): RestClient {
            return RestClient.Builder(
                okHttpClientProvider::get,
                TerminalMessageRedactor,
                logWriter,
            ) { Environment.Prod.filesApiUrl }
                .addCustomRestInterceptor(idempotencyRetryInterceptor)
                .build()
        }

        @Provides
        @Mainland
        fun provideRestClient(
            okHttpClientProvider: OkHttpClientProvider,
            idempotencyRetryInterceptor: IdempotencyRetryInterceptor,
            logWriter: LogWriter,
        ): RestClient {
            return RestClient.Builder(
                okHttpClientProvider::get,
                TerminalMessageRedactor,
                logWriter,
            ) { Environment.Prod.stripeApiUrl }
                .addCustomRestInterceptor(idempotencyRetryInterceptor)
                .build()
        }

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
                    appScope = CoroutineScope(ioDispatcher),
                    rootBeer = RootBeer(context),
                    sdkInt = sdkInt,
                    supportedAbis = listOf(),
                    logger = loggerFactory.create(CotsClient::class)
                ),
                logger = loggerFactory.create(CotsClient::class)
            )
        }

        @Singleton
        @Provides
        fun providesStripeApiTokenProvider(): StripeApiTokenProvider = StripeApiTokenProvider
    }
}

@Module(includes = [SdkPlatformDeviceInfoModule.Bindings::class])
internal object SdkPlatformDeviceInfoModule {
    @Provides
    @Singleton
    internal fun provideCotsHardwareProvider(
        buildValues: BuildValues,
    ): CotsHardwareProvider = AndroidSdkCotsHardwareProvider(buildValues)

    @Provides
    @Singleton
    internal fun provideReaderPlatformDeviceInfo(
        statusManager: TerminalStatusManager,
        cotsHardwareProvider: CotsHardwareProvider,
    ): ReaderPlatformDeviceInfo = ReaderPlatformDeviceInfo(statusManager, cotsHardwareProvider,)

    @Module
    interface Bindings {
        @Binds
        fun providePlatformDeviceInfo(
            readerPlatformDeviceInfo: ReaderPlatformDeviceInfo
        ): PlatformDeviceInfo

        @Binds
        fun provideEmbeddedSoftwareVersionProvider(
            readerPlatformDeviceInfo: ReaderPlatformDeviceInfo
        ): EmbeddedSoftwareVersionProvider
    }
}

@Module(includes = [TerminalSessionModule.Bindings::class])
internal object TerminalSessionModule {
    @Provides
    fun provideDefaultIpReaderActivator(
        backgroundReaderActivator: BackgroundReaderActivator
    ): DefaultIpReaderActivator = DefaultIpReaderActivator(backgroundReaderActivator)

    @Provides
    fun provideDefaultBackgroundReaderActivator(
        terminalStatusManager: TerminalStatusManager,
        sessionTokenRepository: SessionTokenRepository,
        terminalSessionReaderActivator: dagger.Lazy<TerminalSessionReaderActivator>,
        loggerFactory: LoggerFactory,
    ): DefaultBackgroundReaderActivator = DefaultBackgroundReaderActivator(
        statusManager = terminalStatusManager,
        sessionTokenRepository = sessionTokenRepository,
        terminalSessionActivatorLazy = lazy { terminalSessionReaderActivator.get() },
        loggerFactory = loggerFactory,
    )

    @Provides
    @Singleton
    fun provideDefaultTerminalSessionReaderActivator(
        resourceRepository: ResourceRepository,
        readerActivationListener: ReaderActivationListener,
        transactionRepository: TransactionRepository,
        adapter: Adapter,
        loggerFactory: LoggerFactory,
    ): DefaultTerminalSessionReaderActivator = DefaultTerminalSessionReaderActivator(
        resourceRepository = resourceRepository,
        readerActivationListener = readerActivationListener,
        transactionRepository = transactionRepository,
        offlineReaderSetup = object : OfflineReaderSetup {
            override fun onReaderActivated(
                accountId: String,
                reader: Reader,
                connectionConfiguration: ConnectionConfiguration
            ) {
                // Do nothing
            }
        },
        adapter = adapter,
        loggerFactory = loggerFactory,
    )

    @Provides
    @Singleton
    fun provideSessionTokenInterceptor(
        backgroundReaderActivator: BackgroundReaderActivator,
        sessionTokenRepository: SessionTokenRepository,
        loggerFactory: LoggerFactory
    ): SessionTokenInterceptor = SessionTokenInterceptor(
        backgroundActivator = backgroundReaderActivator,
        sessionTokenRepository = sessionTokenRepository,
        loggerFactory = loggerFactory,
    )

    @Module
    interface Bindings {
        @Binds
        fun bindIpReaderActivator(defaultIpReaderActivator: DefaultIpReaderActivator): ReaderActivator

        @Binds
        fun bindBackgroundReaderActivator(
            defaultBackgroundReaderActivator: DefaultBackgroundReaderActivator
        ): BackgroundReaderActivator

        @Binds
        fun bindTerminalSessionReaderActivator(
            defaultTerminalSessionReaderActivator: DefaultTerminalSessionReaderActivator
        ): TerminalSessionReaderActivator

        @Binds
        fun bindReaderActivationListener(sessionTokenInterceptor: SessionTokenInterceptor): ReaderActivationListener
    }
}
