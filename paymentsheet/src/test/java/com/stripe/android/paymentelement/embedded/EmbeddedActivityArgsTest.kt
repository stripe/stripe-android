package com.stripe.android.paymentelement.embedded

import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfigurationFactory
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.parcelize.parcelableCreator
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class EmbeddedActivityArgsTest {
    @Test
    fun `loading state preserves appearance through parceling`() {
        val appearance = PaymentSheet.Appearance(
            colorsLight = PaymentSheet.Colors.Builder.light().surface(0xFF123456.toInt()).build(),
        )

        val args = parcelRoundTrip(EmbeddedActivityArgs.PresentationState.Loading(appearance))

        val loading = args.presentationState as EmbeddedActivityArgs.PresentationState.Loading
        assertThat(loading.appearance).isEqualTo(appearance)
    }

    @Test
    fun `ready state preserves metadata and configuration through parceling`() {
        val metadata = PaymentMethodMetadataFactory.create()
        val configuration = EmbeddedConfigurationFactory.create(merchantDisplayName = "Ready merchant")
        val args = parcelRoundTrip(
            EmbeddedActivityArgs.PresentationState.Ready(
                paymentMethodMetadata = metadata,
                configuration = configuration,
            ),
        )

        val readyState = args.presentationState as EmbeddedActivityArgs.PresentationState.Ready
        assertThat(readyState.paymentMethodMetadata).isEqualTo(metadata)
        assertThat(readyState.configuration).isEqualTo(configuration)
    }

    private fun parcelRoundTrip(presentationState: EmbeddedActivityArgs.PresentationState): EmbeddedActivityArgs {
        val args = EmbeddedActivityArgs(
            productUsage = setOf("EmbeddedPaymentElement"),
            paymentElementCallbackIdentifier = "EmbeddedActivityArgsTest",
            statusBarColor = null,
            selection = null,
            previousNewSelections = Bundle(),
            customerState = null,
            linkAccountInfo = LinkAccountUpdate.Value(null),
            promotions = emptyList(),
            launchMode = EmbeddedLaunchMode.PaymentOptions,
            presentationState = presentationState,
        )
        val parcel = Parcel.obtain()
        try {
            args.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            val restoredArgs = parcelableCreator<EmbeddedActivityArgs>().createFromParcel(parcel)
            return requireNotNull(
                EmbeddedActivityArgs.fromIntent(Intent().putExtra(EmbeddedActivityArgs.EXTRA_ARGS, restoredArgs))
            )
        } finally {
            parcel.recycle()
        }
    }
}
