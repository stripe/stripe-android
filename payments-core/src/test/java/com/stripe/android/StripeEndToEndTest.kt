package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.model.AccountParams
import com.stripe.android.model.AddressFixtures
import com.stripe.android.model.Card
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardFunding
import com.stripe.android.model.CardParamsFixtures
import com.stripe.android.model.DateOfBirth
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.SourceParams
import com.stripe.android.model.SourceTypeModel
import com.stripe.android.model.Token
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.utils.TestUtils.idleLooper
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class StripeEndToEndTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val defaultStripe = Stripe(context, ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)

    private val testDispatcher = UnconfinedTestDispatcher()

    @Test
    fun testCreateAccountToken() {
        val token = defaultStripe.createAccountTokenSynchronous(
            accountParams = AccountParams.create(
                tosShownAndAccepted = true,
                individual = AccountParams.BusinessTypeParams.Individual(
                    firstName = "Jenny",
                    lastName = "Rosen",
                    address = AddressFixtures.ADDRESS,
                    dateOfBirth = DateOfBirth(1, 1, 1990)
                )
            )
        )
        assertThat(requireNotNull(token).type)
            .isEqualTo(Token.Type.Account)
    }

    @Test
    fun createPaymentMethodSynchronous_withAuBecsDebit() {
        val paymentMethod =
            Stripe(context, ApiKeyFixtures.AU_BECS_DEBIT_PUBLISHABLE_KEY)
                .createPaymentMethodSynchronous(
                    PaymentMethodCreateParamsFixtures.AU_BECS_DEBIT
                )
        requireNotNull(paymentMethod)
        assertThat(paymentMethod.type)
            .isEqualTo(PaymentMethod.Type.AuBecsDebit)
    }

    @Test
    fun retrievePaymentIntentAsync_withInvalidClientSecret_shouldReturnInvalidRequestException() {
        val paymentIntentCallback: ApiResultCallback<PaymentIntent> = mock()
        createStripeWithTestScope().retrievePaymentIntent(
            clientSecret = "pi_abc_secret_invalid",
            callback = paymentIntentCallback
        )
        idleLooper()

        verify(paymentIntentCallback).onError(
            argWhere {
                it is InvalidRequestException && it.message == "No such payment_intent: 'pi_abc'"
            }
        )
    }

    @Test
    fun retrieveSetupIntentAsync_withInvalidClientSecret_shouldReturnInvalidRequestException() = runTest {
        val setupIntentCallback: ApiResultCallback<SetupIntent> = mock()

        createStripeWithTestScope().retrieveSetupIntent(
            clientSecret = "seti_abc_secret_invalid",
            callback = setupIntentCallback
        )
        idleLooper()

        verify(setupIntentCallback).onError(
            argWhere {
                it is InvalidRequestException && it.message == "No such setupintent: 'seti_abc'"
            }
        )
    }

    @Test
    fun `createPaymentMethod with CB cards should create expected Networks object`() {
        val stripe = Stripe(context, ApiKeyFixtures.CB_PUBLISHABLE_KEY)
        val createPaymentMethod = { number: String ->
            stripe.createPaymentMethodSynchronous(
                paymentMethodCreateParams = PaymentMethodCreateParams.create(
                    card = PaymentMethodCreateParams.Card(
                        number = number,
                        expiryMonth = 1,
                        expiryYear = 2045,
                        cvc = "123"
                    )
                )
            )
        }

        assertThat(createPaymentMethod("4000002500001001")?.card?.networks)
            .isEqualTo(
                PaymentMethod.Card.Networks(
                    available = setOf("cartes_bancaires", "visa"),
                    selectionMandatory = false,
                    preferred = null
                )
            )

        assertThat(createPaymentMethod("5555552500001001")?.card?.networks)
            .isEqualTo(
                PaymentMethod.Card.Networks(
                    available = setOf("cartes_bancaires", "mastercard"),
                    selectionMandatory = false,
                    preferred = null
                )
            )
    }

    @Test
    fun `create Source using CardParams should return object with expected data`() {
        val source = defaultStripe.createSourceSynchronous(
            SourceParams.createCardParams(CardParamsFixtures.DEFAULT)
        )
        assertThat(source?.sourceTypeModel)
            .isEqualTo(
                SourceTypeModel.Card(
                    addressLine1Check = "unchecked",
                    addressZipCheck = "unchecked",
                    brand = CardBrand.Visa,
                    country = "US",
                    cvcCheck = "unchecked",
                    expiryMonth = 12,
                    expiryYear = 2045,
                    last4 = "4242",
                    funding = CardFunding.Credit,
                    threeDSecureStatus = SourceTypeModel.Card.ThreeDSecureStatus.Optional
                )
            )
    }

    @Test
    fun `create card token using CardParams should return object with expected data`() {
        val token = defaultStripe.createCardTokenSynchronous(
            CardParamsFixtures.DEFAULT
        )
        val card = requireNotNull(token?.card)

        assertThat(card)
            .isEqualTo(
                Card(
                    expMonth = 12,
                    expYear = 2045,
                    id = card.id,
                    name = "Jenny Rosen",
                    last4 = "4242",
                    addressLine1 = "123 Market St",
                    addressLine1Check = "unchecked",
                    addressLine2 = "#345",
                    addressCity = "San Francisco",
                    addressState = "CA",
                    addressZip = "94107",
                    addressZipCheck = "unchecked",
                    addressCountry = "US",
                    brand = CardBrand.Visa,
                    funding = CardFunding.Credit,
                    country = "US",
                    currency = "usd",
                    cvcCheck = "unchecked"
                )
            )
    }

    @Test
    fun `Card objects should be populated with the expected CardBrand value`() {
        assertThat(
            listOf(
                CardNumberFixtures.AMEX_NO_SPACES to CardBrand.AmericanExpress,
                CardNumberFixtures.VISA_NO_SPACES to CardBrand.Visa,
                CardNumberFixtures.MASTERCARD_NO_SPACES to CardBrand.MasterCard,
                CardNumberFixtures.JCB_NO_SPACES to CardBrand.JCB,
                CardNumberFixtures.UNIONPAY_16_NO_SPACES to CardBrand.UnionPay,
                CardNumberFixtures.DISCOVER_NO_SPACES to CardBrand.Discover,
                CardNumberFixtures.DINERS_CLUB_14_NO_SPACES to CardBrand.DinersClub
            ).all { (cardNumber, cardBrand) ->
                val token = defaultStripe.createCardTokenSynchronous(
                    CardParamsFixtures.DEFAULT.copy(
                        number = cardNumber
                    )
                )
                token?.card?.brand == cardBrand
            }
        ).isTrue()
    }

    @Test
    fun `createRadarSession() should return a valid Radar Session id`() = runTest {
        val radarSession = createStripeWithTestScope().createRadarSession()
        assertThat(radarSession.id)
            .startsWith("rse_")
    }

    private fun createStripeWithTestScope(
        publishableKey: String = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
    ): Stripe {
        val stripeRepository = StripeApiRepository(
            context,
            { publishableKey },
            workContext = testDispatcher
        )
        return Stripe(
            stripeRepository = stripeRepository,
            paymentController = StripePaymentController.create(
                context,
                publishableKey,
                stripeRepository
            ),
            publishableKey = publishableKey,
            workContext = testDispatcher
        )
    }
}
