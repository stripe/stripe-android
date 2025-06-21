package com.stripe.onramp.repositories

import androidx.annotation.RestrictTo
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.networking.ApiRequest
import com.stripe.onramp.model.KycInfo
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

/**
 * Repository interface for crypto-related operations.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface CryptoRepository {
    /**
     * Submit KYC information for verification.
     *
     * @param kycInfo The KYC information to submit.
     * @return Result indicating success or failure of the KYC submission.
     */
    suspend fun submitKycInfo(kycInfo: KycInfo): kotlin.Result<Unit>
}

/**
 * Repository that uses [CryptoApiService] for crypto-related services.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CryptoApiRepository @Inject constructor(
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
    @Named(STRIPE_ACCOUNT_ID) private val stripeAccountIdProvider: () -> String?,
    private val cryptoApiService: CryptoApiService,
    @IOContext private val workContext: CoroutineContext,
) : CryptoRepository {

    override suspend fun submitKycInfo(kycInfo: KycInfo): kotlin.Result<Unit> =
        withContext(workContext) {
            cryptoApiService.submitKycInfo(
                kycInfo = kycInfo,
                requestOptions = buildRequestOptions()
            ).map { Unit } // Map KycSubmissionResponse to Unit
        }

    private fun buildRequestOptions(): ApiRequest.Options {
        return ApiRequest.Options(
            apiKey = publishableKeyProvider(),
            stripeAccount = stripeAccountIdProvider(),
        )
    }
}
