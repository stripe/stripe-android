package com.stripe.android.lpmfoundations.luxe

import android.content.res.Resources
import androidx.annotation.VisibleForTesting
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodRegistry
import com.stripe.android.lpmfoundations.paymentmethod.isSupported
import com.stripe.android.model.LuxePostConfirmActionRepository
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.StripeIntent
import com.stripe.android.ui.core.elements.LpmSerializer
import com.stripe.android.ui.core.elements.SharedDataSpec
import com.stripe.android.ui.core.elements.transform
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class is responsible for loading the LPM UI Specification for all LPMs, and returning
 * a particular requested LPM.
 *
 * This is not injected as a singleton because when the activity is killed
 * the FormViewModel and SheetViewModel don't share the Dagger graph and the
 * repository is not a singleton.  Additionally every time you create a new
 * form view model a new repository is created and thus needs to be initialized.
 */
@Singleton
internal class LpmRepository(
    private val arguments: LpmRepositoryArguments,
    private val lpmInitialFormData: LpmInitialFormData = LpmInitialFormData.Instance,
    private val lpmPostConfirmData: LuxePostConfirmActionRepository = LuxePostConfirmActionRepository.Instance,
) {

    @Inject
    constructor(resources: Resources) : this(
        arguments = LpmRepositoryArguments(resources),
    )

    fun fromCode(code: PaymentMethodCode?) = lpmInitialFormData.fromCode(code)

    fun values(): List<SupportedPaymentMethod> = lpmInitialFormData.values()

    /**
     * This method will read the [StripeIntent] and their specs as two separate parameters.
     * Any spec not found from the server will be read from disk json file.
     *
     * It is still possible that an lpm that is expected cannot be read successfully from
     * the json spec on disk or the server spec.
     *
     * It is also possible that an LPM is present in the repository that is not present
     * in the expected LPM list.
     *
     * Reading the server spec is all or nothing, any error means none will be read
     * so it is important that the json on disk is successful.
     */
    fun update(
        metadata: PaymentMethodMetadata,
        serverLpmSpecs: String?,
    ): Boolean {
        val sharedDataSpecsResult: Result = getSharedDataSpecs(metadata, serverLpmSpecs)

        update(
            metadata = metadata,
            specs = sharedDataSpecsResult.sharedDataSpecs,
        )

        return !sharedDataSpecsResult.failedToParseServerResponse
    }

    fun getSharedDataSpecs(
        metadata: PaymentMethodMetadata,
        serverLpmSpecs: String?,
    ): Result {
        val expectedLpms = metadata.stripeIntent.paymentMethodTypes
        var failedToParseServerResponse = false

        val sharedDataSpecs: MutableList<SharedDataSpec> = mutableListOf()

        if (!serverLpmSpecs.isNullOrEmpty()) {
            val deserializationResult = LpmSerializer.deserializeList(serverLpmSpecs)
            failedToParseServerResponse = deserializationResult.isFailure
            sharedDataSpecs += deserializationResult.getOrElse { emptyList() }
        }

        // If the server does not return specs, or they are not parsed successfully
        // we will use the LPM on disk if found
        val sharedDataSpecTypes = sharedDataSpecs.map { it.type }.toSet()
        val lpmsNotParsedFromServerSpec = expectedLpms.filter { lpm ->
            lpm !in sharedDataSpecTypes
        }

        if (lpmsNotParsedFromServerSpec.isNotEmpty()) {
            sharedDataSpecs += readFromDisk().filter { it.type in lpmsNotParsedFromServerSpec }
        }

        return Result(
            sharedDataSpecs = sharedDataSpecs,
            failedToParseServerResponse = failedToParseServerResponse,
        )
    }

    private fun readFromDisk(): List<SharedDataSpec> {
        return parseLpms(arguments.resources.assets?.open("lpms.json"))
    }

    fun update(
        metadata: PaymentMethodMetadata,
        specs: List<SharedDataSpec>,
    ) {
        // By mapNotNull we will not accept any LPMs that are not known by the platform.
        val supportedPaymentMethods = specs.mapNotNull { spec ->
            convertToSupportedPaymentMethod(
                metadata = metadata,
                sharedDataSpec = spec,
            )
        }

        val supportedPaymentMethodsByType = supportedPaymentMethods.associateBy { it.code }
        lpmInitialFormData.putAll(supportedPaymentMethodsByType)

        val nextActionSpecsByType = specs.associate { spec ->
            spec.type to spec.nextActionSpec.transform()
        }
        lpmPostConfirmData.update(nextActionSpecsByType)
    }

    private fun parseLpms(inputStream: InputStream?): List<SharedDataSpec> {
        return getJsonStringFromInputStream(inputStream)?.let { string ->
            LpmSerializer.deserializeList(string).getOrElse { emptyList() }
        }.orEmpty()
    }

    private fun getJsonStringFromInputStream(inputStream: InputStream?) =
        inputStream?.bufferedReader().use { it?.readText() }

    private fun convertToSupportedPaymentMethod(
        metadata: PaymentMethodMetadata,
        sharedDataSpec: SharedDataSpec,
    ): SupportedPaymentMethod? {
        val paymentMethodDefinition = PaymentMethodRegistry.definitionsByCode[sharedDataSpec.type]

        if (paymentMethodDefinition?.isSupported(metadata) == true) {
            return paymentMethodDefinition.supportedPaymentMethod(metadata, sharedDataSpec)
        }

        return null
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    class LpmInitialFormData {

        private val codeToSupportedPaymentMethod = mutableMapOf<String, SupportedPaymentMethod>()

        fun values(): List<SupportedPaymentMethod> = codeToSupportedPaymentMethod.values.toList()

        fun fromCode(code: String?) = code?.let { paymentMethodCode ->
            codeToSupportedPaymentMethod[paymentMethodCode]
        }

        fun containsKey(it: String) = codeToSupportedPaymentMethod.containsKey(it)

        fun putAll(map: Map<PaymentMethodCode, SupportedPaymentMethod>) {
            codeToSupportedPaymentMethod.putAll(map)
        }

        internal companion object {
            val Instance = LpmInitialFormData()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: LpmRepository? = null
        fun getInstance(args: LpmRepositoryArguments): LpmRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LpmRepository(args).also { INSTANCE = it }
            }
    }

    data class LpmRepositoryArguments(
        val resources: Resources,
    )

    data class Result(
        val sharedDataSpecs: List<SharedDataSpec>,
        val failedToParseServerResponse: Boolean,
    )
}
