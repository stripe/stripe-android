package com.stripe.android.paymentsheet.paymentdatacollection

import android.content.Context
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.Address
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CountryCode
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheetActivity
import com.stripe.android.paymentsheet.PaymentSheetContract
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.PaymentSheetViewModel
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.databinding.FragmentPaymentsheetAddCardBinding
import com.stripe.android.paymentsheet.databinding.StripeBillingAddressLayoutBinding
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.FragmentConfigFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.AddPaymentMethodsFragmentFactory
import com.stripe.android.utils.TestUtils.idleLooper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CardDataCollectionFragmentTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        PaymentConfiguration.init(
            context,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @Test
    fun `required billing fields should not be visible`() {
        createFragment { _, viewBinding ->
            val billingBinding = StripeBillingAddressLayoutBinding.bind(viewBinding.billingAddress)
            assertThat(billingBinding.address1Divider.isVisible).isFalse()
            assertThat(billingBinding.address1Layout.isVisible).isFalse()
            assertThat(viewBinding.billingAddress.address1View.isVisible).isFalse()

            assertThat(billingBinding.address2Divider.isVisible).isFalse()
            assertThat(billingBinding.address2Layout.isVisible).isFalse()
            assertThat(viewBinding.billingAddress.address2View.isVisible).isFalse()

            assertThat(billingBinding.cityLayout.isVisible).isFalse()
            assertThat(viewBinding.billingAddress.cityView.isVisible).isFalse()

            assertThat(billingBinding.stateDivider.isVisible).isFalse()
            assertThat(billingBinding.stateLayout.isVisible).isFalse()
            assertThat(viewBinding.billingAddress.stateView.isVisible).isFalse()
        }
    }

    @Test
    fun `paymentMethodParams with valid input should return object with expected billing details`() {
        createFragment { fragment, viewBinding ->
            viewBinding.cardMultilineWidget.setCardNumber("4242424242424242")
            viewBinding.cardMultilineWidget.setExpiryDate(1, 2030)
            viewBinding.cardMultilineWidget.setCvcCode("123")
            viewBinding.billingAddress.countryView.setText("United States")
            viewBinding.billingAddress.postalCodeView.setText("94107")

            val paymentSelections = mutableListOf<PaymentSelection>()
            fragment.sheetViewModel.selection.observeForever { paymentSelection ->
                if (paymentSelection != null) {
                    paymentSelections.add(paymentSelection)
                }
            }

            val newCard = paymentSelections.first() as PaymentSelection.New.Card
            assertThat(newCard.paymentMethodCreateParams.billingDetails)
                .isEqualTo(
                    PaymentMethod.BillingDetails(
                        Address(
                            country = "US",
                            postalCode = "94107"
                        )
                    )
                )
        }
    }

    @Test
    fun `selection without customer config and valid card entered should create expected PaymentSelection`() {
        createFragment(PaymentSheetFixtures.ARGS_WITHOUT_CUSTOMER) { fragment, viewBinding ->
            viewBinding.saveCardCheckbox.isChecked = false

            var paymentSelection: PaymentSelection? = null
            fragment.sheetViewModel.selection.observeForever {
                paymentSelection = it
            }

            viewBinding.saveCardCheckbox.isChecked = true

            viewBinding.cardMultilineWidget.setCardNumber("4242424242424242")
            viewBinding.cardMultilineWidget.setExpiryDate(1, 2030)
            viewBinding.cardMultilineWidget.setCvcCode("123")
            viewBinding.billingAddress.countryView.setText("United States")
            viewBinding.billingAddress.postalCodeView.setText("94107")

            val newPaymentSelection = paymentSelection as PaymentSelection.New.Card
            assertThat(newPaymentSelection.shouldSavePaymentMethod)
                .isFalse()
            assertThat(fragment.sheetViewModel.newCard)
                .isEqualTo(paymentSelection)
        }
    }

    @Test
    fun `relaunching the fragment populates the fields`() {
        createFragment(PaymentSheetFixtures.ARGS_WITHOUT_CUSTOMER) { fragment, viewBinding ->
            fragment.sheetViewModel.newCard = PaymentSelection.New.Card(
                PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                CardBrand.Discover,
                false
            )

            viewBinding.saveCardCheckbox.isChecked = false

            var paymentSelection: PaymentSelection? = null
            fragment.sheetViewModel.selection.observeForever {
                paymentSelection = it
            }

            viewBinding.saveCardCheckbox.isChecked = true

            viewBinding.cardMultilineWidget.setCardNumber("4242424242424242")
            viewBinding.cardMultilineWidget.setExpiryDate(1, 2030)
            viewBinding.cardMultilineWidget.setCvcCode("123")
            viewBinding.billingAddress.countryView.setText("United States")
            viewBinding.billingAddress.postalCodeView.setText("94107")

            val newPaymentSelection = paymentSelection as PaymentSelection.New.Card
            assertThat(newPaymentSelection.shouldSavePaymentMethod)
                .isFalse()
        }

        createFragment(PaymentSheetFixtures.ARGS_WITHOUT_CUSTOMER) { fragment, viewBinding ->
            viewBinding.saveCardCheckbox.isChecked = false

            var paymentSelection: PaymentSelection? = null
            fragment.sheetViewModel.selection.observeForever {
                paymentSelection = it
            }

            viewBinding.saveCardCheckbox.isChecked = true

            viewBinding.cardMultilineWidget.setCardNumber("4242424242424242")
            viewBinding.cardMultilineWidget.setExpiryDate(1, 2030)
            viewBinding.cardMultilineWidget.setCvcCode("123")
            viewBinding.billingAddress.countryView.setText("United States")
            viewBinding.billingAddress.postalCodeView.setText("94107")

            val newPaymentSelection = paymentSelection as PaymentSelection.New.Card
            assertThat(newPaymentSelection.shouldSavePaymentMethod)
                .isFalse()
        }
    }

    @Test
    fun `selection when save card checkbox enabled and then valid card entered should create expected PaymentSelection`() {
        createFragment { fragment, viewBinding ->
            assertThat(viewBinding.saveCardCheckbox.isVisible)
                .isTrue()
            viewBinding.saveCardCheckbox.isChecked = false

            var paymentSelection: PaymentSelection? = null
            fragment.sheetViewModel.selection.observeForever {
                paymentSelection = it
            }

            viewBinding.saveCardCheckbox.isChecked = true

            viewBinding.cardMultilineWidget.setCardNumber("4242424242424242")
            viewBinding.cardMultilineWidget.setExpiryDate(1, 2030)
            viewBinding.cardMultilineWidget.setCvcCode("123")
            viewBinding.billingAddress.countryView.setText("United States")
            viewBinding.billingAddress.postalCodeView.setText("94107")

            val newPaymentSelection = paymentSelection as PaymentSelection.New.Card
            assertThat(newPaymentSelection.shouldSavePaymentMethod)
                .isTrue()
            assertThat(fragment.sheetViewModel.newCard)
                .isEqualTo(paymentSelection)
        }
    }

    @Test
    fun `selection when valid card entered and then save card checkbox enabled should create expected PaymentSelection`() {
        createFragment { fragment, viewBinding ->
            assertThat(viewBinding.saveCardCheckbox.isVisible)
                .isTrue()
            viewBinding.saveCardCheckbox.isChecked = false

            var paymentSelection: PaymentSelection? = null
            fragment.sheetViewModel.selection.observeForever {
                paymentSelection = it
            }

            viewBinding.cardMultilineWidget.setCardNumber("4242424242424242")
            viewBinding.cardMultilineWidget.setExpiryDate(1, 2030)
            viewBinding.cardMultilineWidget.setCvcCode("123")
            viewBinding.billingAddress.countryView.setText("United States")
            viewBinding.billingAddress.postalCodeView.setText("94107")

            viewBinding.saveCardCheckbox.isChecked = true

            val newPaymentSelection = paymentSelection as PaymentSelection.New.Card
            assertThat(newPaymentSelection.shouldSavePaymentMethod)
                .isTrue()

            assertThat(fragment.sheetViewModel.newCard?.brand)
                .isEqualTo(CardBrand.Visa)
        }
    }

    @Test
    fun `when processing SetupIntent should hide saveCardCheckbox`() {
        createFragment(stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD) { _, viewBinding ->
            assertThat(viewBinding.saveCardCheckbox.isVisible)
                .isFalse()
        }
    }

    @Test
    fun `checkbox text should reflect merchant display name`() {
        createFragment { _, viewBinding ->
            assertThat(viewBinding.saveCardCheckbox.text)
                .isEqualTo("Save this card for future Widget Store payments")
        }
    }

    @Test
    fun `cardErrors should react to input validity`() {
        createFragment { _, viewBinding ->
            assertThat(viewBinding.cardErrors.isVisible)
                .isFalse()

            viewBinding.cardMultilineWidget.setCardNumber("4242424242424249")
            viewBinding.cardMultilineWidget.setExpiryDate(1, 2010)
            assertThat(viewBinding.cardErrors.text)
                .isEqualTo("Your card's number is invalid.")
            assertThat(viewBinding.cardErrors.isVisible)
                .isTrue()

            viewBinding.cardMultilineWidget.setCardNumber("4242424242424242")
            assertThat(viewBinding.cardErrors.text)
                .isEqualTo("Your card's expiration year is invalid.")
            assertThat(viewBinding.cardErrors.isVisible)
                .isTrue()

            viewBinding.cardMultilineWidget.setExpiryDate(1, 2030)
            assertThat(viewBinding.cardErrors.text.toString())
                .isEmpty()
            assertThat(viewBinding.cardErrors.isVisible)
                .isFalse()
        }
    }

    @Test
    fun `make sure when add card fields are edited newcard is updated`() {
        createFragment { fragment, viewBinding ->
            assertThat(viewBinding.cardErrors.isVisible)
                .isFalse()

            viewBinding.cardMultilineWidget.setCardNumber("4242424242424242")
            viewBinding.cardMultilineWidget.setExpiryDate(1, 2030)
            viewBinding.cardMultilineWidget.setCvcCode("123")
            viewBinding.billingAddress.countryView.setText("United States")
            viewBinding.billingAddress.postalCodeView.setText("94107")

            viewBinding.saveCardCheckbox.isChecked = true

            assertThat(fragment.sheetViewModel.newCard?.brand)
                .isEqualTo(CardBrand.Visa)

            viewBinding.cardMultilineWidget.setCardNumber("378282246310005")

            assertThat(fragment.sheetViewModel.newCard?.brand)
                .isEqualTo(CardBrand.AmericanExpress)
        }
    }

    @Test
    fun `when postal code is valid then billing error is invisible`() {
        createFragment { _, viewBinding ->
            assertThat(viewBinding.cardErrors.isVisible)
                .isFalse()

            viewBinding.billingAddress.countryLayout.setSelectedCountryCode(CountryCode.US)
            viewBinding.billingAddress.postalCodeView.setText("94107")

            assertThat(viewBinding.billingErrors.text.toString())
                .isEmpty()
            assertThat(viewBinding.billingErrors.isVisible)
                .isFalse()
        }
    }

    @Test
    fun `when US zip code is invalid and losing focus then billing error is visible with correct error message`() {
        createFragment { _, viewBinding ->
            assertThat(viewBinding.cardErrors.isVisible)
                .isFalse()

            viewBinding.billingAddress.countryLayout.setSelectedCountryCode(CountryCode.US)
            viewBinding.billingAddress.postalCodeView.setText("123")
            requireNotNull(
                viewBinding.billingAddress.postalCodeView.getParentOnFocusChangeListener()
            ).onFocusChange(
                viewBinding.billingAddress.postalCodeView,
                false
            )
            idleLooper()

            assertThat(viewBinding.billingErrors.text.toString())
                .isEqualTo(context.getString(R.string.address_zip_invalid))
            assertThat(viewBinding.billingErrors.isVisible)
                .isTrue()
        }
    }

    @Test
    fun `when US zip code is valid and losing focus then billing error is invisible`() {
        createFragment { _, viewBinding ->
            assertThat(viewBinding.cardErrors.isVisible)
                .isFalse()

            viewBinding.billingAddress.countryLayout.setSelectedCountryCode(CountryCode.US)
            viewBinding.billingAddress.postalCodeView.setText("94107")
            requireNotNull(
                viewBinding.billingAddress.postalCodeView.getParentOnFocusChangeListener()
            ).onFocusChange(
                viewBinding.billingAddress.postalCodeView,
                false
            )
            idleLooper()

            assertThat(viewBinding.billingErrors.text.toString()).isEmpty()
            assertThat(viewBinding.billingErrors.isVisible)
                .isFalse()
        }
    }

    @Test
    fun `when Canada postal code is invalid and losing focus then billing error is visible with correct error message`() {
        createFragment { _, viewBinding ->
            assertThat(viewBinding.cardErrors.isVisible)
                .isFalse()

            viewBinding.billingAddress.countryLayout.setSelectedCountryCode(CountryCode.CA)
            viewBinding.billingAddress.postalCodeView.setText("!@#")
            requireNotNull(
                viewBinding.billingAddress.postalCodeView.getParentOnFocusChangeListener()
            ).onFocusChange(
                viewBinding.billingAddress.postalCodeView,
                false
            )
            idleLooper()

            assertThat(viewBinding.billingErrors.text.toString())
                .isEqualTo(context.getString(R.string.address_postal_code_invalid))
            assertThat(viewBinding.billingErrors.isVisible)
                .isTrue()
        }
    }

    @Test
    fun `when Canada postal code is valid and losing focus then billing error is invisible`() {
        createFragment { _, viewBinding ->
            assertThat(viewBinding.cardErrors.isVisible)
                .isFalse()

            viewBinding.billingAddress.countryLayout.setSelectedCountryCode(CountryCode.CA)
            viewBinding.billingAddress.postalCodeView.setText("A1G9Z9")
            requireNotNull(
                viewBinding.billingAddress.postalCodeView.getParentOnFocusChangeListener()
            ).onFocusChange(
                viewBinding.billingAddress.postalCodeView,
                false
            )
            idleLooper()

            assertThat(viewBinding.billingErrors.text.toString()).isEmpty()
            assertThat(viewBinding.billingErrors.isVisible)
                .isFalse()
        }
    }

    @Test
    fun `when zip code is empty and losing focus then billing error is invisible`() {
        createFragment { _, viewBinding ->
            assertThat(viewBinding.cardErrors.isVisible)
                .isFalse()

            viewBinding.billingAddress.countryLayout.setSelectedCountryCode(CountryCode.US)
            viewBinding.billingAddress.postalCodeView.setText("")
            requireNotNull(
                viewBinding.billingAddress.postalCodeView.getParentOnFocusChangeListener()
            ).onFocusChange(
                viewBinding.billingAddress.postalCodeView,
                false
            )
            idleLooper()

            assertThat(viewBinding.billingErrors.text.toString()).isEmpty()
            assertThat(viewBinding.billingErrors.isVisible)
                .isFalse()
        }
    }

    @Test
    fun `empty merchant display name shows correct message`() {
        createFragment(PaymentSheetFixtures.ARGS_WITHOUT_CUSTOMER) { _, viewBinding ->
            assertThat(viewBinding.saveCardCheckbox.text)
                .isEqualTo(
                    context.getString(
                        R.string.stripe_paymentsheet_save_this_card_with_merchant_name,
                        "com.stripe.android.paymentsheet.test"
                    )
                )
        }
    }

    @Test
    fun `non-empty merchant display name shows correct message`() {
        createFragment(PaymentSheetFixtures.ARGS_CUSTOMER_WITHOUT_GOOGLEPAY) { _, viewBinding ->
            assertThat(viewBinding.saveCardCheckbox.text)
                .isEqualTo(
                    context.getString(
                        R.string.stripe_paymentsheet_save_this_card_with_merchant_name,
                        PaymentSheetFixtures.MERCHANT_DISPLAY_NAME
                    )
                )
        }
    }

    private fun createFragment(
        args: PaymentSheetContract.Args = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY,
        fragmentConfig: FragmentConfig? = FragmentConfigFixtures.DEFAULT,
        stripeIntent: StripeIntent? = PaymentIntentFixtures.PI_WITH_SHIPPING,
        onReady: (CardDataCollectionFragment<PaymentSheetViewModel>, FragmentPaymentsheetAddCardBinding) -> Unit
    ) {
        val factory = AddPaymentMethodsFragmentFactory(
            PaymentSheetViewModel::class.java,
            PaymentSheetViewModel.Factory(
                { ApplicationProvider.getApplicationContext() },
                { args }
            )
        )
        launchFragmentInContainer<CardDataCollectionFragment<PaymentSheetViewModel>>(
            bundleOf(
                PaymentSheetActivity.EXTRA_FRAGMENT_CONFIG to fragmentConfig,
                PaymentSheetActivity.EXTRA_STARTER_ARGS to args
            ),
            R.style.StripePaymentSheetDefaultTheme,
            factory = factory,
            initialState = Lifecycle.State.INITIALIZED
        ).onFragment { fragment ->
            // Mock sheetViewModel loading the StripeIntent before the Fragment is created
            fragment.sheetViewModel.setStripeIntent(stripeIntent)
        }.moveToState(Lifecycle.State.STARTED)
            .onFragment { fragment ->
                onReady(
                    fragment,
                    FragmentPaymentsheetAddCardBinding.bind(
                        requireNotNull(fragment.view)
                    )
                )
            }
    }
}
