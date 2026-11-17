package com.stripe.android.link

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.testing.FeatureFlagTestRule
import org.junit.Rule
import org.junit.Test

internal class LinkConfigurationEmbeddedInlineOtpTest {

    @get:Rule
    val inlineOtpFeatureFlagTestRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.embeddedInlineLinkOtp,
        isEnabled = false,
    )

    @get:Rule
    val forceDialogFeatureFlagTestRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.embeddedInlineLinkOtpForceDialog,
        isEnabled = false,
    )

    @Test
    fun `inline OTP is on when the server flag is true`() {
        inlineOtpFeatureFlagTestRule.setEnabled(null)

        val configuration = configuration(ENABLE_FLAG to true)

        assertThat(configuration.enableEmbeddedInlineOtp).isTrue()
    }

    @Test
    fun `inline OTP is off when the server flag is false`() {
        inlineOtpFeatureFlagTestRule.setEnabled(null)

        val configuration = configuration(ENABLE_FLAG to false)

        assertThat(configuration.enableEmbeddedInlineOtp).isFalse()
    }

    @Test
    fun `inline OTP is off when the server flag is missing`() {
        inlineOtpFeatureFlagTestRule.setEnabled(null)

        val configuration = configuration()

        assertThat(configuration.enableEmbeddedInlineOtp).isFalse()
    }

    @Test
    fun `local override turns inline OTP on`() {
        inlineOtpFeatureFlagTestRule.setEnabled(true)

        val configuration = configuration(ENABLE_FLAG to false)

        assertThat(configuration.enableEmbeddedInlineOtp).isTrue()
    }

    @Test
    fun `local override turns inline OTP off`() {
        inlineOtpFeatureFlagTestRule.setEnabled(false)

        val configuration = configuration(ENABLE_FLAG to true)

        assertThat(configuration.enableEmbeddedInlineOtp).isFalse()
    }

    @Test
    fun `dialog is forced when the server flag is true`() {
        forceDialogFeatureFlagTestRule.setEnabled(null)

        val configuration = configuration(FORCE_DIALOG_FLAG to true)

        assertThat(configuration.embeddedInlineOtpForcesDialog).isTrue()
    }

    @Test
    fun `dialog is not forced when the server flag is false`() {
        forceDialogFeatureFlagTestRule.setEnabled(null)

        val configuration = configuration(FORCE_DIALOG_FLAG to false)

        assertThat(configuration.embeddedInlineOtpForcesDialog).isFalse()
    }

    @Test
    fun `dialog is not forced when the server flag is missing`() {
        forceDialogFeatureFlagTestRule.setEnabled(null)

        val configuration = configuration()

        assertThat(configuration.embeddedInlineOtpForcesDialog).isFalse()
    }

    @Test
    fun `local override forces the dialog`() {
        forceDialogFeatureFlagTestRule.setEnabled(true)

        val configuration = configuration(FORCE_DIALOG_FLAG to false)

        assertThat(configuration.embeddedInlineOtpForcesDialog).isTrue()
    }

    @Test
    fun `local override stops forcing the dialog`() {
        forceDialogFeatureFlagTestRule.setEnabled(false)

        val configuration = configuration(FORCE_DIALOG_FLAG to true)

        assertThat(configuration.embeddedInlineOtpForcesDialog).isFalse()
    }

    private fun configuration(vararg flags: Pair<String, Boolean>): LinkConfiguration {
        return TestFactory.LINK_CONFIGURATION.copy(flags = flags.toMap())
    }

    private companion object {
        const val ENABLE_FLAG = "link_mobile_enable_embedded_inline_otp"
        const val FORCE_DIALOG_FLAG = "link_mobile_embedded_inline_otp_force_dialog"
    }
}
