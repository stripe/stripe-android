package com.stripe.android.paymentsheet.featuredinstitutions

import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.networking.toRequestOptions
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.uicore.image.StripeImageLoader
import dagger.Binds
import dagger.Module
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

internal interface FeaturedInstitutionsHelper {
    /**
     * The featured institutions of the most recent load, or null if they aren't shown for that load.
     */
    val featuredInstitutions: FeaturedInstitutions?

    /**
     * Starts fetching the featured institutions without waiting for the result.
     */
    fun fetchAsync(
        elementsSession: ElementsSession,
        paymentMethodMetadata: PaymentMethodMetadata,
        apiConfiguration: ApiConfiguration.State,
    )
}

@Singleton
internal class DefaultFeaturedInstitutionsHelper @Inject constructor(
    private val featuredInstitutionsRepository: FeaturedInstitutionsRepository,
    private val imageLoader: StripeImageLoader,
    private val eventReporter: EventReporter,
    @ViewModelScope private val viewModelScope: CoroutineScope,
    @IOContext private val workContext: CoroutineContext,
) : FeaturedInstitutionsHelper {
    @Volatile
    override var featuredInstitutions: FeaturedInstitutions? = null
        private set

    private var fetchJob: Job? = null

    override fun fetchAsync(
        elementsSession: ElementsSession,
        paymentMethodMetadata: PaymentMethodMetadata,
        apiConfiguration: ApiConfiguration.State,
    ) {
        fetchJob?.cancel()
        fetchJob = null
        featuredInstitutions = null

        val hasBankRow = PaymentMethod.Type.Link.code in paymentMethodMetadata.supportedPaymentMethodTypes()
        if (!elementsSession.isFeaturedInstitutionsEnabled || !hasBankRow) {
            return
        }

        val featuredInstitutions = FeaturedInstitutions(onShown = eventReporter::onFeaturedInstitutionsShown)
        this.featuredInstitutions = featuredInstitutions

        fetchJob = viewModelScope.launch(workContext) {
            featuredInstitutionsRepository.retrieveFeaturedInstitutions(
                requestOptions = apiConfiguration.toRequestOptions(idempotencyKey = null),
            ).onSuccess { institutionList ->
                featuredInstitutions.loadIcons(
                    institutions = institutionList.data,
                    imageLoader = imageLoader,
                    onInsufficientIcons = eventReporter::onFeaturedInstitutionsInsufficient,
                )
            }.onFailure { error ->
                // Decorative icons failing to load (e.g. offline) is expected, so this isn't an unexpected error.
                eventReporter.onFeaturedInstitutionsLoadFailed(error)
            }
        }
    }
}

/**
 * Shows institutions that were already fetched by another screen's load, e.g. FlowController's payment options.
 */
internal class PrefetchedFeaturedInstitutionsHelper(
    institutions: List<FeaturedInstitution>?,
    imageLoader: StripeImageLoader,
    eventReporter: EventReporter,
    viewModelScope: CoroutineScope,
    workContext: CoroutineContext,
) : FeaturedInstitutionsHelper {
    override val featuredInstitutions: FeaturedInstitutions? = institutions?.let {
        FeaturedInstitutions(onShown = eventReporter::onFeaturedInstitutionsShown)
    }

    init {
        if (institutions != null && featuredInstitutions != null) {
            viewModelScope.launch(workContext) {
                // The original load already reported its outcome, and these icons are usually in the image cache.
                featuredInstitutions.loadIcons(
                    institutions = institutions,
                    imageLoader = imageLoader,
                    onInsufficientIcons = {},
                )
            }
        }
    }

    override fun fetchAsync(
        elementsSession: ElementsSession,
        paymentMethodMetadata: PaymentMethodMetadata,
        apiConfiguration: ApiConfiguration.State,
    ) {
        // NO-OP
    }
}

internal class NoOpFeaturedInstitutionsHelper @Inject constructor() : FeaturedInstitutionsHelper {
    override val featuredInstitutions: FeaturedInstitutions? = null

    override fun fetchAsync(
        elementsSession: ElementsSession,
        paymentMethodMetadata: PaymentMethodMetadata,
        apiConfiguration: ApiConfiguration.State,
    ) {
        // NO-OP
    }
}

@Module
internal interface FeaturedInstitutionsHelperModule {
    @Binds
    fun bindsFeaturedInstitutionsHelper(impl: DefaultFeaturedInstitutionsHelper): FeaturedInstitutionsHelper

    @Binds
    fun bindsFeaturedInstitutionsRepository(
        impl: DefaultFeaturedInstitutionsRepository
    ): FeaturedInstitutionsRepository
}

@Module
internal interface NoOpFeaturedInstitutionsHelperModule {
    @Binds
    fun bindsFeaturedInstitutionsHelper(impl: NoOpFeaturedInstitutionsHelper): FeaturedInstitutionsHelper
}
