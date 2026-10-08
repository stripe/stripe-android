package com.stripe.android.core.frauddetection

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.core.networking.responseJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.coroutines.CoroutineContext

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface FraudDetectionDataRepository {
    fun refresh(publishableKey: String, stripeAccountId: String?)

    /**
     * Read locally stored [FraudDetectionData] for these credentials without making a network request.
     */
    suspend fun getCached(publishableKey: String, stripeAccountId: String?): FraudDetectionData?

    /**
     * Get the latest [FraudDetectionData]. This is a blocking request.
     *
     * 1. From [FraudDetectionDataStore] if that value is not expired.
     * 2. Otherwise, from the network.
     */
    suspend fun getLatest(publishableKey: String, stripeAccountId: String?): FraudDetectionData?

    fun save(publishableKey: String, stripeAccountId: String?, fraudDetectionData: FraudDetectionData)
}

private val timestampSupplier: () -> Long = {
    Calendar.getInstance().timeInMillis
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface FraudDetectionErrorReporter {
    fun reportFraudDetectionError(error: StripeException, publishableKey: String)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface FraudDetectionEnabledProvider {
    fun provideFraudDetectionEnabled(): Boolean
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultFraudDetectionDataRepository(
    private val localStore: FraudDetectionDataStore,
    private val fraudDetectionDataRequestFactory: FraudDetectionDataRequestFactory,
    private val stripeNetworkClient: StripeNetworkClient,
    private val errorReporter: FraudDetectionErrorReporter,
    private val workContext: CoroutineContext,
    private val fraudDetectionEnabledProvider: FraudDetectionEnabledProvider,
) : FraudDetectionDataRepository {
    private val fraudDetectionEnabled: Boolean
        get() = fraudDetectionEnabledProvider.provideFraudDetectionEnabled()

    override fun refresh(publishableKey: String, stripeAccountId: String?) {
        if (fraudDetectionEnabled) {
            CoroutineScope(workContext).launch {
                getLatest(publishableKey, stripeAccountId)
            }
        }
    }

    override suspend fun getLatest(publishableKey: String, stripeAccountId: String?) = withContext(workContext) {
        localStore.get(publishableKey, stripeAccountId).let { localFraudDetectionData ->
            if (localFraudDetectionData == null ||
                localFraudDetectionData.isExpired(timestampSupplier())
            ) {
                // fraud detection data request failures should be non-fatal
                runCatching {
                    stripeNetworkClient.executeRequest(
                        fraudDetectionDataRequestFactory.create(
                            localFraudDetectionData
                        )
                    ).fraudDetectionData()
                }.onFailure {
                    val error = StripeException.create(it)
                    errorReporter.reportFraudDetectionError(error, publishableKey)
                }.getOrNull()?.also { save(publishableKey, stripeAccountId, it) }
            } else {
                localFraudDetectionData
            }
        }
    }

    override suspend fun getCached(publishableKey: String, stripeAccountId: String?): FraudDetectionData? {
        return if (fraudDetectionEnabled) localStore.get(publishableKey, stripeAccountId) else null
    }

    override fun save(publishableKey: String, stripeAccountId: String?, fraudDetectionData: FraudDetectionData) {
        localStore.save(publishableKey, stripeAccountId, fraudDetectionData)
    }
}

private val fraudDetectionJsonParser = FraudDetectionDataJsonParser(timestampSupplier)

/**
 * Internal extension to convert the [String] body of [StripeResponse] to a [FraudDetectionData].
 */
private fun StripeResponse<String>.fraudDetectionData(): FraudDetectionData? =
    takeIf { isOk }?.let { fraudDetectionJsonParser.parse(it.responseJson()) }
