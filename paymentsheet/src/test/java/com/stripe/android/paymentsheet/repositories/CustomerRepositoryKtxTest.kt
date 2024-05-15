package com.stripe.android.paymentsheet.repositories

import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.utils.FakeCustomerRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.spy
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify

class CustomerRepositoryKtxTest {
    @Test
    fun `'detachPaymentMethodAndDuplicates' attempts to remove all duplicate cards with matching fingerprints`() =
        runTest {
            val fingerprint = "4343434343"
            val differentFingerprint = "5454545454"

            val cards = PaymentMethodFactory.cards(size = 5)
            val cardsWithFingerprints = cards.mapIndexed { index, paymentMethod ->
                if (index < 3) {
                    paymentMethod.copy(
                        card = paymentMethod.card?.copy(
                            fingerprint = fingerprint
                        )
                    )
                } else {
                    paymentMethod.copy(
                        card = paymentMethod.card?.copy(
                            fingerprint = differentFingerprint
                        )
                    )
                }
            }

            val repository = spy(
                FakeCustomerRepository(
                    paymentMethods = cardsWithFingerprints,
                    onDetachPaymentMethod = {
                        Result.success(cards.first())
                    }
                )
            )

            repository.detachPaymentMethodAndDuplicates(
                customerInfo = CustomerRepository.CustomerInfo(
                    id = "cus_1",
                    ephemeralKeySecret = "ephemeral_key_secret",
                ),
                paymentMethodId = cards[0].id!!
            )

            verify(repository).detachPaymentMethod(any(), eq(cards[0].id!!))
            verify(repository).detachPaymentMethod(any(), eq(cards[1].id!!))
            verify(repository).detachPaymentMethod(any(), eq(cards[2].id!!))
        }

    @Test
    fun `'detachPaymentMethodAndDuplicates' returns both successful and failed removals`() =
        runTest {
            val cards = PaymentMethodFactory.cards(size = 5).map { paymentMethod ->
                paymentMethod.copy(
                    card = paymentMethod.card?.copy(
                        fingerprint = "4343434343"
                    )
                )
            }

            val repository = FakeCustomerRepository(
                paymentMethods = cards,
                onDetachPaymentMethod = { paymentMethodId ->
                    if (paymentMethodId == cards[3].id || paymentMethodId == cards[4].id) {
                        Result.failure(TestException("Failed!"))
                    } else {
                        val card = cards.find { paymentMethod ->
                            paymentMethodId == paymentMethod.id
                        }!!

                        Result.success(card)
                    }
                }
            )

            val results = repository.detachPaymentMethodAndDuplicates(
                customerInfo = CustomerRepository.CustomerInfo(
                    id = "cus_1",
                    ephemeralKeySecret = "ephemeral_key_secret",
                ),
                paymentMethodId = cards.first().id!!
            )

            assertThat(results).containsExactly(
                PaymentMethodRemovalResult(
                    paymentMethodId = cards[0].id!!,
                    result = Result.success(cards[0])
                ),
                PaymentMethodRemovalResult(
                    paymentMethodId = cards[1].id!!,
                    result = Result.success(cards[1])
                ),
                PaymentMethodRemovalResult(
                    paymentMethodId = cards[2].id!!,
                    result = Result.success(cards[2])
                ),
                PaymentMethodRemovalResult(
                    paymentMethodId = cards[3].id!!,
                    result = Result.failure(TestException("Failed!"))
                ),
                PaymentMethodRemovalResult(
                    paymentMethodId = cards[4].id!!,
                    result = Result.failure(TestException("Failed!"))
                )
            )
        }

    @Test
    fun `'detachPaymentMethodAndDuplicates' returns payment method object received from removal operation`() =
        runTest {
            val cards = PaymentMethodFactory.cards(size = 2).map { paymentMethod ->
                paymentMethod.copy(
                    customerId = "cus_111",
                    card = paymentMethod.card?.copy(fingerprint = "4343434343"),
                )
            }

            val repository = FakeCustomerRepository(
                paymentMethods = cards,
                onDetachPaymentMethod = { paymentMethodId ->
                    Result.success(
                        cards.find { paymentMethod ->
                            paymentMethod.id == paymentMethodId
                        }!!.copy(
                            customerId = null
                        )
                    )
                }
            )

            val results = repository.detachPaymentMethodAndDuplicates(
                customerInfo = CustomerRepository.CustomerInfo(
                    id = "cus_1",
                    ephemeralKeySecret = "ephemeral_key_secret",
                ),
                paymentMethodId = cards.first().id!!
            )

            assertThat(results).containsExactly(
                PaymentMethodRemovalResult(
                    paymentMethodId = cards.first().id!!,
                    result = Result.success(cards.first().copy(customerId = null))
                ),
                PaymentMethodRemovalResult(
                    paymentMethodId = cards.last().id!!,
                    result = Result.success(cards.last().copy(customerId = null))
                )
            )
        }

    @Test
    fun `'detachPaymentMethodAndDuplicates' returns single removal failure if fails to fetch payment methods`() =
        runTest {
            val cards = PaymentMethodFactory.cards(size = 2)
            val repository = FakeCustomerRepository(
                paymentMethods = cards,
                onGetPaymentMethods = {
                    Result.failure(TestException("Failed!"))
                },
            )

            val results = repository.detachPaymentMethodAndDuplicates(
                customerInfo = CustomerRepository.CustomerInfo(
                    id = "cus_1",
                    ephemeralKeySecret = "ephemeral_key_secret",
                ),
                paymentMethodId = cards.first().id!!
            )

            assertThat(results).containsExactly(
                PaymentMethodRemovalResult(
                    paymentMethodId = cards.first().id!!,
                    result = Result.failure(TestException("Failed!"))
                ),
            )
        }

    @Test
    fun `'detachPaymentMethodAndDuplicates' returns success with no removals if no payment methods were found`() =
        runTest {
            val repository = FakeCustomerRepository(
                paymentMethods = listOf(),
            )

            val results = repository.detachPaymentMethodAndDuplicates(
                customerInfo = CustomerRepository.CustomerInfo(
                    id = "cus_1",
                    ephemeralKeySecret = "ephemeral_key_secret",
                ),
                paymentMethodId = "pm_1"
            )

            assertThat(results).isEmpty()
        }

    private data class TestException(override val message: String?) : Exception()
}
