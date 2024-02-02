package com.stripe.android.lpmfoundations.luxe

import android.content.res.Resources
import androidx.annotation.RestrictTo
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LpmRepository(
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
        val expectedLpms = metadata.stripeIntent.paymentMethodTypes
        var failedToParseServerResponse = false

        if (!serverLpmSpecs.isNullOrEmpty()) {
            val deserializationResult = LpmSerializer.deserializeList(serverLpmSpecs)
            failedToParseServerResponse = deserializationResult.isFailure
            val serverLpmObjects = deserializationResult.getOrElse { emptyList() }
            update(
                metadata = metadata,
                specs = serverLpmObjects,
            )
        }

        // If the server does not return specs, or they are not parsed successfully
        // we will use the LPM on disk if found
        val lpmsNotParsedFromServerSpec = expectedLpms.filter { lpm ->
            !lpmInitialFormData.containsKey(lpm)
        }

        if (lpmsNotParsedFromServerSpec.isNotEmpty()) {
            parseMissingLpmsFromDisk(
                missingLpms = lpmsNotParsedFromServerSpec,
                metadata = metadata,
            )
        }

        return !failedToParseServerResponse
    }

    private fun parseMissingLpmsFromDisk(
        missingLpms: List<String>,
        metadata: PaymentMethodMetadata,
    ) {
        val missingSpecsOnDisk = readFromDisk().filter { it.type in missingLpms }

        val missingLpmsByType = missingSpecsOnDisk.mapNotNull { spec ->
            convertToSupportedPaymentMethod(
                metadata = metadata,
                sharedDataSpec = spec,
            )
        }.associateBy {
            it.code
        }

        lpmInitialFormData.putAll(missingLpmsByType)
    }

    /**
     * This method can be used to initialize the LpmRepository with a given map of payment methods.
     */
    fun initializeWithPaymentMethods(
        paymentMethods: Map<String, SupportedPaymentMethod>
    ) {
        lpmInitialFormData.putAll(paymentMethods)
    }

    private fun readFromDisk(): List<SharedDataSpec> {
        return parseLpms(arguments.resources.assets?.open("lpms.json"))
    }

    private fun update(
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
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        @Volatile
        private var INSTANCE: LpmRepository? = null
        fun getInstance(args: LpmRepositoryArguments): LpmRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LpmRepository(args).also { INSTANCE = it }
            }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class LpmRepositoryArguments(
        val resources: Resources,
    )
}
