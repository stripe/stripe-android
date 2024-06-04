package com.stripe.android.paymentmethodmessaging

import android.graphics.Bitmap
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodMessage
import com.stripe.android.paymentmethodmessaging.view.PaymentMethodMessageMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentMethodMessageMapperTest {

    private val paymentMethodMessage = mock<PaymentMethodMessage>().apply {
        whenever(displayHtml).thenReturn("html")
        whenever(learnMoreUrl).thenReturn("url")
    }

    @Test
    fun `mapper maps message to data`() = runTest {
        val mapper = PaymentMethodMessageMapper(
            config = mock(),
            imageLoader = mock(),
        )

        val data = mapper.mapAsync(
            this,
            paymentMethodMessage,
            imageGetter = suspend {
                mapOf()
            }
        ).await()

        assertThat(data.message.displayHtml)
            .isEqualTo(paymentMethodMessage.displayHtml)
        assertThat(data.message.learnMoreUrl)
            .isEqualTo(paymentMethodMessage.learnMoreUrl)
        assertThat(data.images)
            .isEqualTo(emptyMap<String, Bitmap>())
    }
}
