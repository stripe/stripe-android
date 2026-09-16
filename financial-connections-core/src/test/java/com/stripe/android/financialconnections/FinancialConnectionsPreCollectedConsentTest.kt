package com.stripe.android.financialconnections

import android.os.Parcel
import com.google.common.truth.Truth.assertThat
import kotlinx.parcelize.parcelableCreator
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FinancialConnectionsPreCollectedConsentTest {

    @Test
    fun `parcel round trip preserves evidence exactly`() {
        val consent = FinancialConnectionsPreCollectedConsent(
            consent = "fccons_123",
            collectedAt = 1_725_000_000L,
        )
        val parcel = Parcel.obtain()

        consent.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val restoredConsent = parcelableCreator<FinancialConnectionsPreCollectedConsent>().createFromParcel(parcel)

        assertThat(restoredConsent.consent).isEqualTo("fccons_123")
        assertThat(restoredConsent.collectedAt).isEqualTo(1_725_000_000L)
        parcel.recycle()
    }
}
