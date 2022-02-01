package com.stripe.android.stripe3ds2.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripe3ds2.CertificateFixtures
import com.stripe.android.stripe3ds2.init.ui.StripeUiCustomization
import com.stripe.android.stripe3ds2.observability.FakeErrorReporter
import com.stripe.android.stripe3ds2.security.PublicKeyFactory
import com.stripe.android.stripe3ds2.transaction.MessageVersionRegistry
import com.stripe.android.stripe3ds2.transaction.SdkTransactionId
import com.stripe.android.stripe3ds2.transaction.Transaction
import com.stripe.android.stripe3ds2.transaction.TransactionFactory
import com.stripe.android.stripe3ds2.utils.ImageCache
import com.stripe.android.stripe3ds2.views.Brand
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class StripeThreeDs2ServiceImplTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private val transactionFactory: TransactionFactory = mock()
    private val transaction: Transaction = mock()
    private val imageCache: ImageCache = mock()

    private val errorReporter = FakeErrorReporter()
    private val messageVersionRegistry = MessageVersionRegistry()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val publicKeyFactory = PublicKeyFactory(context, errorReporter)

    private val service: StripeThreeDs2Service by lazy {
        StripeThreeDs2ServiceImpl(
            messageVersionRegistry,
            imageCache,
            errorReporter,
            transactionFactory
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

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
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

    private companion object {
        private const val DIRECTORY_SERVER_ID = "F000000000"
    }
}
