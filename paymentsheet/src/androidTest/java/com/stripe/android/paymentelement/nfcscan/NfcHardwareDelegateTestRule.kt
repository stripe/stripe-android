package com.stripe.android.paymentelement.nfcscan

import android.nfc.NfcAntennaInfo
import android.nfc.Tag
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.common.nfcscan.hardware.NfcHardwareDelegate
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

internal class NfcHardwareDelegateTestRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                NfcHardwareDelegate.override = AvailableNfcHardwareDelegate()
                try {
                    base.evaluate()
                } finally {
                    NfcHardwareDelegate.override = null
                }
            }
        }
    }
}

private class AvailableNfcHardwareDelegate : NfcHardwareDelegate {
    override fun isAvailable(): Boolean = true

    override fun antenna(): NfcAntennaInfo? = null

    override fun start(
        activity: AppCompatActivity,
        onTagDiscovered: (Tag) -> Unit,
    ) {
        throw IllegalStateException("Should not be called!")
    }
}
