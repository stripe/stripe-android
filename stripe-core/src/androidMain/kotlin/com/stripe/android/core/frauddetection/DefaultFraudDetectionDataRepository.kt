package com.stripe.android.core.frauddetection

import androidx.annotation.RestrictTo
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.coroutines.CoroutineContext

private val timestampSupplier: () -> Long = {
    Calendar.getInstance().timeInMillis
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
    private var cachedFraudDetectionData: FraudDetectionData? = null

    private val fraudDetectionEnabled: Boolean
        get() = fraudDetectionEnabledProvider.provideFraudDetectionEnabled()

    override fun refresh() {
        if (fraudDetectionEnabled) {
            CoroutineScope(workContext).launch {
                getLatest()
            }
        }
    }

    override suspend fun getLatest() = withContext(workContext) {
        val latestFraudDetectionData = localStore.get().let { localFraudDetectionData ->
            if (localFraudDetectionData == null || localFraudDetectionData.isExpired(timestampSupplier())) {
                // fraud detection data request failures should be non-fatal
                runCatching {
                    stripeNetworkClient.executeRequest(
                        fraudDetectionDataRequestFactory.create(
                            localFraudDetectionData
                        )
                    ).fraudDetectionData()
                }.onFailure {
                    errorReporter.reportFraudDetectionError(it)
                }.getOrNull()
            } else {
                localFraudDetectionData
            }
        }

        if (cachedFraudDetectionData != latestFraudDetectionData) {
            latestFraudDetectionData?.let(::save)
        }

        latestFraudDetectionData
    }

    override fun getCached(): FraudDetectionData? {
        return cachedFraudDetectionData.takeIf {
            fraudDetectionEnabled
        }
    }

    override fun save(fraudDetectionData: FraudDetectionData) {
        cachedFraudDetectionData = fraudDetectionData
        localStore.save(fraudDetectionData)
    }
}

private val fraudDetectionJsonParser = FraudDetectionDataJsonParser(timestampSupplier)

/**
 * Internal extension to convert the [String] body of [StripeResponse] to a [FraudDetectionData].
 */
private fun StripeResponse<String>.fraudDetectionData(): FraudDetectionData? =
    takeIf { isOk }?.let { fraudDetectionJsonParser.parse(it.body ?: "{}") }
