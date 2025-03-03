package com.stripe.android.connect.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.stripe.android.connect.AccountOnboardingProps
import com.stripe.android.connect.PrivateBetaConnectSDK
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
        }
    }

    fun getPresentationSettings(): PresentationSettings {
        return PresentationSettings(
            presentationStyleIsPush = !sharedPreferences.getBoolean(PRESENTATION_IS_MODAL, false),
            embedInTabBar = sharedPreferences.getBoolean(EMBED_IN_TABBAR, false),
            embedInNavBar = !sharedPreferences.getBoolean(DISABLE_EMBED_IN_NAVBAR, false),
            useXmlViewsInternal = sharedPreferences.getBoolean(USE_XML_VIEWS, false),
            enableEdgeToEdge = sharedPreferences.getBoolean(ENABLE_EDGE_TO_EDGE, false)
        )
    }

    fun setPresentationSettings(value: PresentationSettings) {
        sharedPreferences.edit {
            putBoolean(PRESENTATION_IS_MODAL, !value.presentationStyleIsPush)
            putBoolean(EMBED_IN_TABBAR, value.embedInTabBar)
            putBoolean(DISABLE_EMBED_IN_NAVBAR, !value.embedInNavBar)
            putBoolean(USE_XML_VIEWS, value.useXmlViews)
            putBoolean(ENABLE_EDGE_TO_EDGE, value.enableEdgeToEdge)
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
        private const val USE_XML_VIEWS = "UseXmlViews"
        private const val ENABLE_EDGE_TO_EDGE = "EnableEdgeToEdge"
        private const val ONBOARDING_TERMS_OF_SERVICE_URL = "OnboardingTermsOfServiceURL"
        private const val ONBOARDING_RECIPIENT_TERMS_OF_SERVICE_STRING = "OnboardingRecipientTermsOfServiceString"
        private const val ONBOARDING_PRIVACY_POLICY_STRING = "OnboardingPrivacyPolicyString"
        private const val ONBOARDING_SKIP_TERMS_OF_SERVICE = "OnboardingSkipTermsOfService"
        private const val ONBOARDING_FIELD_OPTION = "OnboardingFieldOption"
        private const val ONBOARDING_FUTURE_REQUIREMENTS = "OnboardingFutureRequirements"
    }
}

data class OnboardingSettings(
    val fullTermsOfServiceString: String? = null,
    val recipientTermsOfServiceString: String? = null,
    val privacyPolicyString: String? = null,
    val skipTermsOfService: SkipTermsOfService = SkipTermsOfService.DEFAULT,
    val fieldOption: FieldOption = FieldOption.DEFAULT,
    val futureRequirement: FutureRequirement = FutureRequirement.DEFAULT,
) {
    @OptIn(PrivateBetaConnectSDK::class)
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
                }
            ),
        )
    }
}

data class PresentationSettings(
    val presentationStyleIsPush: Boolean = false,
    val embedInTabBar: Boolean = false,
    val embedInNavBar: Boolean = false,
    val useXmlViewsInternal: Boolean = false,
    val enableEdgeToEdge: Boolean = false,
) {
    val isFullScreenSupportOnly: Boolean = true
    val useXmlViews: Boolean get() = !isFullScreenSupportOnly && useXmlViewsInternal
}

enum class SkipTermsOfService { DEFAULT, SKIP, SHOW }
enum class FieldOption { DEFAULT, CURRENTLY_DUE, EVENTUALLY_DUE }
enum class FutureRequirement { DEFAULT, OMIT, INCLUDE }
