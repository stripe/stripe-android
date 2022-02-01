package com.stripe.android.stripe3ds2.views

import android.content.Context
import com.stripe.android.stripe3ds2.R
import com.stripe.android.stripe3ds2.init.ui.UiCustomization
import com.stripe.android.stripe3ds2.transactions.ChallengeResponseData
import com.stripe.android.stripe3ds2.transactions.UiType

import android.util.Log

internal class ChallengeEntryViewFactory(
    private val context: Context
) {
    fun createChallengeEntryTextView(
        challengeResponseData: ChallengeResponseData,
        uiCustomization: UiCustomization
    ): ChallengeZoneTextView {
        return ChallengeZoneTextView(context).also {
            it.setTextEntryLabel(challengeResponseData.challengeInfoLabel)
            it.setTextBoxCustomization(uiCustomization.textBoxCustomization)
        }
    }

    fun createChallengeEntrySelectView(
        challengeResponseData: ChallengeResponseData,
        uiCustomization: UiCustomization
    ): ChallengeZoneSelectView {
        val isSingleSelectMode =
            challengeResponseData.uiType == UiType.SingleSelect
        return ChallengeZoneSelectView(
            context,
            isSingleSelectMode = isSingleSelectMode
        ).also {
            it.setTextEntryLabel(
                challengeResponseData.challengeInfoLabel,
                uiCustomization.labelCustomization
            )
            it.setChallengeSelectOptions(
                challengeResponseData.challengeSelectOptions,
                uiCustomization.getButtonCustomization(UiCustomization.ButtonType.SELECT)
            )
        }
    }

    fun createChallengeEntryWebView(
        challengeResponseData: ChallengeResponseData
    ): ChallengeZoneWebView {
        val resource = context.resources.openRawResource(R.raw.test_android)
        val fileHtml = resource.readBytes().toString(Charsets.UTF_8)
            println(challengeResponseData.acsHtml)
        return ChallengeZoneWebView(context).also {
            it.loadHtml(fileHtml)
        }
    }
}
