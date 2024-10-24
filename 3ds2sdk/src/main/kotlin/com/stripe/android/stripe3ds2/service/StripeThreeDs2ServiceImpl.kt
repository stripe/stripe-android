package com.stripe.android.stripe3ds2.service

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.stripe.android.stripe3ds2.exceptions.InvalidInputException
import com.stripe.android.stripe3ds2.exceptions.SDKRuntimeException
import com.stripe.android.stripe3ds2.init.AppInfoRepository
import com.stripe.android.stripe3ds2.init.DefaultAppInfoRepository
import com.stripe.android.stripe3ds2.init.DefaultSecurityChecker
import com.stripe.android.stripe3ds2.init.DeviceDataFactoryImpl
import com.stripe.android.stripe3ds2.init.DeviceParamNotAvailableFactoryImpl
import com.stripe.android.stripe3ds2.init.SecurityChecker
import com.stripe.android.stripe3ds2.init.Warning
import com.stripe.android.stripe3ds2.init.ui.StripeUiCustomization
import com.stripe.android.stripe3ds2.observability.DefaultErrorReporter
import com.stripe.android.stripe3ds2.observability.ErrorReporter
import com.stripe.android.stripe3ds2.security.EphemeralKeyPairGenerator
import com.stripe.android.stripe3ds2.security.PublicKeyFactory
import com.stripe.android.stripe3ds2.security.StripeEphemeralKeyPairGenerator
import com.stripe.android.stripe3ds2.transaction.DefaultAuthenticationRequestParametersFactory
import com.stripe.android.stripe3ds2.transaction.DefaultTransactionFactory
import com.stripe.android.stripe3ds2.transaction.Logger
import com.stripe.android.stripe3ds2.transaction.MessageVersionRegistry
import com.stripe.android.stripe3ds2.transaction.SdkTransactionId
import com.stripe.android.stripe3ds2.transaction.Transaction
import com.stripe.android.stripe3ds2.transaction.TransactionFactory
import com.stripe.android.stripe3ds2.utils.AnalyticsDelegate
import com.stripe.android.stripe3ds2.utils.ImageCache
import com.stripe.android.stripe3ds2.views.Brand
import java.security.PublicKey
import java.security.cert.X509Certificate
import java.util.ServiceLoader
import kotlin.coroutines.CoroutineContext

class StripeThreeDs2ServiceImpl @VisibleForTesting internal constructor(
    private val messageVersionRegistry: MessageVersionRegistry,
    private val imageCache: ImageCache,
    private val errorReporter: ErrorReporter,
    private val transactionFactory: TransactionFactory,
    private val publicKeyFactory: PublicKeyFactory,
    override val warnings: List<Warning>,
) : StripeThreeDs2Service {

    @JvmOverloads
    constructor(
        context: Context,
        enableLogging: Boolean = false,
        workContext: CoroutineContext
    ) : this(
        context,
        STRIPE_SDK_REFERENCE_NUMBER,
        enableLogging,
        workContext
    )

    constructor(
        context: Context,
        sdkReferenceNumber: String,
        enableLogging: Boolean = false,
        workContext: CoroutineContext
    ) : this(
        context,
        ImageCache.Default,
        sdkReferenceNumber,
        enableLogging,
        workContext = workContext
    )

    private constructor(
        context: Context,
        imageCache: ImageCache,
        sdkReferenceNumber: String,
        enableLogging: Boolean,
        workContext: CoroutineContext
    ) : this(
        context,
        imageCache,
        sdkReferenceNumber,
        // TODO(mshafrir): add config
        DefaultErrorReporter(
            context = context.applicationContext,
            logger = Logger.get(enableLogging)
        ),
        workContext
    )

    private constructor(
        context: Context,
        imageCache: ImageCache,
        sdkReferenceNumber: String,
        errorReporter: ErrorReporter,
        workContext: CoroutineContext
    ) : this(
        context,
        imageCache,
        sdkReferenceNumber,
        errorReporter,
        StripeEphemeralKeyPairGenerator(errorReporter),
        DefaultSecurityChecker(),
        MessageVersionRegistry(),
        DefaultAppInfoRepository(context, workContext),
        workContext
    )

    private constructor(
        context: Context,
        imageCache: ImageCache,
        sdkReferenceNumber: String,
        errorReporter: ErrorReporter,
        ephemeralKeyPairGenerator: EphemeralKeyPairGenerator,
        securityChecker: SecurityChecker,
        messageVersionRegistry: MessageVersionRegistry,
        appInfoRepository: AppInfoRepository,
        workContext: CoroutineContext
    ) : this(
        messageVersionRegistry = messageVersionRegistry,
        imageCache = imageCache,
        errorReporter = errorReporter,
        transactionFactory = DefaultTransactionFactory(
            DefaultAuthenticationRequestParametersFactory(
                DeviceDataFactoryImpl(
                    context = context.applicationContext,
                    appInfoRepository = appInfoRepository,
                    messageVersionRegistry = messageVersionRegistry
                ),
                DeviceParamNotAvailableFactoryImpl(),
                securityChecker,
                ephemeralKeyPairGenerator,
                appInfoRepository,
                messageVersionRegistry,
                sdkReferenceNumber,
                errorReporter,
                workContext
            ),
            ephemeralKeyPairGenerator,
            sdkReferenceNumber,
        ),
        warnings = securityChecker.getWarnings(),
        publicKeyFactory = PublicKeyFactory(context, errorReporter)
    )

    @Throws(InvalidInputException::class, SDKRuntimeException::class)
    override fun createTransaction(
        sdkTransactionId: SdkTransactionId,
        directoryServerID: String,
        messageVersion: String?,
        isLiveMode: Boolean,
        directoryServerName: String,
        rootCerts: List<X509Certificate>,
        dsPublicKey: PublicKey,
        keyId: String?,
        uiCustomization: StripeUiCustomization
    ): Transaction {
        return createTransaction(
            directoryServerID,
            messageVersion,
            isLiveMode,
            directoryServerName,
            rootCerts,
            dsPublicKey,
            keyId,
            sdkTransactionId = sdkTransactionId
        )
    }

    private fun createTransaction(
        directoryServerID: String,
        messageVersion: String?,
        isLiveMode: Boolean,
        directoryServerName: String,
        rootCerts: List<X509Certificate>,
        dsPublicKey: PublicKey,
        keyId: String?,
        sdkTransactionId: SdkTransactionId
    ): Transaction {
        if (!messageVersionRegistry.isSupported(messageVersion)) {
            throw InvalidInputException("Message version is unsupported: ${messageVersion.orEmpty()}")
        }

        return transactionFactory.create(
            directoryServerID,
            rootCerts,
            dsPublicKey,
            keyId,
            sdkTransactionId,
            isLiveMode,
            Brand.lookup(
                directoryServerName,
                errorReporter
            )
        )
    }

    override fun cleanup() {
        imageCache.clear()
    }

    override fun getPublicKey(directoryServerId: String): PublicKey {
        return publicKeyFactory.create(directoryServerId)
    }

    private companion object {
        private const val STRIPE_SDK_REFERENCE_NUMBER = "3DS_LOA_SDK_STIN_020100_00142"
    }
}

class AnalyticsProvider private constructor() {
    private val loader: ServiceLoader<AnalyticsDelegate> = ServiceLoader.load(AnalyticsDelegate::class.java)

    fun serviceImpl(): AnalyticsDelegate? {
        if (loader.iterator().hasNext()) {
            return loader.iterator().next()
        }

        return null
    }

    companion object {
        private var provider: AnalyticsProvider? = null

        val instance: AnalyticsProvider
            get() {
                val provider = this.provider ?: AnalyticsProvider()

                if (this.provider ==  null) {
                    this.provider = provider
                }

                return provider
            }
    }
}
