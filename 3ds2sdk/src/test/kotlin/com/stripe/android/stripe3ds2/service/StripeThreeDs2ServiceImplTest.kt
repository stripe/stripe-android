package com.stripe.android.stripe3ds2.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripe3ds2.CertificateFixtures
import com.stripe.android.stripe3ds2.init.DefaultSecurityChecker
import com.stripe.android.stripe3ds2.init.ui.StripeUiCustomization
import com.stripe.android.stripe3ds2.observability.FakeErrorReporter
import com.stripe.android.stripe3ds2.security.PublicKeyFactory
import com.stripe.android.stripe3ds2.transaction.MessageVersionRegistry
import com.stripe.android.stripe3ds2.transaction.SdkTransactionId
import com.stripe.android.stripe3ds2.transaction.Transaction
import com.stripe.android.stripe3ds2.transaction.TransactionFactory
import com.stripe.android.stripe3ds2.utils.ImageCache
import com.stripe.android.stripe3ds2.views.Brand
import org.junit.runner.RunWith
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper.idleMainLooper
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class StripeThreeDs2ServiceImplTest {

    private val transactionFactory: TransactionFactory = mock()
    private val transaction: Transaction = mock()
    private val imageCache: ImageCache = mock()

    private val errorReporter = FakeErrorReporter()
    private val messageVersionRegistry = MessageVersionRegistry()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val publicKeyFactory = PublicKeyFactory(context, errorReporter)
    private val warnings = DefaultSecurityChecker().getWarnings()

    private val service: StripeThreeDs2Service by lazy {
        StripeThreeDs2ServiceImpl(
            messageVersionRegistry,
            imageCache,
            errorReporter,
            transactionFactory,
            publicKeyFactory,
            null,
            warnings
        )
    }

    @BeforeTest
    fun before() {
        whenever(
            transactionFactory.create(
                eq(DIRECTORY_SERVER_ID),
                any(),
                any(),
                anyOrNull(),
                any(),
                any(),
                any()
            )
        ).thenReturn(transaction)
    }

    @Test
    fun createTransaction_whenInitialized_shouldReturnTransaction() {
        val publicKey = publicKeyFactory.create(DIRECTORY_SERVER_ID)
        val transaction = service
            .createTransaction(
                SdkTransactionId.create(),
                DIRECTORY_SERVER_ID,
                messageVersionRegistry.current,
                true,
                "visa",
                emptyList(),
                publicKey,
                null,
                StripeUiCustomization()
            )
        assertThat(this.transaction)
            .isSameInstanceAs(transaction)

        verify(transactionFactory).create(
            eq(DIRECTORY_SERVER_ID),
            any(),
            eq(publicKey),
            isNull(),
            any(),
            any(),
            eq(Brand.Visa)
        )
    }

    @Test
    fun createTransaction_withBrand_shouldCreateTransactionWithBrand() {
        val transaction = service
            .createTransaction(
                SdkTransactionId.create(),
                DIRECTORY_SERVER_ID,
                messageVersionRegistry.current,
                true,
                "american_express",
                CertificateFixtures.ROOT_CERTS,
                CertificateFixtures.DS_CERTIFICATE_RSA.publicKey,
                null,
                StripeUiCustomization()
            )
        idleMainLooper()

        assertThat(this.transaction)
            .isSameInstanceAs(transaction)
        verify(transactionFactory).create(
            eq(DIRECTORY_SERVER_ID),
            eq(CertificateFixtures.ROOT_CERTS),
            eq(CertificateFixtures.DS_CERTIFICATE_RSA.publicKey),
            isNull(),
            any(),
            any(),
            eq(Brand.Amex),
        )
    }

    @Test
    fun cleanup_whenInitialized_shouldNotThrowException() {
        service.cleanup()
        verify(imageCache).clear()
    }

    @Test
    fun getPublicKey_shouldCreateAPublicKey() {
        val publicKeyExpected = publicKeyFactory.create(DIRECTORY_SERVER_ID)
        val publicKeyActual = service.getPublicKey(DIRECTORY_SERVER_ID)
        assertThat(publicKeyActual).isEqualTo(publicKeyExpected)
    }

    @Test
    fun getWarnings_shouldReturnDefaultWarnings() {
        assertThat(service.warnings).isEqualTo(warnings)
    }

    private companion object {
        private const val DIRECTORY_SERVER_ID = "F055545342"
    }
}
