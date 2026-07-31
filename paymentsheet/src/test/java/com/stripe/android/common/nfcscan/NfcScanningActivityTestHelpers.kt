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
import org.robolectric.shadows.ShadowNfcAdapter

internal object NfcScanningActivityTestHelpers {
    fun launchScenario(
        context: Context,
        composeRule: ComposeTestRule,
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        block: suspend NfcScanningActivityScenario.() -> Unit,
    ) {
        val nfcAdapter = getEnabledNfcAdapter(context)

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
                scenario.onActivity {
                    runBlocking {
                        block(
                            NfcScanningActivityScenario(
                                composeRule = composeRule,
                                activityScenario = scenario,
                                isoDep = fakeIsoDep,
                                nfcAdapter = nfcAdapter,
                            ).apply {
                                waitForUi()
                            }
                        )
                    }
                }
            }
        }
    }

    fun getEnabledNfcAdapter(context: Context): ShadowNfcAdapter? {
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_NFC, true)

        return NfcAdapter.getDefaultAdapter(context)?.let { shadowOf(it) }?.also { it.setEnabled(true) }
    }
}
