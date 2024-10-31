package com.stripe.android.connect.example.data

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.stripe.android.connect.example.ui.appearance.AppearanceInfo

class SettingsService private constructor(context: Context) {

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
        return AppearanceInfo.AppearanceId.valueOf(appearanceId)
    }

    fun setAppearanceId(value: AppearanceInfo.AppearanceId?) {
        sharedPreferences.edit { putString(APPEARANCE_ID_KEY, value?.name) }
    }

    fun getOnboardingSettings(): OnboardingSettings {
        return OnboardingSettings(
            fullTermsOfServiceString = sharedPreferences.getString(ONBOARDING_TERMS_OF_SERVICE_URL, null),
            recipientTermsOfServiceString = sharedPreferences.getString(ONBOARDING_RECIPIENT_TERMS_OF_SERVICE_STRING, null),
            privacyPolicyString = sharedPreferences.getString(ONBOARDING_PRIVACY_POLICY_STRING, null),
            skipTermsOfService = SkipTermsOfService.valueOf(sharedPreferences.getString(ONBOARDING_SKIP_TERMS_OF_SERVICE, SkipTermsOfService.DEFAULT.name) ?: SkipTermsOfService.DEFAULT.name),
            fieldOption = FieldOption.valueOf(sharedPreferences.getString(ONBOARDING_FIELD_OPTION, FieldOption.DEFAULT.name) ?: FieldOption.DEFAULT.name),
            futureRequirement = FutureRequirement.valueOf(sharedPreferences.getString(ONBOARDING_FUTURE_REQUIREMENTS, FutureRequirement.DEFAULT.name) ?: FutureRequirement.DEFAULT.name)
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
            embedInNavBar = !sharedPreferences.getBoolean(DISABLE_EMBED_IN_NAVBAR, false)
        )
    }

    fun setPresentationSettings(value: PresentationSettings) {
        sharedPreferences.edit {
            putBoolean(PRESENTATION_IS_MODAL, !value.presentationStyleIsPush)
            putBoolean(EMBED_IN_TABBAR, value.embedInTabBar)
            putBoolean(DISABLE_EMBED_IN_NAVBAR, !value.embedInNavBar)
        }
    }

    fun getSelectedMerchant(): String? {
        return sharedPreferences.getString(SELECTED_MERCHANT_KEY, null)
    }

    fun setSelectedMerchant(accountId: String) {
        sharedPreferences.edit { putString(SELECTED_MERCHANT_KEY, accountId) }
    }

    companion object {
        // Keys
        private const val SERVER_BASE_URL_KEY = "ServerBaseURL"
        private const val APPEARANCE_ID_KEY = "AppearanceId"
        private const val SELECTED_MERCHANT_KEY = "SelectedMerchant"
        private const val PRESENTATION_IS_MODAL = "PresentationIsModal"
        private const val DISABLE_EMBED_IN_NAVBAR = "DisableEmbedInNavbar"
        private const val EMBED_IN_TABBAR = "EmbedInTabbar"
        private const val ONBOARDING_TERMS_OF_SERVICE_URL = "OnboardingTermsOfServiceURL"
        private const val ONBOARDING_RECIPIENT_TERMS_OF_SERVICE_STRING = "OnboardingRecipientTermsOfServiceString"
        private const val ONBOARDING_PRIVACY_POLICY_STRING = "OnboardingPrivacyPolicyString"
        private const val ONBOARDING_SKIP_TERMS_OF_SERVICE = "OnboardingSkipTermsOfService"
        private const val ONBOARDING_FIELD_OPTION = "OnboardingFieldOption"
        private const val ONBOARDING_FUTURE_REQUIREMENTS = "OnboardingFutureRequirements"

        // Instance
        private var instance: SettingsService? = null

        fun init(application: Application): SettingsService {
            return SettingsService(application).also {
                instance = it
            }
        }

        fun getInstance(): SettingsService {
            return instance ?: throw IllegalStateException("SettingsService is not initialized")
        }
    }
}

data class OnboardingSettings(
    val fullTermsOfServiceString: String?,
    val recipientTermsOfServiceString: String?,
    val privacyPolicyString: String?,
    val skipTermsOfService: SkipTermsOfService,
    val fieldOption: FieldOption,
    val futureRequirement: FutureRequirement
)

data class PresentationSettings(
    val presentationStyleIsPush: Boolean,
    val embedInTabBar: Boolean,
    val embedInNavBar: Boolean
)

enum class SkipTermsOfService { DEFAULT, SKIP, SHOW }
enum class FieldOption { DEFAULT, OPTIONAL, REQUIRED }
enum class FutureRequirement { DEFAULT, INCLUDE, EXCLUDE }
