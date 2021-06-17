package com.stripe.android

import com.stripe.android.model.PaymentMethod
import com.stripe.android.testharness.TestEphemeralKeyProvider
import org.junit.runner.RunWith
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.net.HttpURLConnection
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test class for [EphemeralKeyManager].
 */
@RunWith(RobolectricTestRunner::class)
class EphemeralKeyManagerTest {

    private val keyManagerListener: EphemeralKeyManager.KeyManagerListener = mock()

    private val operationIdFactory = OperationIdFactory.get()

    private val operationCaptor: KArgumentCaptor<EphemeralOperation> = argumentCaptor()
    private val ephemeralKeyArgumentCaptor: KArgumentCaptor<EphemeralKey> = argumentCaptor()
    private val testEphemeralKeyProvider: TestEphemeralKeyProvider = TestEphemeralKeyProvider()

    @Test
    fun shouldRefreshKey_whenKeyIsNullAndTimeIsInFuture_returnsTrue() {
        val futureTime = Calendar.getInstance().apply {
            add(Calendar.YEAR, 1)
        }.timeInMillis
        assertTrue(
            createEphemeralKeyManager(timeSupplier = { futureTime })
                .shouldRefreshKey(null)
        )
    }

    @Test
    fun shouldRefreshKey_whenKeyIsNullAndTimeIsInPast_returnsTrue() {
        val pastTime = Calendar.getInstance().apply {
            add(Calendar.YEAR, -1)
        }.timeInMillis
        assertTrue(
            createEphemeralKeyManager(timeSupplier = { pastTime })
                .shouldRefreshKey(null)
        )
    }

    @Test
    fun shouldRefreshKey_whenKeyExpiryIsAfterBufferFromPresent_returnsFalse() {
        val currentTime =
            TimeUnit.SECONDS.toMillis(DEFAULT_EXPIRES + 2 * TEST_SECONDS_BUFFER)
        val key = EphemeralKeyFixtures.FIRST.copy(expires = currentTime)

        assertFalse(
            createEphemeralKeyManager(timeSupplier = { currentTime })
                .shouldRefreshKey(key)
        )
    }

    @Test
    fun shouldRefreshKey_whenKeyExpiryIsInThePast_returnsTrue() {
        val currentTime = Calendar.getInstance().apply {
            timeInMillis -= 100L
        }.timeInMillis
        val key = EphemeralKeyFixtures.FIRST.copy(
            expires = TimeUnit.MILLISECONDS.toSeconds(currentTime)
        )
        assertTrue(
            createEphemeralKeyManager(timeSupplier = { currentTime })
                .shouldRefreshKey(key)
        )
    }

    @Test
    fun shouldRefreshKey_whenKeyExpiryIsInFutureButWithinBuffer_returnsTrue() {
        val parsedExpiryTimeInMillis = TimeUnit.SECONDS
            .toMillis(EphemeralKeyFixtures.FIRST.expires)
        val bufferTimeInMillis = TimeUnit.SECONDS.toMillis(TEST_SECONDS_BUFFER)

        val notFarEnoughInTheFuture = parsedExpiryTimeInMillis + bufferTimeInMillis / 2

        assertTrue(
            createEphemeralKeyManager(timeSupplier = { notFarEnoughInTheFuture })
                .shouldRefreshKey(EphemeralKeyFixtures.FIRST)
        )
    }

    @Test
    fun createKeyManager_updatesEphemeralKey_notifiesListener() {
        testEphemeralKeyProvider
            .setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        createEphemeralKeyManager(operationIdFactory)

        verify(keyManagerListener).onKeyUpdate(
            ephemeralKeyArgumentCaptor.capture(),
            any<EphemeralOperation.RetrieveKey>()
        )
        val ephemeralKey = ephemeralKeyArgumentCaptor.firstValue
        assertEquals(EphemeralKeyFixtures.FIRST.id, ephemeralKey.id)
    }

    @Test
    fun retrieveEphemeralKey_whenUpdateNecessary_returnsUpdateAndArguments() {
        testEphemeralKeyProvider
            .setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)

        val keyManager = createEphemeralKeyManager(operationIdFactory)

        val operation = EphemeralOperation.Customer.GetPaymentMethods(
            type = PaymentMethod.Type.Card,
            id = operationIdFactory.create(),
            productUsage = emptySet()
        )
        keyManager.retrieveEphemeralKey(operation)

        verify(keyManagerListener, times(2))
            .onKeyUpdate(
                ephemeralKeyArgumentCaptor.capture(),
                operationCaptor.capture()
            )

        // first retrieve the key
        assertNotNull(ephemeralKeyArgumentCaptor.firstValue)
        assertTrue(operationCaptor.firstValue is EphemeralOperation.RetrieveKey)

        // then perform the operation
        assertNotNull(ephemeralKeyArgumentCaptor.secondValue)
        assertEquals(operation, operationCaptor.secondValue)
    }

    @Test
    fun updateKeyIfNecessary_whenReturnsError_setsExistingKeyToNull() {
        testEphemeralKeyProvider
            .setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        val keyManager = createEphemeralKeyManager(operationIdFactory) {
            TimeUnit.SECONDS.toMillis(EphemeralKeyFixtures.FIRST.expires) + 1L
        }

        // Make sure we're in a good state
        verify(keyManagerListener)
            .onKeyUpdate(
                ephemeralKeyArgumentCaptor.capture(),
                any<EphemeralOperation.RetrieveKey>()
            )
        assertNotNull(ephemeralKeyArgumentCaptor.firstValue)

        // Set up the error
        val errorMessage = "This is an error"
        testEphemeralKeyProvider.setNextError(404, errorMessage)

        // It should be necessary to update because the key is expired.
        val operationId = operationIdFactory.create()
        keyManager.retrieveEphemeralKey(
            EphemeralOperation.RetrieveKey(
                id = operationId,
                productUsage = emptySet()
            )
        )

        verify(keyManagerListener)
            .onKeyError(operationId, 404, errorMessage)
        verifyNoMoreInteractions(keyManagerListener)
    }

    @Test
    fun triggerCorrectErrorOnInvalidRawKey() {
        val operationId = "12345"
        val operationIdFactory: OperationIdFactory = mock()
        whenever(operationIdFactory.create()).thenReturn(operationId)

        testEphemeralKeyProvider.setNextRawEphemeralKey("Not_a_JSON")
        createEphemeralKeyManager(operationIdFactory)

        verify(keyManagerListener, never())
            .onKeyUpdate(
                any(),
                any<EphemeralOperation.RetrieveKey>()
            )
        verify(keyManagerListener).onKeyError(
            eq(operationId),
            eq(HttpURLConnection.HTTP_INTERNAL_ERROR),
            argWhere { errorMessage ->
                errorMessage.startsWith(
                    "Received an ephemeral key that could not be parsed. See https://stripe.com/docs/mobile/android/basic for more details."
                )
            }
        )
    }

    @Test
    fun triggerCorrectErrorOnInvalidJsonKey() {
        val operationId = "12345"
        val operationIdFactory = OperationIdFactory { operationId }

        testEphemeralKeyProvider.setNextRawEphemeralKey("{}")
        createEphemeralKeyManager(operationIdFactory)

        verify(keyManagerListener, never())
            .onKeyUpdate(
                any(),
                any<EphemeralOperation.RetrieveKey>()
            )
        verify(keyManagerListener).onKeyError(
            eq(operationId),
            eq(HttpURLConnection.HTTP_INTERNAL_ERROR),
            argWhere { errorMessage ->
                errorMessage.startsWith(
                    "Received an ephemeral key that could not be parsed. See https://stripe.com/docs/mobile/android/basic for more details."
                )
            }
        )
    }

    @Test
    fun triggerCorrectErrorOnEmptyKey() {
        val operationId = "12345"
        val operationIdFactory = OperationIdFactory { operationId }

        testEphemeralKeyProvider.setNextRawEphemeralKey("")
        createEphemeralKeyManager(operationIdFactory)

        verify(keyManagerListener, never())
            .onKeyUpdate(
                any(),
                any<EphemeralOperation.RetrieveKey>()
            )
        verify(keyManagerListener).onKeyError(
            eq(operationId),
            eq(HttpURLConnection.HTTP_INTERNAL_ERROR),
            argWhere { errorMessage ->
                errorMessage.startsWith(
                    "Received an ephemeral key that could not be parsed. See https://stripe.com/docs/mobile/android/basic for more details."
                )
            }
        )
    }

    @Test
    fun init_whenShouldPrefetchEphemeralKeyIsFalse_shouldNotFetch() {
        val operationIdFactory: OperationIdFactory = mock()
        EphemeralKeyManager(
            testEphemeralKeyProvider,
            keyManagerListener,
            operationIdFactory,
            false,
            timeBufferInSeconds = TEST_SECONDS_BUFFER
        )
        verify(operationIdFactory, never()).create()
    }

    private fun createEphemeralKeyManager(
        operationIdFactory: OperationIdFactory = OperationIdFactory.get(),
        timeSupplier: TimeSupplier = { Calendar.getInstance().timeInMillis }
    ): EphemeralKeyManager {
        return EphemeralKeyManager(
            testEphemeralKeyProvider,
            keyManagerListener,
            operationIdFactory,
            true,
            timeSupplier,
            TEST_SECONDS_BUFFER
        )
    }

    private companion object {
        private const val TEST_SECONDS_BUFFER = 10L
        private const val DEFAULT_EXPIRES = 1501199335L
    }
}
