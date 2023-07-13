package com.stripe.android.customersheet

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.customersheet.CustomerAdapter.PaymentOption.Companion.toPaymentOption
import com.stripe.android.customersheet.StripeCustomerAdapter.Companion.CACHED_CUSTOMER_MAX_AGE_MILLIS
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.FakePrefsRepository
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.utils.FakeCustomerRepository
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCustomerSheetApi::class)
class CustomerAdapterTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        PaymentConfiguration.init(
            context = context,
            publishableKey = "pk_123",
        )
    }

    @Test
    fun `CustomerAdapter can be created`() {
        val customerEphemeralKeyProvider = CustomerEphemeralKeyProvider {
            CustomerAdapter.Result.success(
                CustomerEphemeralKey(
                    customerId = "cus_123",
                    ephemeralKey = "ek_123"
                )
            )
        }

        val setupIntentClientSecretProvider = SetupIntentClientSecretProvider {
            CustomerAdapter.Result.success("seti_123")
        }

        val adapter = CustomerAdapter.create(
            context = context,
            customerEphemeralKeyProvider = customerEphemeralKeyProvider,
            setupIntentClientSecretProvider = setupIntentClientSecretProvider
        )

        assertThat(adapter).isNotNull()
    }

    @Test
    fun `CustomerEphemeralKey is cached`() = runTest {
        val ephemeralKeyProviderCounter = AtomicInteger(0)
        val adapter = createAdapter(
            customerEphemeralKeyProvider = {
                ephemeralKeyProviderCounter.incrementAndGet()
                CustomerAdapter.Result.success(
                    CustomerEphemeralKey(
                        customerId = testScheduler.currentTime.toString(),
                        ephemeralKey = "ek_123"
                    )
                )
            },
            timeProvider = { testScheduler.currentTime }
        )

        advanceTimeBy(1)
        var customer = adapter.getCustomerEphemeralKey()
        assertThat(customer.getOrNull()?.customerId).isEqualTo("1")
        assertThat(ephemeralKeyProviderCounter.get()).isEqualTo(1)

        customer = adapter.getCustomerEphemeralKey()
        assertThat(customer.getOrNull()?.customerId).isEqualTo("1")
        assertThat(ephemeralKeyProviderCounter.get()).isEqualTo(1)
    }

    @Test
    fun `CustomerEphemeralKey is expired and updated`() = runTest {
        val ephemeralKeyProviderCounter = AtomicInteger(0)
        val adapter = createAdapter(
            customerEphemeralKeyProvider = {
                ephemeralKeyProviderCounter.incrementAndGet()
                CustomerAdapter.Result.success(
                    CustomerEphemeralKey(
                        customerId = testScheduler.currentTime.toString(),
                        ephemeralKey = "ek_123"
                    )
                )
            },
            timeProvider = { testScheduler.currentTime }
        )

        var customer = adapter.getCustomerEphemeralKey()
        assertThat(customer.getOrNull()?.customerId).isEqualTo("0")
        assertThat(ephemeralKeyProviderCounter.get()).isEqualTo(1)

        advanceTimeBy(CACHED_CUSTOMER_MAX_AGE_MILLIS)
        customer = adapter.getCustomerEphemeralKey()
        assertThat(customer.getOrNull()?.customerId).isEqualTo("0")
        assertThat(ephemeralKeyProviderCounter.get()).isEqualTo(1)

        advanceTimeBy(1)
        customer = adapter.getCustomerEphemeralKey()
        assertThat(customer.getOrNull()?.customerId)
            .isEqualTo("${CACHED_CUSTOMER_MAX_AGE_MILLIS + 1}")
        assertThat(ephemeralKeyProviderCounter.get()).isEqualTo(2)
    }

    @Test
    fun `retrievePaymentMethods returns success`() = runTest {
        val adapter = createAdapter(
            customerRepository = FakeCustomerRepository(
                paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            )
        )
        val paymentMethods = adapter.retrievePaymentMethods()
        assertThat(
            paymentMethods.getOrNull()
        ).contains(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
    }

    @Test
    fun `retrievePaymentMethods returns failure when customer is not fetched`() = runTest {
        val error = CustomerAdapter.Result.failure<CustomerEphemeralKey>(
            cause = Exception("Cannot get customer"),
            displayMessage = "Merchant says cannot get customer"
        )
        val adapter = createAdapter(
            customerEphemeralKeyProvider = { error }
        )
        val result = adapter.retrievePaymentMethods()
        assertThat((result.value as CustomerAdapter.Result.Failure).displayMessage)
            .isEqualTo("Merchant says cannot get customer")
    }

    @Test
    fun `attachPaymentMethod succeeds when the payment method is attached`() = runTest {
        val adapter = createAdapter(
            customerRepository = FakeCustomerRepository(
                onAttachPaymentMethod = {
                    Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
                }
            )
        )
        val result = adapter.attachPaymentMethod("pm_1234")
        assertThat(result.getOrNull()).isNotNull()
    }

    @Test
    fun `attachPaymentMethod fails with default message when the payment method couldn't be attached`() = runTest {
        val adapter = createAdapter(
            customerRepository = FakeCustomerRepository(
                onAttachPaymentMethod = {
                    Result.failure(
                        APIException(
                            message = "could not attach payment method",
                        )
                    )
                }
            )
        )
        val result = adapter.attachPaymentMethod("pm_1234")
        assertThat((result.value as CustomerAdapter.Result.Failure).displayMessage)
            .isEqualTo("Something went wrong")
    }

    @Test
    fun `attachPaymentMethod fails with Stripe message when the payment method couldn't be attached`() = runTest {
        val adapter = createAdapter(
            customerRepository = FakeCustomerRepository(
                onAttachPaymentMethod = {
                    Result.failure(
                        APIException(
                            message = "could not attach payment method",
                            stripeError = StripeError(message = "Unable to attach payment method")
                        )
                    )
                }
            )
        )
        val result = adapter.attachPaymentMethod("pm_1234")
        assertThat((result.value as CustomerAdapter.Result.Failure).displayMessage)
            .isEqualTo("Unable to attach payment method")
    }

    @Test
    fun `detachPaymentMethod succeeds when the payment method is detached`() = runTest {
        val adapter = createAdapter(
            customerRepository = FakeCustomerRepository(
                onDetachPaymentMethod = {
                    Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
                }
            )
        )
        val result = adapter.detachPaymentMethod("pm_1234")
        assertThat(result.getOrNull()).isEqualTo(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )
    }

    @Test
    fun `detachPaymentMethod fails with default message when the payment method couldn't be detached`() = runTest {
        val adapter = createAdapter(
            customerRepository = FakeCustomerRepository(
                onDetachPaymentMethod = {
                    Result.failure(
                        APIException(
                            message = "could not detach payment method",
                        )
                    )
                }
            )
        )
        val result = adapter.detachPaymentMethod("pm_1234")
        assertThat((result.value as CustomerAdapter.Result.Failure).displayMessage)
            .isEqualTo("Something went wrong")
    }

    @Test
    fun `detachPaymentMethod fails with Stripe message when the payment method couldn't be detached`() = runTest {
        val adapter = createAdapter(
            customerRepository = FakeCustomerRepository(
                onDetachPaymentMethod = {
                    Result.failure(
                        APIException(
                            message = "could not detach payment method",
                            stripeError = StripeError(message = "Unable to detach payment method")
                        )
                    )
                }
            )
        )
        val result = adapter.detachPaymentMethod("pm_1234")
        assertThat((result.value as CustomerAdapter.Result.Failure).displayMessage)
            .isEqualTo("Unable to detach payment method")
    }

    @Test
    fun `setSelectedPaymentMethodOption saves the selected payment method`() = runTest {
        val adapter = createAdapter(
            customerEphemeralKeyProvider = {
                CustomerAdapter.Result.success(
                    CustomerEphemeralKey(
                        customerId = "cus_123",
                        ephemeralKey = "ek_123"
                    )
                )
            },
            prefsRepositoryFactory = {
                DefaultPrefsRepository(
                    context = context,
                    customerId = it.customerId,
                    workContext = testScheduler
                )
            }
        )
        adapter.setSelectedPaymentOption(
            paymentOption = CustomerAdapter.PaymentOption.StripeId("pm_1234")
        )
        val result = adapter.retrieveSelectedPaymentOption()
        assertThat(result.getOrNull()).isEqualTo(
            CustomerAdapter.PaymentOption.StripeId("pm_1234")
        )
    }

    @Test
    fun `setSelectedPaymentMethodOption clears the saved payment method`() = runTest {
        val adapter = createAdapter(
            customerEphemeralKeyProvider = {
                CustomerAdapter.Result.success(
                    CustomerEphemeralKey(
                        customerId = "cus_123",
                        ephemeralKey = "ek_123"
                    )
                )
            },
            prefsRepositoryFactory = {
                DefaultPrefsRepository(
                    context = context,
                    customerId = it.customerId,
                    workContext = testScheduler
                )
            }
        )
        adapter.setSelectedPaymentOption(
            paymentOption = CustomerAdapter.PaymentOption.StripeId("pm_1234")
        )
        var result = adapter.retrieveSelectedPaymentOption()
        assertThat(result.getOrNull()).isEqualTo(
            CustomerAdapter.PaymentOption.StripeId("pm_1234")
        )

        adapter.setSelectedPaymentOption(
            paymentOption = null
        )
        result = adapter.retrieveSelectedPaymentOption()
        assertThat(result.getOrNull()).isEqualTo(null)
    }

    @Test
    fun `setSelectedPaymentMethodOption succeeds when payment selection was saved`() = runTest {
        val prefsRepository = FakePrefsRepository(
            setSavedSelectionResult = true
        )
        val adapter = createAdapter(
            customerEphemeralKeyProvider = {
                CustomerAdapter.Result.success(
                    CustomerEphemeralKey(
                        customerId = "cus_123",
                        ephemeralKey = "ek_123"
                    )
                )
            },
            prefsRepositoryFactory = {
                prefsRepository
            },
        )
        val result = adapter.setSelectedPaymentOption(
            paymentOption = CustomerAdapter.PaymentOption.StripeId("pm_1234")
        )
        assertThat(result.getOrNull())
            .isEqualTo(Unit)

        val paymentOptionResult = adapter.retrieveSelectedPaymentOption()
        assertThat(paymentOptionResult.getOrNull())
            .isEqualTo(CustomerAdapter.PaymentOption.StripeId("pm_1234"))
    }

    @Test
    fun `setSelectedPaymentMethodOption fails when payment selection was unable to be saved`() = runTest {
        val adapter = createAdapter(
            customerEphemeralKeyProvider = {
                CustomerAdapter.Result.success(
                    CustomerEphemeralKey(
                        customerId = "cus_123",
                        ephemeralKey = "ek_123"
                    )
                )
            },
            prefsRepositoryFactory = {
                FakePrefsRepository(
                    setSavedSelectionResult = false
                )
            },
        )
        val result = adapter.setSelectedPaymentOption(
            paymentOption = CustomerAdapter.PaymentOption.StripeId("pm_1234")
        )
        assertThat((result.value as CustomerAdapter.Result.Failure).cause.message)
            .isEqualTo("Unable to persist payment option StripeId(id=pm_1234)")
        assertThat(result.value.displayMessage)
            .isEqualTo("Something went wrong")
    }

    @Test
    fun `setSelectedPaymentMethodOption sets none when there is no selection`() = runTest {
        val adapter = createAdapter(
            customerEphemeralKeyProvider = {
                CustomerAdapter.Result.success(
                    CustomerEphemeralKey(
                        customerId = "cus_123",
                        ephemeralKey = "ek_123"
                    )
                )
            },
            prefsRepositoryFactory = {
                DefaultPrefsRepository(
                    context = context,
                    customerId = it.customerId,
                    workContext = testScheduler
                )
            }
        )
        adapter.setSelectedPaymentOption(
            paymentOption = null
        )
        val result = adapter.retrieveSelectedPaymentOption()
        assertThat(result.getOrNull()).isNull()
    }

    @Test
    fun `PersistablePaymentMethodOption to SavedSelection`() {
        assertThat(CustomerAdapter.PaymentOption.GooglePay.toSavedSelection())
            .isEqualTo(SavedSelection.GooglePay)
        assertThat(CustomerAdapter.PaymentOption.Link.toSavedSelection())
            .isEqualTo(SavedSelection.Link)
        assertThat(CustomerAdapter.PaymentOption.StripeId("pm_1234").toSavedSelection())
            .isEqualTo(SavedSelection.PaymentMethod("pm_1234"))
    }

    @Test
    fun `SavedSelection to PersistablePaymentMethodOption`() {
        assertThat(SavedSelection.GooglePay.toPaymentOption())
            .isEqualTo(CustomerAdapter.PaymentOption.GooglePay)
        assertThat(SavedSelection.Link.toPaymentOption())
            .isEqualTo(CustomerAdapter.PaymentOption.Link)
        assertThat(SavedSelection.PaymentMethod("pm_1234").toPaymentOption())
            .isEqualTo(CustomerAdapter.PaymentOption.StripeId("pm_1234"))
    }

    @Test
    fun `setupIntentClientSecretForCustomerAttach succeeds`() = runTest {
        val adapter = createAdapter(
            setupIntentClientSecretProvider = {
                CustomerAdapter.Result.success("seti_123")
            },
        )
        val result = adapter.setupIntentClientSecretForCustomerAttach()
        assertThat(result.getOrNull()).isEqualTo("seti_123")
    }

    @Test
    fun `setupIntentClientSecretForCustomerAttach fails when client secret cannot be retrieved`() = runTest {
        val adapter = createAdapter(
            setupIntentClientSecretProvider = {
                CustomerAdapter.Result.failure(
                    cause = Exception("some exception"),
                    displayMessage = "Couldn't get client secret"
                )
            },
        )
        val result = adapter.setupIntentClientSecretForCustomerAttach()
        assertThat((result.value as CustomerAdapter.Result.Failure).displayMessage)
            .isEqualTo("Couldn't get client secret")
    }

    @Test
    fun `setupIntentClientSecretForCustomerAttach fails when null and called`() = runTest {
        val adapter = createAdapter(
            setupIntentClientSecretProvider = null,
        )

        val error = assertFailsWith<IllegalArgumentException> {
            adapter.setupIntentClientSecretForCustomerAttach()
        }

        assertThat(error.message).isEqualTo(
            "setupIntentClientSecretProvider cannot be null"
        )
    }

    @Test
    fun `canCreateSetupIntents should return true if setupIntentClientSecretForCustomerAttach is not null`() {
        var adapter = createAdapter(
            setupIntentClientSecretProvider = null,
        )

        assertThat(adapter.canCreateSetupIntents).isFalse()

        adapter = createAdapter(
            setupIntentClientSecretProvider = { CustomerAdapter.Result.success("client_secret") },
        )

        assertThat(adapter.canCreateSetupIntents).isTrue()
    }

    @Test
    fun `toPaymentSelection returns the right results`() = runTest {
        val paymentMethodProvider: (paymentMethodId: String) -> PaymentMethod? = {
            if (it == PaymentMethodFixtures.CARD_PAYMENT_METHOD.id) {
                PaymentMethodFixtures.CARD_PAYMENT_METHOD
            } else {
                null
            }
        }
        val savedPaymentOption = CustomerAdapter.PaymentOption.StripeId(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.id!!
        )
        val savedSelection = savedPaymentOption.toPaymentSelection(paymentMethodProvider)
        assertThat(savedSelection)
            .isInstanceOf(PaymentSelection.Saved::class.java)

        val googlePaymentOption = CustomerAdapter.PaymentOption.GooglePay
        val googleSelection = googlePaymentOption.toPaymentSelection(paymentMethodProvider)
        assertThat(googleSelection)
            .isInstanceOf(PaymentSelection.GooglePay::class.java)

        val linkPaymentOption = CustomerAdapter.PaymentOption.Link
        val linkSelection = linkPaymentOption.toPaymentSelection(paymentMethodProvider)
        assertThat(linkSelection)
            .isInstanceOf(PaymentSelection.Link::class.java)

        val nullSavedPaymentOption = CustomerAdapter.PaymentOption.StripeId("id_123")
        val nullSavedSelection = nullSavedPaymentOption.toPaymentSelection(paymentMethodProvider)
        assertThat(nullSavedSelection)
            .isNull()
    }

    @Test
    fun `CustomerAdapter Result can be created using success`() {
        val result: CustomerAdapter.Result<String> = CustomerAdapter.Result.success("Hello")
        assertThat(result.value)
            .isEqualTo("Hello")
        assertThat(result.getOrNull())
            .isEqualTo("Hello")

        var newResult = result.map { "world" }
        assertThat(newResult.value)
            .isEqualTo("world")

        newResult = newResult.mapCatching { "Hello world" }
        assertThat(newResult.value)
            .isEqualTo("Hello world")

        newResult = newResult.fold(
            onSuccess = {
                CustomerAdapter.Result.success("Success")
            },
            onFailure = { cause, displayMessage ->
                CustomerAdapter.Result.failure(
                    cause = cause,
                    displayMessage = displayMessage
                )
            }
        )
        assertThat(newResult.value)
            .isEqualTo("Success")
    }

    @Test
    fun `CustomerAdapter Result can be created using failure`() {
        val result: CustomerAdapter.Result<String> = CustomerAdapter.Result.failure(
            cause = IllegalStateException("Illegal state"),
            displayMessage = "This is display message",
        )
        assertThat((result.value as CustomerAdapter.Result.Failure).displayMessage)
            .isEqualTo("This is display message")
        assertThat(result.getOrNull())
            .isNull()

        var newResult = result.map { "world" }
        assertThat(newResult.isFailure).isTrue()
        assertThat((newResult.value as CustomerAdapter.Result.Failure).displayMessage)
            .isEqualTo("This is display message")

        newResult = newResult.mapCatching { "Hello world" }
        assertThat(newResult.isFailure).isTrue()
        assertThat((newResult.value as CustomerAdapter.Result.Failure).displayMessage)
            .isEqualTo("This is display message")

        newResult = newResult.fold(
            onSuccess = {
                CustomerAdapter.Result.success("Success")
            },
            onFailure = { _, _ ->
                CustomerAdapter.Result.failure(
                    cause = IllegalStateException("Illegal state"),
                    displayMessage = "This is a new display message",
                )
            }
        )
        assertThat(newResult.isFailure).isTrue()
        assertThat((newResult.value as CustomerAdapter.Result.Failure).displayMessage)
            .isEqualTo("This is a new display message")
    }

    @Test
    fun `CustomerAdapter Result is compatible with StripeException`() {
        val result: CustomerAdapter.Result<String> = CustomerAdapter.Result.failure(
            cause = APIException(
                stripeError = StripeError(message = "Some API error")
            ),
            displayMessage = "There was a problem with Stripe",
        )
        assertThat((result.value as CustomerAdapter.Result.Failure).displayMessage)
            .isEqualTo("There was a problem with Stripe")

        var newResult = result.map {
            "New result"
        }
        assertThat((newResult.value as CustomerAdapter.Result.Failure).displayMessage)
            .isEqualTo("There was a problem with Stripe")

        newResult = result.mapCatching {
            "New result"
        }
        assertThat((newResult.value as CustomerAdapter.Result.Failure).displayMessage)
            .isEqualTo("There was a problem with Stripe")

        var stripeResult: CustomerAdapter.Result<String> = CustomerAdapter.Result.failure(
            cause = APIException(
                message = "Unlocalized message",
                stripeError = null,
            ),
            displayMessage = "There was a problem with Stripe",
        )
        assertThat((stripeResult.value as CustomerAdapter.Result.Failure).displayMessage)
            .isEqualTo("There was a problem with Stripe")

        stripeResult = CustomerAdapter.Result.failure(
            cause = APIException(
                message = "Unlocalized message"
            ),
            displayMessage = null,
        )
        assertThat((stripeResult.value as CustomerAdapter.Result.Failure).displayMessage)
            .isEqualTo(null)
    }

    private fun createAdapter(
        customerEphemeralKeyProvider: CustomerEphemeralKeyProvider =
            CustomerEphemeralKeyProvider {
                CustomerAdapter.Result.success(
                    CustomerEphemeralKey(
                        customerId = "cus_123",
                        ephemeralKey = "ek_123"
                    )
                )
            },
        setupIntentClientSecretProvider: SetupIntentClientSecretProvider? =
            SetupIntentClientSecretProvider {
                CustomerAdapter.Result.success("seti_123")
            },
        timeProvider: () -> Long = { 1L },
        customerRepository: CustomerRepository = FakeCustomerRepository(),
        prefsRepositoryFactory: (CustomerEphemeralKey) -> PrefsRepository = {
            FakePrefsRepository()
        }
    ): StripeCustomerAdapter {
        return StripeCustomerAdapter(
            context = context,
            customerEphemeralKeyProvider = customerEphemeralKeyProvider,
            setupIntentClientSecretProvider = setupIntentClientSecretProvider,
            timeProvider = timeProvider,
            customerRepository = customerRepository,
            prefsRepositoryFactory = prefsRepositoryFactory,
            workContext = testDispatcher
        )
    }
}
