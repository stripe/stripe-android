package com.stripe.android.customersheet

import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.IS_LIVE_MODE
import com.stripe.android.customersheet.data.CustomerSheetInitializationDataSource
import com.stripe.android.customersheet.data.CustomerSheetSession
import com.stripe.android.customersheet.util.CustomerSheetHacks
import com.stripe.android.customersheet.util.sortPaymentMethods
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.lpmfoundations.luxe.LpmRepository
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.model.PaymentMethod
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.financialconnections.IsFinancialConnectionsAvailable
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.model.validate
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal interface CustomerSheetLoader {
    suspend fun load(configuration: CustomerSheet.Configuration): Result<CustomerSheetState.Full>
}

internal class DefaultCustomerSheetLoader(
    @Named(IS_LIVE_MODE) private val isLiveModeProvider: () -> Boolean,
    private val googlePayRepositoryFactory: @JvmSuppressWildcards (GooglePayEnvironment) -> GooglePayRepository,
    private val isFinancialConnectionsAvailable: IsFinancialConnectionsAvailable,
    private val lpmRepository: LpmRepository,
    private val initializationDataSourceProvider: Deferred<CustomerSheetInitializationDataSource>,
    private val errorReporter: ErrorReporter,
    private val workContext: CoroutineContext
) : CustomerSheetLoader {

    @Inject constructor(
        @Named(IS_LIVE_MODE) isLiveModeProvider: () -> Boolean,
        googlePayRepositoryFactory: @JvmSuppressWildcards (GooglePayEnvironment) -> GooglePayRepository,
        isFinancialConnectionsAvailable: IsFinancialConnectionsAvailable,
        lpmRepository: LpmRepository,
        errorReporter: ErrorReporter,
        @IOContext workContext: CoroutineContext
    ) : this(
        isLiveModeProvider = isLiveModeProvider,
        googlePayRepositoryFactory = googlePayRepositoryFactory,
        isFinancialConnectionsAvailable = isFinancialConnectionsAvailable,
        lpmRepository = lpmRepository,
        initializationDataSourceProvider = CustomerSheetHacks.initializationDataSource,
        errorReporter = errorReporter,
        workContext = workContext,
    )

    override suspend fun load(
        configuration: CustomerSheet.Configuration
    ): Result<CustomerSheetState.Full> = workContext.runCatching {
        val initializationDataSource = retrieveInitializationDataSource().getOrThrow()
        var customerSheetSession = initializationDataSource.loadCustomerSheetSession().toResult().getOrThrow()

        val filteredPaymentMethods = customerSheetSession.paymentMethods.filter {
            PaymentSheetCardBrandFilter(configuration.cardBrandAcceptance).isAccepted(it)
        }
        customerSheetSession = customerSheetSession.copy(
            paymentMethods = filteredPaymentMethods
        )

        val metadata = createPaymentMethodMetadata(
            configuration = configuration,
            customerSheetSession = customerSheetSession,
        )

        createCustomerSheetState(
            customerSheetSession = customerSheetSession,
            metadata = metadata,
            configuration = configuration,
        )
    }

    private suspend fun retrieveInitializationDataSource(): Result<CustomerSheetInitializationDataSource> {
        return initializationDataSourceProvider.awaitAsResult(
            timeout = 5.seconds,
            error = {
                "Couldn't find an instance of InitializationDataSource. " +
                    "Are you instantiating CustomerSheet unconditionally in your app?"
            },
        ).onFailure {
            errorReporter.report(
                errorEvent = ErrorReporter.ExpectedErrorEvent.CUSTOMER_SHEET_ADAPTER_NOT_FOUND,
                stripeException = StripeException.create(it)
            )
        }
    }

    private suspend fun createPaymentMethodMetadata(
        configuration: CustomerSheet.Configuration,
        customerSheetSession: CustomerSheetSession,
    ): PaymentMethodMetadata {
        val elementsSession = customerSheetSession.elementsSession
        val sharedDataSpecs = lpmRepository.getSharedDataSpecs(
            stripeIntent = elementsSession.stripeIntent,
            serverLpmSpecs = elementsSession.paymentMethodSpecs,
        ).sharedDataSpecs

        val isGooglePayReadyAndEnabled = configuration.googlePayEnabled && googlePayRepositoryFactory(
            if (isLiveModeProvider()) GooglePayEnvironment.Production else GooglePayEnvironment.Test
        ).isReady().first()

        return PaymentMethodMetadata.create(
            elementsSession = elementsSession,
            configuration = configuration,
            paymentMethodSaveConsentBehavior = customerSheetSession.paymentMethodSaveConsentBehavior,
            sharedDataSpecs = sharedDataSpecs,
            isGooglePayReady = isGooglePayReadyAndEnabled,
            isFinancialConnectionsAvailable = isFinancialConnectionsAvailable
        )
    }

    private fun createCustomerSheetState(
        customerSheetSession: CustomerSheetSession,
        metadata: PaymentMethodMetadata,
        configuration: CustomerSheet.Configuration,
    ): CustomerSheetState.Full {
        val paymentMethods = customerSheetSession.paymentMethods

        val paymentSelection = customerSheetSession.savedSelection?.let { selection ->
            when (selection) {
                is SavedSelection.GooglePay -> PaymentSelection.GooglePay
                is SavedSelection.Link -> PaymentSelection.Link
                is SavedSelection.PaymentMethod -> {
                    paymentMethods.find { paymentMethod ->
                        paymentMethod.id == selection.id
                    }?.let {
                        PaymentSelection.Saved(it)
                    }
                }
                is SavedSelection.None -> null
            }
        }

        val sortedPaymentMethods = sortPaymentMethods(
            paymentMethods = customerSheetSession.paymentMethods,
            selection = paymentSelection as? PaymentSelection.Saved
        )

        val supportedPaymentMethods = metadata.sortedSupportedPaymentMethods()

        val validSupportedPaymentMethods = filterSupportedPaymentMethods(supportedPaymentMethods)

        return CustomerSheetState.Full(
            config = configuration,
            paymentMethodMetadata = metadata,
            supportedPaymentMethods = validSupportedPaymentMethods,
            customerPaymentMethods = sortedPaymentMethods,
            paymentSelection = paymentSelection,
            validationError = customerSheetSession.elementsSession.stripeIntent.validate(),
            customerPermissions = customerSheetSession.permissions,
        )
    }

    private fun filterSupportedPaymentMethods(
        supportedPaymentMethods: List<SupportedPaymentMethod>,
    ): List<SupportedPaymentMethod> {
        val supported = setOfNotNull(
            PaymentMethod.Type.Card.code,
            PaymentMethod.Type.USBankAccount.code
        )
        return supportedPaymentMethods.filter {
            supported.contains(it.code)
        }
    }
}

private suspend fun <T> Deferred<T>.awaitAsResult(
    timeout: Duration,
    error: () -> String,
): Result<T> {
    val result = withTimeoutOrNull(timeout) { await() }
    return if (result != null) {
        Result.success(result)
    } else {
        Result.failure(IllegalStateException(error()))
    }
}
