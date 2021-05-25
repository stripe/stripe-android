package com.stripe.android

import android.content.Context
import com.stripe.android.networking.DefaultFraudDetectionDataRequestExecutor
import com.stripe.android.networking.DefaultFraudDetectionDataRequestFactory
import com.stripe.android.networking.FraudDetectionData
import com.stripe.android.networking.FraudDetectionDataRequestExecutor
import com.stripe.android.networking.FraudDetectionDataRequestFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.coroutines.CoroutineContext

internal interface FraudDetectionDataRepository {
    fun refresh()

    /**
     * Get the cached [FraudDetectionData]. This is not a blocking request.
     */
    fun getCached(): FraudDetectionData?

    /**
     * Get the latest [FraudDetectionData]. This is a blocking request.
     *
     * 1. From [FraudDetectionDataStore] if that value is not expired.
     * 2. Otherwise, from the network.
     */
    suspend fun getLatest(): FraudDetectionData?

    fun save(fraudDetectionData: FraudDetectionData)
}

internal class DefaultFraudDetectionDataRepository(
    private val localStore: FraudDetectionDataStore,
    private val fraudDetectionDataRequestFactory: FraudDetectionDataRequestFactory,
    private val fraudDetectionDataRequestExecutor: FraudDetectionDataRequestExecutor,
    private val workContext: CoroutineContext
) : FraudDetectionDataRepository {
    private var cachedFraudDetectionData: FraudDetectionData? = null

    private val timestampSupplier: () -> Long = {
        Calendar.getInstance().timeInMillis
    }

    @JvmOverloads
    constructor(
        context: Context,
        workContext: CoroutineContext = Dispatchers.IO
    ) : this(
        localStore = DefaultFraudDetectionDataStore(context, workContext),
        fraudDetectionDataRequestFactory = DefaultFraudDetectionDataRequestFactory(context),
        fraudDetectionDataRequestExecutor = DefaultFraudDetectionDataRequestExecutor(
            workContext = workContext
        ),
        workContext = workContext
    )

    override fun refresh() {
        if (Stripe.advancedFraudSignalsEnabled) {
            CoroutineScope(workContext).launch {
                getLatest()
            }
        }
    }

    override suspend fun getLatest() = withContext(workContext) {
        val latestFraudDetectionData = localStore.get().let { localFraudDetectionData ->
            if (localFraudDetectionData == null ||
                localFraudDetectionData.isExpired(timestampSupplier())
            ) {
                fraudDetectionDataRequestExecutor.execute(
                    request = fraudDetectionDataRequestFactory.create(
                        localFraudDetectionData
                    )
                )
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
            Stripe.advancedFraudSignalsEnabled
        }
    }

    override fun save(fraudDetectionData: FraudDetectionData) {
        cachedFraudDetectionData = fraudDetectionData
        localStore.save(fraudDetectionData)
    }
}
