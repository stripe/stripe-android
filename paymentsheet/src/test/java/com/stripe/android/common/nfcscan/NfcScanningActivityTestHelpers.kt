 package com.stripe.android.common.nfcscan

import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.tech.IsoDep
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.core.app.ActivityScenario
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import kotlinx.coroutines.runBlocking
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mockStatic
import org.robolectric.Shadows.shadowOf

internal object NfcScanningActivityTestHelpers {
    fun launchScenario(
        context: Context,
        composeRule: ComposeTestRule,
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        autoAdvance: Boolean = true,
        block: suspend NfcScanningActivityScenario.() -> Unit,
    ) {
        composeRule.mainClock.autoAdvance = autoAdvance

        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_NFC, true)

        val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
            ?.let { shadowOf(it) }
            ?.also { it.setEnabled(true) }

        val fakeIsoDep = FakeIsoDep()
        val intent = NfcScanningContract.createIntent(
            context = context,
            input = NfcScanningContract.Args(
                paymentMethodMetadata = paymentMethodMetadata,
            ),
        )

        mockStatic(IsoDep::class.java).use { mockedIsoDep ->
            mockedIsoDep.`when`<IsoDep> { IsoDep.get(any()) }.thenReturn(fakeIsoDep.wrappedInstance)

            ActivityScenario.launchActivityForResult<NfcScanningActivity>(intent).use { scenario ->
                scenario.onActivity { activity ->
                    runBlocking {
                        block(
                            NfcScanningActivityScenario(
                                activity = activity,
                                composeRule = composeRule,
                                activityScenario = scenario,
                                isoDep = fakeIsoDep,
                                nfcAdapter = nfcAdapter,
                                paymentMethodMetadata = paymentMethodMetadata,
                            ).apply {
                                waitForUi()
                            }
                        )
                    }
                }
            }

            fakeIsoDep.ensureAllEventsConsumed()
        }
    }
}
