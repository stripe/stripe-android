package com.stripe.android.connect.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.stripe.android.connect.AccountOnboardingProps
import com.stripe.android.connect.PaymentsProps
import com.stripe.android.connect.PreviewConnectSDK
import com.stripe.android.connect.example.ui.appearance.AppearanceInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooManyFunctions")
class SettingsService @Inject constructor(@ApplicationContext context: Context) {

    /**
     * Keep a strong reference to change listeners else they'll be garbage collected.
     * See docs for [SharedPreferences.registerOnSharedPreferenceChangeListener].
     */
    private val changeListeners =
        mutableListOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("SettingsService", Context.MODE_PRIVATE)
    }

    fun getSelectedServerBaseURL(): String? {
        return sharedPreferences.getString(SERVER_BASE_URL_KEY, null)
    }

    fun setSelectedServerBaseURL(value: String) {
        sharedPreferences.edit { putString(SERVER_BASE_URL_KEY, value) }
    }

    fun getAppearanceId(): AppearanceInfo.AppearanceId? {
        val appearanceId = sharedPreferences.getString(APPEARANCE_ID_KEY, null) ?: return null
        return AppearanceInfo.AppearanceId.entries.firstOrNull { it.name == appearanceId }
    }

    fun getAppearanceIdFlow(): Flow<AppearanceInfo.AppearanceId?> {
        return observePref(APPEARANCE_ID_KEY) { prefs, key ->
            val appearanceId = prefs.getString(key, null) ?: return@observePref null
            AppearanceInfo.AppearanceId.entries.firstOrNull { it.name == appearanceId }
        }
    }

    fun setAppearanceId(value: AppearanceInfo.AppearanceId?) {
        sharedPreferences.edit { putString(APPEARANCE_ID_KEY, value?.name) }
    }

    fun getOnboardingSettings(): OnboardingSettings {
        return OnboardingSettings(
            fullTermsOfServiceString = sharedPreferences.getString(ONBOARDING_TERMS_OF_SERVICE_URL, null),
            recipientTermsOfServiceString = sharedPreferences.getString(
                ONBOARDING_RECIPIENT_TERMS_OF_SERVICE_STRING,
                null,
            ),
            privacyPolicyString = sharedPreferences.getString(ONBOARDING_PRIVACY_POLICY_STRING, null),
            skipTermsOfService = SkipTermsOfService.valueOf(
                sharedPreferences.getString(ONBOARDING_SKIP_TERMS_OF_SERVICE, SkipTermsOfService.DEFAULT.name)
                    ?: SkipTermsOfService.DEFAULT.name,
            ),
            fieldOption = FieldOption.valueOf(
                sharedPreferences.getString(ONBOARDING_FIELD_OPTION, FieldOption.DEFAULT.name)
                    ?: FieldOption.DEFAULT.name,
            ),
            futureRequirement = FutureRequirement.valueOf(
                sharedPreferences.getString(ONBOARDING_FUTURE_REQUIREMENTS, FutureRequirement.DEFAULT.name)
                    ?: FutureRequirement.DEFAULT.name,
            ),
            requirementsMode = RequirementsMode.valueOf(
                sharedPreferences.getString(ONBOARDING_REQUIREMENTS_MODE, RequirementsMode.DEFAULT.name)
                    ?: RequirementsMode.DEFAULT.name,
            ),
            requirementsText = sharedPreferences.getString(ONBOARDING_REQUIREMENTS_TEXT, null),
        )
    }

    fun setOnboardingSettings(value: OnboardingSettings) {
        sharedPreferences.edit {
            putString(ONBOARDING_TERMS_OF_SERVICE_URL, value.fullTermsOfServiceString)
            putString(ONBOARDING_RECIPIENT_TERMS_OF_SERVICE_STRING, value.recipientTermsOfServiceString)
            putString(ONBOARDING_PRIVACY_POLICY_STRING, value.privacyPolicyString)
            putString(ONBOARDING_SKIP_TERMS_OF_SERVICE, value.skipTermsOfService.name)
            putString(ONBOARDING_FIELD_OPTION, value.fieldOption.name)
            putString(ONBOARDING_FUTURE_REQUIREMENTS, value.futureRequirement.name)
            putString(ONBOARDING_REQUIREMENTS_MODE, value.requirementsMode.name)
            putString(ONBOARDING_REQUIREMENTS_TEXT, value.requirementsText)
        }
    }

    fun getPresentationSettings(): PresentationSettings {
        return PresentationSettings(
            presentationStyleIsPush = !sharedPreferences.getBoolean(PRESENTATION_IS_MODAL, false),
            embedInTabBar = sharedPreferences.getBoolean(EMBED_IN_TABBAR, false),
            embedInNavBar = !sharedPreferences.getBoolean(DISABLE_EMBED_IN_NAVBAR, false),
            enableEdgeToEdge = sharedPreferences.getBoolean(ENABLE_EDGE_TO_EDGE, false)
        )
    }

    fun setPresentationSettings(value: PresentationSettings) {
        sharedPreferences.edit {
            putBoolean(PRESENTATION_IS_MODAL, !value.presentationStyleIsPush)
            putBoolean(EMBED_IN_TABBAR, value.embedInTabBar)
            putBoolean(DISABLE_EMBED_IN_NAVBAR, !value.embedInNavBar)
            putBoolean(ENABLE_EDGE_TO_EDGE, value.enableEdgeToEdge)
        }
    }

    @OptIn(PreviewConnectSDK::class)
    fun getPaymentsSettings(): PaymentsSettings {
        return PaymentsSettings(
            amountFilterType = AmountFilterType.valueOf(
                sharedPreferences.getString(PAYMENTS_AMOUNT_FILTER_TYPE, AmountFilterType.NONE.name)
                    ?: AmountFilterType.NONE.name
            ),
            amountValue = sharedPreferences.getString(PAYMENTS_AMOUNT_VALUE, "0.0")?.toDoubleOrNull() ?: 0.0,
            amountLowerBound = sharedPreferences.getString(PAYMENTS_AMOUNT_LOWER_BOUND, "0.0")?.toDoubleOrNull() ?: 0.0,
            amountUpperBound = sharedPreferences.getString(PAYMENTS_AMOUNT_UPPER_BOUND, "100.0")
                ?.toDoubleOrNull() ?: 100.0,
            statusFilters = sharedPreferences.getString(PAYMENTS_STATUS_FILTER, null)?.let { statusString ->
                if (statusString.isNotEmpty()) {
                    statusString.split(",").mapNotNull { statusName ->
                        try {
                            PaymentsProps.Status.valueOf(statusName.trim())
                        } catch (e: IllegalArgumentException) {
                            // Invalid status name, skip this entry
                            android.util.Log.w("SettingsService", "Invalid status: $statusName", e)
                            null
                        }
                    }
                } else {
                    emptyList()
                }
            } ?: emptyList(),
            paymentMethodFilter = sharedPreferences.getString(PAYMENTS_PAYMENT_METHOD_FILTER, null)?.let {
                PaymentsProps.PaymentMethod.valueOf(it)
            },
            dateFilterType = DateFilterType.valueOf(
                sharedPreferences.getString(PAYMENTS_DATE_FILTER_TYPE, DateFilterType.NONE.name)
                    ?: DateFilterType.NONE.name
            ),
            dateValue = sharedPreferences.getString(PAYMENTS_DATE_VALUE, "") ?: "",
            dateStart = sharedPreferences.getString(PAYMENTS_DATE_START, "") ?: "",
            dateEnd = sharedPreferences.getString(PAYMENTS_DATE_END, "") ?: ""
        )
    }

    @OptIn(PreviewConnectSDK::class)
    fun setPaymentsSettings(value: PaymentsSettings) {
        sharedPreferences.edit {
            putString(PAYMENTS_AMOUNT_FILTER_TYPE, value.amountFilterType.name)
            putString(PAYMENTS_AMOUNT_VALUE, value.amountValue.toString())
            putString(PAYMENTS_AMOUNT_LOWER_BOUND, value.amountLowerBound.toString())
            putString(PAYMENTS_AMOUNT_UPPER_BOUND, value.amountUpperBound.toString())
            putString(PAYMENTS_STATUS_FILTER, value.statusFilters?.joinToString(",") { it.name })
            putString(PAYMENTS_PAYMENT_METHOD_FILTER, value.paymentMethodFilter?.name)
            putString(PAYMENTS_DATE_FILTER_TYPE, value.dateFilterType.name)
            putString(PAYMENTS_DATE_VALUE, value.dateValue)
            putString(PAYMENTS_DATE_START, value.dateStart)
            putString(PAYMENTS_DATE_END, value.dateEnd)
        }
    }

    fun getSelectedMerchant(): String? {
        return sharedPreferences.getString(SELECTED_MERCHANT_KEY, null)
    }

    fun setSelectedMerchant(accountId: String) {
        sharedPreferences.edit { putString(SELECTED_MERCHANT_KEY, accountId) }
    }

    /**
     * Returns a [Flow] that emits the current and changed values of the given [key].
     */
    private inline fun <T> observePref(
        key: String,
        crossinline getter: (sharedPrefs: SharedPreferences, key: String) -> T
    ): Flow<T> {
        return callbackFlow {
            trySend(getter(sharedPreferences, key))
            val callback =
                SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, k ->
                    if (k == key) {
                        trySend(getter(sharedPreferences, key))
                    }
                }
            synchronized(changeListeners) { changeListeners.add(callback) }
            sharedPreferences.registerOnSharedPreferenceChangeListener(callback)
            awaitClose {
                synchronized(changeListeners) { changeListeners.remove(callback) }
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(callback)
            }
        }
    }

    companion object {
        // Keys
        private const val SERVER_BASE_URL_KEY = "ServerBaseURL"
        private const val APPEARANCE_ID_KEY = "AppearanceId"
        private const val SELECTED_MERCHANT_KEY = "SelectedMerchant"
        private const val PRESENTATION_IS_MODAL = "PresentationIsModal"
        private const val DISABLE_EMBED_IN_NAVBAR = "DisableEmbedInNavbar"
        private const val EMBED_IN_TABBAR = "EmbedInTabbar"
        private const val ENABLE_EDGE_TO_EDGE = "EnableEdgeToEdge"
        private const val ONBOARDING_TERMS_OF_SERVICE_URL = "OnboardingTermsOfServiceURL"
        private const val ONBOARDING_RECIPIENT_TERMS_OF_SERVICE_STRING = "OnboardingRecipientTermsOfServiceString"
        private const val ONBOARDING_PRIVACY_POLICY_STRING = "OnboardingPrivacyPolicyString"
        private const val ONBOARDING_SKIP_TERMS_OF_SERVICE = "OnboardingSkipTermsOfService"
        private const val ONBOARDING_FIELD_OPTION = "OnboardingFieldOption"
        private const val ONBOARDING_FUTURE_REQUIREMENTS = "OnboardingFutureRequirements"
        private const val ONBOARDING_REQUIREMENTS_MODE = "OnboardingRequirementsMode"
        private const val ONBOARDING_REQUIREMENTS_TEXT = "OnboardingRequirementsText"
        private const val PAYMENTS_AMOUNT_FILTER_TYPE = "PaymentsAmountFilterType"
        private const val PAYMENTS_AMOUNT_VALUE = "PaymentsAmountValue"
        private const val PAYMENTS_AMOUNT_LOWER_BOUND = "PaymentsAmountLowerBound"
        private const val PAYMENTS_AMOUNT_UPPER_BOUND = "PaymentsAmountUpperBound"
        private const val PAYMENTS_DATE_FILTER_TYPE = "PaymentsDateFilterType"
        private const val PAYMENTS_DATE_VALUE = "PaymentsDateValue"
        private const val PAYMENTS_DATE_START = "PaymentsDateStart"
        private const val PAYMENTS_DATE_END = "PaymentsDateEnd"
        private const val PAYMENTS_STATUS_FILTER = "PaymentsStatusFilter"
        private const val PAYMENTS_PAYMENT_METHOD_FILTER = "PaymentsPaymentMethodFilter"
    }
}

data class OnboardingSettings(
    val fullTermsOfServiceString: String? = null,
    val recipientTermsOfServiceString: String? = null,
    val privacyPolicyString: String? = null,
    val skipTermsOfService: SkipTermsOfService = SkipTermsOfService.DEFAULT,
    val fieldOption: FieldOption = FieldOption.DEFAULT,
    val futureRequirement: FutureRequirement = FutureRequirement.DEFAULT,
    val requirementsMode: RequirementsMode = RequirementsMode.DEFAULT,
    val requirementsText: String? = null,
) {
    fun toProps(): AccountOnboardingProps {
        return AccountOnboardingProps(
            fullTermsOfServiceUrl = fullTermsOfServiceString,
            recipientTermsOfServiceUrl = recipientTermsOfServiceString,
            privacyPolicyUrl = privacyPolicyString,
            skipTermsOfServiceCollection = when (skipTermsOfService) {
                SkipTermsOfService.DEFAULT -> null
                SkipTermsOfService.SKIP -> true
                SkipTermsOfService.SHOW -> false
            },
            collectionOptions = AccountOnboardingProps.CollectionOptions(
                fields = when (fieldOption) {
                    FieldOption.DEFAULT -> null
                    FieldOption.CURRENTLY_DUE -> AccountOnboardingProps.FieldOption.CURRENTLY_DUE
                    FieldOption.EVENTUALLY_DUE -> AccountOnboardingProps.FieldOption.EVENTUALLY_DUE
                },
                futureRequirements = when (futureRequirement) {
                    FutureRequirement.DEFAULT -> null
                    FutureRequirement.OMIT -> AccountOnboardingProps.FutureRequirementOption.OMIT
                    FutureRequirement.INCLUDE -> AccountOnboardingProps.FutureRequirementOption.INCLUDE
                },
                requirements = buildRequirementsOption()
            ),
        )
    }

    private fun buildRequirementsOption(): AccountOnboardingProps.RequirementsOption? {
        return when (requirementsMode) {
            RequirementsMode.DEFAULT -> null
            RequirementsMode.ONLY -> requirementsText?.let { text ->
                if (text.isNotBlank()) {
                    AccountOnboardingProps.RequirementsOption.only(
                        text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    )
                } else {
                    null
                }
            }
            RequirementsMode.EXCLUDE -> requirementsText?.let { text ->
                if (text.isNotBlank()) {
                    AccountOnboardingProps.RequirementsOption.exclude(
                        text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    )
                } else {
                    null
                }
            }
        }
    }
}

data class PresentationSettings(
    val presentationStyleIsPush: Boolean = false,
    val embedInTabBar: Boolean = false,
    val embedInNavBar: Boolean = false,
    val enableEdgeToEdge: Boolean = false,
)

enum class SkipTermsOfService { DEFAULT, SKIP, SHOW }
enum class FieldOption { DEFAULT, CURRENTLY_DUE, EVENTUALLY_DUE }
enum class FutureRequirement { DEFAULT, OMIT, INCLUDE }
enum class RequirementsMode { DEFAULT, ONLY, EXCLUDE }

@OptIn(PreviewConnectSDK::class)
data class PaymentsSettings(
    val amountFilterType: AmountFilterType = AmountFilterType.NONE,
    val amountValue: Double = 0.0,
    val amountLowerBound: Double = 0.0,
    val amountUpperBound: Double = 100.0,
    val statusFilters: List<PaymentsProps.Status> = emptyList(),
    val paymentMethodFilter: PaymentsProps.PaymentMethod? = null,
    val dateFilterType: DateFilterType = DateFilterType.NONE,
    val dateValue: String = "",
    val dateStart: String = "",
    val dateEnd: String = "",
) {
    private fun hasFilters(
        amountFilter: PaymentsProps.AmountFilter?,
        dateFilter: PaymentsProps.DateFilter?,
        statusFilters: List<PaymentsProps.Status>,
        paymentMethodFilter: PaymentsProps.PaymentMethod?
    ): Boolean {
        return amountFilter != null ||
            dateFilter != null ||
            statusFilters.isNotEmpty() ||
            paymentMethodFilter != null
    }

    private fun createAmountFilter(): PaymentsProps.AmountFilter? {
        return when (amountFilterType) {
            AmountFilterType.NONE -> null
            AmountFilterType.EQUALS -> PaymentsProps.AmountFilter.equalTo(amountValue)
            AmountFilterType.GREATER_THAN -> PaymentsProps.AmountFilter.greaterThan(amountValue)
            AmountFilterType.LESS_THAN -> PaymentsProps.AmountFilter.lessThan(amountValue)
            AmountFilterType.BETWEEN -> PaymentsProps.AmountFilter.between(amountLowerBound, amountUpperBound)
        }
    }

    private fun createDateFilter(): PaymentsProps.DateFilter? {
        return when (dateFilterType) {
            DateFilterType.NONE -> null
            DateFilterType.BEFORE -> createSingleDateFilter(dateValue) { date ->
                PaymentsProps.DateFilter.before(date)
            }
            DateFilterType.AFTER -> createSingleDateFilter(dateValue) { date ->
                PaymentsProps.DateFilter.after(date)
            }
            DateFilterType.BETWEEN -> createDateRangeFilter()
        }
    }

    private fun createSingleDateFilter(
        dateString: String,
        filterFactory: (java.util.Date) -> PaymentsProps.DateFilter
    ): PaymentsProps.DateFilter? {
        return if (dateString.isNotEmpty()) {
            try {
                filterFactory(java.util.Date(dateString.toLong()))
            } catch (e: NumberFormatException) {
                android.util.Log.w("SettingsService", "Invalid date: $dateString", e)
                null
            }
        } else {
            null
        }
    }

    private fun createDateRangeFilter(): PaymentsProps.DateFilter? {
        return if (dateStart.isNotEmpty() && dateEnd.isNotEmpty()) {
            try {
                PaymentsProps.DateFilter.between(
                    java.util.Date(dateStart.toLong()),
                    java.util.Date(dateEnd.toLong())
                )
            } catch (e: NumberFormatException) {
                android.util.Log.w("SettingsService", "Invalid date range: $dateStart-$dateEnd", e)
                null
            }
        } else {
            null
        }
    }

    fun toProps(): PaymentsProps {
        val amountFilter = createAmountFilter()
        val dateFilter = createDateFilter()

        val defaultFilters = if (hasFilters(amountFilter, dateFilter, statusFilters, paymentMethodFilter)) {
            PaymentsProps.PaymentsListDefaultFilters(
                amount = amountFilter,
                date = dateFilter,
                status = statusFilters.takeIf { it.isNotEmpty() },
                paymentMethod = paymentMethodFilter
            )
        } else {
            null
        }

        return PaymentsProps(defaultFilters = defaultFilters)
    }
}

enum class AmountFilterType { NONE, EQUALS, GREATER_THAN, LESS_THAN, BETWEEN }
enum class DateFilterType { NONE, BEFORE, AFTER, BETWEEN }
