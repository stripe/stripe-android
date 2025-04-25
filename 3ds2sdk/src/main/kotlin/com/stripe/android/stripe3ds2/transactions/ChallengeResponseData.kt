package com.stripe.android.stripe3ds2.transactions

import android.os.Parcelable
import android.util.Base64
import android.util.DisplayMetrics
import androidx.annotation.VisibleForTesting
import com.stripe.android.stripe3ds2.transaction.SdkTransactionId
import com.stripe.android.stripe3ds2.transaction.TransactionStatus
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.ArrayList
import java.util.UUID

@Parcelize
data class ChallengeResponseData constructor(
    val serverTransId: String,
    val acsTransId: String,
    val acsHtml: String? = null,
    val acsHtmlRefresh: String? = null,
    val uiType: UiType? = null,
    val isChallengeCompleted: Boolean = false,
    val challengeInfoHeader: String? = null,
    val challengeInfoLabel: String? = null,
    val challengeInfoText: String? = null,
    val challengeAdditionalInfoText: String? = null,
    val shouldShowChallengeInfoTextIndicator: Boolean = false,
    val challengeSelectOptions: List<ChallengeSelectOption>? = null,
    val expandInfoLabel: String? = null,
    val expandInfoText: String? = null,
    val issuerImage: Image? = null,
    val messageExtensions: List<MessageExtension>? = null,
    val messageVersion: String,
    val oobAppUrl: String? = null,
    val oobAppLabel: String? = null,
    val oobContinueLabel: String? = null,
    val paymentSystemImage: Image? = null,
    val resendInformationLabel: String? = null,
    val sdkTransId: SdkTransactionId,
    val submitAuthenticationLabel: String? = null,
    val whitelistingInfoText: String? = null,
    val whyInfoLabel: String? = null,
    val whyInfoText: String? = null,
    val transStatus: String? = null
) : Parcelable {

    /**
     * @return true if there is no UI to show for this CRes, or if the CRes's fields are valid for
     * the given [UiType]
     */
    internal val isValidForUi: Boolean
        @VisibleForTesting
        get() {
            if (uiType == null) {
                return true
            }

            if (uiType == UiType.Html) {
                return !acsHtml.isNullOrBlank()
            }

            // required fields for text, single-select, multi-select
            val isTextOrSelectType = uiType == UiType.Text ||
                uiType == UiType.SingleSelect ||
                uiType == UiType.MultiSelect

            if (isTextOrSelectType &&
                setOf(
                    challengeInfoHeader,
                    challengeInfoLabel,
                    challengeInfoText
                ).any { it.isNullOrBlank() }
            ) {
                return false
            }

            // required fields for oob
            if (uiType == UiType.OutOfBand && setOf(
                    challengeInfoHeader,
                    challengeInfoText
                ).any { it.isNullOrBlank() }
            ) {
                return false
            }

            if (!oobContinueLabel.isNullOrEmpty() &&
                (
                    challengeInfoHeader.isNullOrEmpty() &&
                        challengeInfoText.isNullOrEmpty()
                    )
            ) {
                return false
            }

            if (uiType == UiType.OutOfBand) {
                return setOf(
                    oobAppLabel,
                    oobAppUrl,
                    oobContinueLabel
                ).any {
                    !it.isNullOrBlank()
                }
            }

            if (uiType == UiType.SingleSelect || uiType == UiType.MultiSelect) {
                if (challengeSelectOptions.isNullOrEmpty()) {
                    return false
                }
            }

            // required for text, single-select, and multi-select
            return !submitAuthenticationLabel.isNullOrBlank()
        }

    @Throws(JSONException::class)
    fun toJson(): JSONObject {
        return JSONObject()
            .put(FIELD_MESSAGE_TYPE, MESSAGE_TYPE)
            .put(FIELD_SERVER_TRANS_ID, serverTransId)
            .put(FIELD_ACS_TRANS_ID, acsTransId)
            .put(FIELD_ACS_HTML, acsHtml)
            .put(FIELD_ACS_HTML_REFRESH, acsHtmlRefresh)
            .put(FIELD_ACS_UI_TYPE, uiType?.code)
            .put(FIELD_CHALLENGE_COMPLETION_INDICATOR, if (isChallengeCompleted) "Y" else "N")
            .put(FIELD_CHALLENGE_INFO_HEADER, challengeInfoHeader)
            .put(FIELD_CHALLENGE_INFO_LABEL, challengeInfoLabel)
            .put(FIELD_CHALLENGE_INFO_TEXT, challengeInfoText)
            .put(FIELD_CHALLENGE_ADDITIONAL_INFO_TEXT, challengeAdditionalInfoText)
            .put(
                FIELD_CHALLENGE_SELECT_INFO,
                ChallengeSelectOption.toJsonArray(challengeSelectOptions)
            )
            .put(FIELD_EXPAND_INFO_LABEL, expandInfoLabel)
            .put(FIELD_EXPAND_INFO_TEXT, expandInfoText)
            .put(
                FIELD_ISSUER_IMAGE,
                issuerImage?.toJson()
            )
            .put(
                FIELD_MESSAGE_EXTENSION,
                MessageExtension.toJsonArray(messageExtensions)
            )
            .put(FIELD_MESSAGE_VERSION, messageVersion)
            .put(FIELD_OOB_APP_URL, oobAppUrl)
            .put(FIELD_OOB_APP_LABEL, oobAppLabel)
            .put(FIELD_OOB_CONTINUE_LABEL, oobContinueLabel)
            .put(
                FIELD_PAYMENT_SYSTEM_IMAGE,
                paymentSystemImage?.toJson()
            )
            .put(FIELD_RESEND_INFORMATION_LABEL, resendInformationLabel)
            .put(FIELD_SDK_TRANS_ID, sdkTransId)
            .put(FIELD_SUBMIT_AUTHENTICATION_LABEL, submitAuthenticationLabel)
            .put(FIELD_WHITELISTING_INFO_TEXT, whitelistingInfoText)
            .put(FIELD_WHY_INFO_LABEL, whyInfoLabel)
            .put(FIELD_WHY_INFO_TEXT, whyInfoText)
            .put(FIELD_TRANS_STATUS, transStatus)
            .also {
                if (!isChallengeCompleted) {
                    it.put(
                        FIELD_CHALLENGE_INFO_TEXT_INDICATOR,
                        if (shouldShowChallengeInfoTextIndicator) "Y" else "N"
                    )
                }
            }
    }

    @Parcelize
    data class Image constructor(
        internal val mediumUrl: String? = null,
        internal val highUrl: String? = null,
        internal val extraHighUrl: String?
    ) : Parcelable {

        /**
         * Returns the highest fidelity image URL, or null if no image URLs
         *
         * @return highest fidelity image URL
         */
        val highestFidelityImageUrl: String?
            get() {
                return listOf(
                    extraHighUrl,
                    highUrl,
                    mediumUrl
                ).firstOrNull {
                    !it.isNullOrBlank()
                }
            }

        @Throws(JSONException::class)
        internal fun toJson(): JSONObject {
            return JSONObject()
                .put(FIELD_MEDIUM, mediumUrl)
                .put(FIELD_HIGH, highUrl)
                .put(FIELD_EXTRA_HIGH, extraHighUrl)
        }

        /**
         * Determine which of the 3 image URLs should be used to display the image based on the
         * screen's density. If the density-appropriate URL is null, return the highest fidelity
         * URL.
         *
         * @param density the device's screen density
         * @return the image URL most appropriate for the given density
         */
        fun getUrlForDensity(density: Int): String? {
            return when {
                density <= DisplayMetrics.DENSITY_MEDIUM -> mediumUrl
                density >= DisplayMetrics.DENSITY_XHIGH -> extraHighUrl
                else -> highUrl
            }.takeUnless {
                it.isNullOrBlank()
            } ?: highestFidelityImageUrl
        }

        internal companion object {
            private const val FIELD_MEDIUM = "medium"
            private const val FIELD_HIGH = "high"
            private const val FIELD_EXTRA_HIGH = "extraHigh"

            internal fun fromJson(imageJson: JSONObject?): Image? {
                return imageJson?.let {
                    Image(
                        it.optString(FIELD_MEDIUM),
                        it.optString(FIELD_HIGH),
                        it.optString(FIELD_EXTRA_HIGH)
                    )
                }
            }
        }
    }

    @Parcelize
    data class ChallengeSelectOption constructor(
        val name: String,
        val text: String
    ) : Parcelable {

        @Throws(JSONException::class)
        private fun toJson(): JSONObject {
            return JSONObject()
                .put(name, text)
        }

        internal companion object {
            internal fun fromJson(
                challengeSelectOptionsJson: JSONArray?
            ): List<ChallengeSelectOption>? {
                if (challengeSelectOptionsJson == null) {
                    return null
                }

                val challengeSelectOptions = ArrayList<ChallengeSelectOption>()
                for (i in 0 until challengeSelectOptionsJson.length()) {
                    challengeSelectOptionsJson.optJSONObject(i)?.let {
                        val name = it.keys().next()
                        val text = it.optString(name)
                        challengeSelectOptions.add(ChallengeSelectOption(name, text))
                    }
                }

                return challengeSelectOptions
            }

            @Throws(JSONException::class)
            internal fun toJsonArray(options: List<ChallengeSelectOption>?): JSONArray? {
                if (options == null) {
                    return null
                }

                val optionsJsonArray = JSONArray()
                for (option in options) {
                    optionsJsonArray.put(option.toJson())
                }
                return optionsJsonArray
            }
        }
    }

    internal companion object {
        private const val FIELD_SERVER_TRANS_ID = "threeDSServerTransID"
        private const val FIELD_ACS_TRANS_ID = "acsTransID"
        private const val FIELD_ACS_HTML = "acsHTML"
        private const val FIELD_ACS_HTML_REFRESH = "acsHTMLRefresh"
        private const val FIELD_ACS_UI_TYPE = "acsUiType"
        private const val FIELD_CHALLENGE_ADDITIONAL_INFO_TEXT = "challengeAddInfo"
        private const val FIELD_CHALLENGE_COMPLETION_INDICATOR = "challengeCompletionInd"
        private const val FIELD_CHALLENGE_INFO_HEADER = "challengeInfoHeader"
        private const val FIELD_CHALLENGE_INFO_LABEL = "challengeInfoLabel"
        private const val FIELD_CHALLENGE_INFO_TEXT = "challengeInfoText"
        private const val FIELD_CHALLENGE_INFO_TEXT_INDICATOR = "challengeInfoTextIndicator"
        private const val FIELD_CHALLENGE_SELECT_INFO = "challengeSelectInfo"
        private const val FIELD_EXPAND_INFO_LABEL = "expandInfoLabel"
        private const val FIELD_EXPAND_INFO_TEXT = "expandInfoText"
        private const val FIELD_ISSUER_IMAGE = "issuerImage"
        private const val FIELD_MESSAGE_EXTENSION = "messageExtension"
        private const val FIELD_MESSAGE_TYPE = "messageType"
        private const val FIELD_MESSAGE_VERSION = "messageVersion"
        private const val FIELD_OOB_APP_URL = "oobAppURL"
        private const val FIELD_OOB_APP_LABEL = "oobAppLabel"
        private const val FIELD_OOB_CONTINUE_LABEL = "oobContinueLabel"
        private const val FIELD_PAYMENT_SYSTEM_IMAGE = "psImage"
        private const val FIELD_RESEND_INFORMATION_LABEL = "resendInformationLabel"
        private const val FIELD_SDK_TRANS_ID = "sdkTransID"
        private const val FIELD_SUBMIT_AUTHENTICATION_LABEL = "submitAuthenticationLabel"
        private const val FIELD_WHITELISTING_INFO_TEXT = "whitelistingInfoText"
        private const val FIELD_WHY_INFO_LABEL = "whyInfoLabel"
        private const val FIELD_WHY_INFO_TEXT = "whyInfoText"
        private const val FIELD_TRANS_STATUS = "transStatus"

        internal const val MESSAGE_TYPE = "CRes"

        private const val YES_VALUE = "Y"
        private const val NO_VALUE = "N"
        private const val WHITELIST_INFO_TEXT_MAX_LENGTH = 64

        private val YES_NO_VALUES = listOf(YES_VALUE, NO_VALUE)

        /**
         * @param cresJson the decrypted JSON returned in the CRes
         * @return a [ChallengeResponseData] model object
         *
         * @throws ChallengeResponseParseException if the JSON format or data fails validation
         */
        @Throws(ChallengeResponseParseException::class)
        @Suppress("LongMethod", "CyclomaticComplexMethod", "MaximumLineLength", "MaxLineLength")
        internal fun fromJson(cresJson: JSONObject): ChallengeResponseData {
            checkMessageType(cresJson)

            val isChallengedCompleted =
                getYesNoValue(cresJson, FIELD_CHALLENGE_COMPLETION_INDICATOR, true)
            val sdkTransId = SdkTransactionId(getTransactionId(cresJson, FIELD_SDK_TRANS_ID))
            val serverTransId = getTransactionId(cresJson, FIELD_SERVER_TRANS_ID).toString()
            val acsTransId = getTransactionId(cresJson, FIELD_ACS_TRANS_ID).toString()
            val messageVersion = getMessageVersion(cresJson)
            val messageExtensions = getMessageExtensions(cresJson)

            val shouldShowChallengeInfoTextIndicator =
                getYesNoValue(cresJson, FIELD_CHALLENGE_INFO_TEXT_INDICATOR, false)
            val resendInformationLabel = getResendInformationLabel(cresJson)
            val challengeSelectOptionsJsonArray = getChallengeSelectInfoArray(cresJson)
            val uiType = if (isChallengedCompleted) null else getUiType(cresJson)
            val submitAuthenticationLabel = uiType?.let { getSubmitAuthenticationLabel(cresJson, uiType) }
            val acsHtml = uiType?.let { getDecodedAcsHtml(cresJson, uiType) }
            val oobContinueLabel = uiType?.let { getOobContinueLabel(cresJson, uiType) }
            val challengeSelectOptions =
                ChallengeSelectOption.fromJson(challengeSelectOptionsJsonArray)
            val acsHtmlRefresh = if (isChallengedCompleted) null else decodeHtml(cresJson.optString(FIELD_ACS_HTML_REFRESH))
            val challengeInfoHeader = if (isChallengedCompleted) null else cresJson.optString(FIELD_CHALLENGE_INFO_HEADER)
            val challengeInfoLabel = if (isChallengedCompleted) null else cresJson.optString(FIELD_CHALLENGE_INFO_LABEL)
            val challengeInfoText = if (isChallengedCompleted) null else cresJson.optString(FIELD_CHALLENGE_INFO_TEXT)
            val challengeAdditionalInfoText = if (isChallengedCompleted) null else cresJson.optString(FIELD_CHALLENGE_ADDITIONAL_INFO_TEXT)
            val whitelistingInfoText = if (isChallengedCompleted) null else cresJson.optString(FIELD_WHITELISTING_INFO_TEXT)
            val whyInfoLabel = if (isChallengedCompleted) null else cresJson.optString(FIELD_WHY_INFO_LABEL)
            val whyInfoText = if (isChallengedCompleted) null else cresJson.optString(FIELD_WHY_INFO_TEXT)

            val cresData = ChallengeResponseData(
                serverTransId = serverTransId,
                acsTransId = acsTransId,
                sdkTransId = sdkTransId,
                isChallengeCompleted = isChallengedCompleted,
                messageVersion = messageVersion,
                messageExtensions = messageExtensions,
                acsHtml = acsHtml,
                acsHtmlRefresh = acsHtmlRefresh,
                uiType = uiType,
                challengeInfoHeader = challengeInfoHeader,
                challengeInfoLabel = challengeInfoLabel,
                challengeInfoText = challengeInfoText,
                challengeAdditionalInfoText = challengeAdditionalInfoText,
                shouldShowChallengeInfoTextIndicator = shouldShowChallengeInfoTextIndicator,
                challengeSelectOptions = challengeSelectOptions,
                expandInfoLabel = if (isChallengedCompleted) null else cresJson.optString(FIELD_EXPAND_INFO_LABEL),
                expandInfoText = if (isChallengedCompleted) null else cresJson.optString(FIELD_EXPAND_INFO_TEXT),
                issuerImage = Image.fromJson(cresJson.optJSONObject(FIELD_ISSUER_IMAGE)),
                oobAppUrl = if (isChallengedCompleted) null else cresJson.optString(FIELD_OOB_APP_URL),
                oobAppLabel = if (isChallengedCompleted) null else cresJson.optString(FIELD_OOB_APP_LABEL),
                oobContinueLabel = oobContinueLabel,
                paymentSystemImage = Image.fromJson(
                    cresJson.optJSONObject(
                        FIELD_PAYMENT_SYSTEM_IMAGE
                    )
                ),
                resendInformationLabel = resendInformationLabel,
                submitAuthenticationLabel = submitAuthenticationLabel,
                whitelistingInfoText = whitelistingInfoText,
                whyInfoLabel = whyInfoLabel,
                whyInfoText = whyInfoText,
                transStatus = if (isChallengedCompleted) getTransStatus(cresJson).code else ""
            )

            if (!cresData.isValidForUi) {
                throw ChallengeResponseParseException
                    .createRequiredDataElementMissing("UI fields missing")
            }

            if (cresData.whitelistingInfoText != null &&
                cresData.whitelistingInfoText.length > WHITELIST_INFO_TEXT_MAX_LENGTH
            ) {
                throw ChallengeResponseParseException
                    .createInvalidDataElementFormat("Whitelisting info text exceeds length.")
            }

            return cresData
        }

        @VisibleForTesting
        @Throws(ChallengeResponseParseException::class)
        internal fun checkMessageType(cresJson: JSONObject) {
            if (MESSAGE_TYPE != cresJson.optString(FIELD_MESSAGE_TYPE)) {
                throw ChallengeResponseParseException(
                    ProtocolError.InvalidMessageReceived.code,
                    "Message is not CRes",
                    "Invalid Message Type"
                )
            }
        }

        @VisibleForTesting
        @Throws(ChallengeResponseParseException::class)
        internal fun getUiType(cresJson: JSONObject): UiType {
            val uiTypeCode = cresJson.optString(FIELD_ACS_UI_TYPE)
            if (uiTypeCode.isNullOrBlank()) {
                throw ChallengeResponseParseException
                    .createRequiredDataElementMissing(FIELD_ACS_UI_TYPE)
            }

            return UiType.fromCode(uiTypeCode)
                ?: throw ChallengeResponseParseException.createInvalidDataElementFormat(
                    FIELD_ACS_UI_TYPE
                )
        }

        @VisibleForTesting
        @Throws(ChallengeResponseParseException::class)
        internal fun getYesNoValue(
            cresJson: JSONObject,
            fieldName: String,
            isRequired: Boolean
        ): Boolean {
            val value: String? = if (isRequired) {
                if (cresJson.has(fieldName)) {
                    cresJson.getString(fieldName)
                } else {
                    throw ChallengeResponseParseException
                        .createRequiredDataElementMissing(fieldName)
                }
            } else {
                getOrNull(cresJson, fieldName)
            }
            if (value != null && !YES_NO_VALUES.contains(value)) {
                throw if (isRequired && value.isNullOrBlank()) {
                    ChallengeResponseParseException.createRequiredDataElementMissing(fieldName)
                } else {
                    ChallengeResponseParseException.createInvalidDataElementFormat(fieldName)
                }
            } else {
                return YES_VALUE == value
            }
        }

        @VisibleForTesting
        @Throws(ChallengeResponseParseException::class)
        internal fun getResendInformationLabel(cresJson: JSONObject): String? {
            val resendInformationLabel = getOrNull(cresJson, FIELD_RESEND_INFORMATION_LABEL)
            if (resendInformationLabel != null && resendInformationLabel.isEmpty()) {
                throw ChallengeResponseParseException
                    .createInvalidDataElementFormat(FIELD_RESEND_INFORMATION_LABEL)
            }

            return resendInformationLabel
        }

        @VisibleForTesting
        @Throws(ChallengeResponseParseException::class)
        internal fun getChallengeSelectInfoArray(cresJson: JSONObject): JSONArray? {
            return cresJson.takeIf { it.has(FIELD_CHALLENGE_SELECT_INFO) }?.let {
                runCatching {
                    it.getJSONArray(FIELD_CHALLENGE_SELECT_INFO)
                }.getOrElse {
                    throw ChallengeResponseParseException
                        .createInvalidDataElementFormat(FIELD_CHALLENGE_SELECT_INFO)
                }
            }
        }

        @VisibleForTesting
        @Throws(ChallengeResponseParseException::class)
        internal fun getMessageVersion(cresJson: JSONObject): String {
            return cresJson.optString(FIELD_MESSAGE_VERSION).takeIf {
                it.isNotBlank()
            } ?: throw ChallengeResponseParseException
                .createRequiredDataElementMissing(FIELD_MESSAGE_VERSION)
        }

        @VisibleForTesting
        @Throws(ChallengeResponseParseException::class)
        internal fun getTransactionId(cresJson: JSONObject, fieldName: String): UUID {
            val transId = cresJson.optString(fieldName)
            if (transId.isNullOrBlank()) {
                throw ChallengeResponseParseException.createRequiredDataElementMissing(fieldName)
            }
            runCatching {
                return UUID.fromString(transId)
            }.getOrElse {
                throw ChallengeResponseParseException.createInvalidDataElementFormat(fieldName)
            }
        }

        @VisibleForTesting
        @Throws(ChallengeResponseParseException::class)
        internal fun getTransStatus(cresJson: JSONObject): TransactionStatus {
            val transactionStatusCode = cresJson.optString(FIELD_TRANS_STATUS)
            if (transactionStatusCode.isNullOrBlank()) {
                throw ChallengeResponseParseException
                    .createRequiredDataElementMissing(FIELD_TRANS_STATUS)
            }
            val transactionStatus = TransactionStatus
                .fromCode(transactionStatusCode)
            return transactionStatus
                ?: throw ChallengeResponseParseException
                    .createInvalidDataElementFormat(FIELD_TRANS_STATUS)
        }

        @Throws(ChallengeResponseParseException::class)
        internal fun getSubmitAuthenticationLabel(cresJson: JSONObject, uiType: UiType): String? {
            val submitAuthenticationLabel = getOrNull(cresJson, FIELD_SUBMIT_AUTHENTICATION_LABEL)

            if (submitAuthenticationLabel.isNullOrBlank() && uiType.requiresSubmitButton) {
                throw ChallengeResponseParseException
                    .createRequiredDataElementMissing(FIELD_SUBMIT_AUTHENTICATION_LABEL)
            }

            return submitAuthenticationLabel
        }

        @VisibleForTesting
        @Throws(ChallengeResponseParseException::class)
        internal fun getDecodedAcsHtml(cresJson: JSONObject, uiType: UiType): String? {
            val encodedHtml = getOrNull(cresJson, FIELD_ACS_HTML)
            if (encodedHtml.isNullOrBlank() && uiType == UiType.Html) {
                throw ChallengeResponseParseException
                    .createRequiredDataElementMissing(FIELD_ACS_HTML)
            }

            val containsBadHTML = encodedHtml == null ||
                encodedHtml.contains("\n") ||
                encodedHtml.contains(" ") ||
                encodedHtml.contains("+") ||
                encodedHtml.contains("/")
            val endsWithBadSuffix = encodedHtml != null && encodedHtml.endsWith("=")

            if (uiType == UiType.Html && (containsBadHTML || endsWithBadSuffix)) {
                throw ChallengeResponseParseException
                    .createInvalidDataElementFormat(FIELD_ACS_HTML)
            }

            return decodeHtml(encodedHtml)
        }

        private fun decodeHtml(encodedHtml: String?): String? {
            return encodedHtml?.let {
                runCatching {
                    String(Base64.decode(encodedHtml, Base64.URL_SAFE), Charsets.UTF_8)
                }.getOrNull()
            }
        }

        @VisibleForTesting
        @Throws(ChallengeResponseParseException::class)
        internal fun getOobContinueLabel(cresJson: JSONObject, uiType: UiType): String? {
            val oobContinueLabel = cresJson.optString(FIELD_OOB_CONTINUE_LABEL)
            if (oobContinueLabel.isNullOrBlank() && uiType == UiType.OutOfBand) {
                throw ChallengeResponseParseException
                    .createRequiredDataElementMissing(FIELD_OOB_CONTINUE_LABEL)
            }

            return oobContinueLabel
        }

        @VisibleForTesting
        @Throws(ChallengeResponseParseException::class)
        internal fun getMessageExtensions(cresJson: JSONObject): List<MessageExtension>? {
            val messageExtensions =
                MessageExtension.fromJson(cresJson.optJSONArray(FIELD_MESSAGE_EXTENSION))

            if (messageExtensions != null) {
                val unrecognizedCriticalMessageExtensions =
                    messageExtensions.filter {
                        it.criticalityIndicator && !it.isProcessable
                    }

                if (unrecognizedCriticalMessageExtensions.isNotEmpty()) {
                    throw ChallengeResponseParseException(
                        ProtocolError.UnrecognizedCriticalMessageExtensions,
                        unrecognizedCriticalMessageExtensions.joinToString(separator = ",")
                    )
                }
            }

            return messageExtensions
        }

        private fun getOrNull(json: JSONObject, fieldName: String): String? {
            return if (json.has(fieldName)) {
                json.getString(fieldName)
            } else {
                null
            }
        }
    }
}
