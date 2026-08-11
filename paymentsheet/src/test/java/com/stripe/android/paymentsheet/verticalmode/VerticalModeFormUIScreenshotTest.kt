package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.DefaultCardFundingFilter
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.LinkButtonState
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.VerticalModeForm
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.ui.PaymentSheetFlowType
import com.stripe.android.paymentsheet.ui.PaymentSheetScreen
import com.stripe.android.paymentsheet.utils.ViewModelStoreOwnerContext
import com.stripe.android.paymentsheet.viewmodels.FakeBaseSheetViewModel
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.flow.update
import org.junit.Rule
import org.junit.Test

internal class VerticalModeFormUIScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        boxModifier = Modifier
            .padding(16.dp)
    )

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Test
    fun cardFormIsDisplayed() {
        paparazziRule.snapshot {
            val metadata = PaymentMethodMetadataFactory.create()
            CreateTestScenario(paymentMethodCode = "card", metadata = metadata)
        }
    }

    @Test
    fun cardFieldsAreDisabledWhenProcessing() {
        paparazziRule.snapshot {
            val metadata = PaymentMethodMetadataFactory.create()
            CreateTestScenario(paymentMethodCode = "card", metadata = metadata, isProcessing = true)
        }
    }

    @Test
    fun fullCardForm() {
        val metadata = PaymentMethodMetadataFactory.create()
        val initialScreen = VerticalModeForm(
            FakeVerticalModeFormInteractor.create(
                paymentMethodCode = "card",
                metadata = metadata,
            )
        )
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = true)

        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
            }
        }
    }

    @Test
    fun fullCardFormWithValidation() {
        val metadata = PaymentMethodMetadataFactory.create()
        val initialScreen = VerticalModeForm(
            FakeVerticalModeFormInteractor.create(
                paymentMethodCode = "card",
                metadata = metadata,
                isValidating = true,
            )
        )
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = true)

        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
            }
        }
    }

    @Test
    fun fullCardFormWithAddressBillingDetails() {
        val metadata = PaymentMethodMetadataFactory.create()
        val initialScreen = VerticalModeForm(
            FakeVerticalModeFormInteractor.create(
                paymentMethodCode = "card",
                metadata = metadata.copy(
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
                        phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
                        email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                    )
                ),
            )
        )
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = true)

        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
            }
        }
    }

    @Test
    fun fullCardFormWithFullBillingDetails() {
        val metadata = PaymentMethodMetadataFactory.create()
        val initialScreen = VerticalModeForm(
            FakeVerticalModeFormInteractor.create(
                paymentMethodCode = "card",
                metadata = metadata.copy(
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                    )
                ),
            )
        )
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = true)

        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
            }
        }
    }

    @Test
    fun fullCardFormWithFullBillingDetailsAndValidation() {
        val metadata = PaymentMethodMetadataFactory.create()
        val initialScreen = VerticalModeForm(
            FakeVerticalModeFormInteractor.create(
                paymentMethodCode = "card",
                metadata = metadata.copy(
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                    )
                ),
                isValidating = true,
            )
        )
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = true)

        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
            }
        }
    }

    @Test
    fun fullCardFormWithZipAndContactInfo() {
        val metadata = PaymentMethodMetadataFactory.create()
        val initialScreen = VerticalModeForm(
            FakeVerticalModeFormInteractor.create(
                paymentMethodCode = "card",
                metadata = metadata.copy(
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
                    )
                ),
            )
        )
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = true)

        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
            }
        }
    }

    @Test
    fun fullCardFormWithZipAndContactInfoAndValidation() {
        val metadata = PaymentMethodMetadataFactory.create()
        val initialScreen = VerticalModeForm(
            FakeVerticalModeFormInteractor.create(
                paymentMethodCode = "card",
                metadata = metadata.copy(
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
                    )
                ),
                isValidating = true,
            )
        )
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = true)

        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
            }
        }
    }

    @Test
    fun fullCardFormWithAddressAndEmail() {
        val metadata = PaymentMethodMetadataFactory.create()
        val initialScreen = VerticalModeForm(
            FakeVerticalModeFormInteractor.create(
                paymentMethodCode = "card",
                metadata = metadata.copy(
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
                        phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
                        email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                    )
                ),
            )
        )
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = true)

        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
            }
        }
    }

    @Test
    fun fullCardFormWithAddressAndPhone() {
        val metadata = PaymentMethodMetadataFactory.create()
        val initialScreen = VerticalModeForm(
            FakeVerticalModeFormInteractor.create(
                paymentMethodCode = "card",
                metadata = metadata.copy(
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
                        phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                    )
                ),
            )
        )
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = true)

        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
            }
        }
    }

    @Test
    fun fullCardFormWithEmail() {
        val metadata = PaymentMethodMetadataFactory.create()
        val initialScreen = VerticalModeForm(
            FakeVerticalModeFormInteractor.create(
                paymentMethodCode = "card",
                metadata = metadata.copy(
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
                        phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
                        email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                    )
                ),
            )
        )
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = true)

        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
            }
        }
    }

    @Test
    fun fullCardFormWithPhone() {
        val metadata = PaymentMethodMetadataFactory.create()
        val initialScreen = VerticalModeForm(
            FakeVerticalModeFormInteractor.create(
                paymentMethodCode = "card",
                metadata = metadata.copy(
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
                        phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                    )
                ),
            )
        )
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = true)

        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
            }
        }
    }

    @Test
    fun fullCardFormWithPhoneAndEmail() {
        val metadata = PaymentMethodMetadataFactory.create()
        val initialScreen = VerticalModeForm(
            FakeVerticalModeFormInteractor.create(
                paymentMethodCode = "card",
                metadata = metadata.copy(
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
                        phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                    )
                ),
            )
        )
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = true)

        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
            }
        }
    }

    @Test
    fun fullCardFormWithError() {
        val metadata = PaymentMethodMetadataFactory.create()
        val initialScreen = VerticalModeForm(
            FakeVerticalModeFormInteractor.create(
                paymentMethodCode = "card",
                metadata = metadata,
            )
        )
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = true)
        viewModel.onError("An error occurred.".resolvableString)

        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
            }
        }
    }

    @Suppress("ThrowsCount")
    @Test
    fun fullCardFormWithLink() {
        val metadata = PaymentMethodMetadataFactory.create()
        val initialScreen = VerticalModeForm(
            FakeVerticalModeFormInteractor.create(
                paymentMethodCode = "card",
                metadata = metadata,
            ),
            showsWalletHeader = true,
        )
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = false)
        viewModel.walletsStateSource.value = WalletsState(
            link = WalletsState.Link(
                state = LinkButtonState.Default,
                linkBrand = LinkBrand.Link,
            ),
            googlePay = null,
            buttonsEnabled = true,
            dividerTextResource = R.string.stripe_paymentsheet_or_pay_with_card,
            onGooglePayPressed = { throw AssertionError("Not expected.") },
            onLinkPressed = { throw AssertionError("Not expected.") },
            walletsAllowedInHeader = WalletType.entries,
            cardFundingFilter = DefaultCardFundingFilter,
            cardBrandFilter = DefaultCardBrandFilter,
        )

        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
            }
        }
    }

    @Test
    fun fullCardFormWithSaveForLater() {
        val metadata = PaymentMethodMetadataFactory.create(
            hasCustomerConfiguration = true
        )
        val initialScreen = VerticalModeForm(
            FakeVerticalModeFormInteractor.create(
                paymentMethodCode = "card",
                metadata = metadata,
            )
        )
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = true)

        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
            }
        }
    }

    @Test
    fun fullCashAppForm() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("cashapp"),
            ),
        )
        val initialScreen = VerticalModeForm(
            FakeVerticalModeFormInteractor.create(
                paymentMethodCode = "cashapp",
                metadata = metadata,
            )
        )
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = false)
        viewModel.primaryButtonUiStateSource.update { original ->
            original?.copy(enabled = true)
        }

        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
            }
        }
    }

    @Test
    fun klarnaFullBillingAddressForm() {
        paparazziRule.snapshot {
            CreateTestScenario(
                paymentMethodCode = "klarna",
                metadata = lpmMetadata(
                    paymentMethodCode = "klarna",
                    addressCollectionMode =
                        PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                    automaticTaxEnabled = false,
                    collectContacts = false,
                ),
            )
        }
    }

    @Test
    fun weroFullBillingAddressFormWithContacts() {
        paparazziRule.snapshot {
            CreateTestScenario(
                paymentMethodCode = "wero",
                metadata = lpmMetadata(
                    paymentMethodCode = "wero",
                    addressCollectionMode =
                        PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                    automaticTaxEnabled = false,
                    collectContacts = true,
                ),
            )
        }
    }

    @Test
    fun cashAppAutomaticTaxBillingAddressForm() {
        paparazziRule.snapshot {
            CreateTestScenario(
                paymentMethodCode = "cashapp",
                metadata = lpmMetadata(
                    paymentMethodCode = "cashapp",
                    addressCollectionMode =
                        PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
                    automaticTaxEnabled = true,
                    collectContacts = false,
                ),
            )
        }
    }

    private fun lpmMetadata(
        paymentMethodCode: PaymentMethodCode,
        addressCollectionMode: PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode,
        automaticTaxEnabled: Boolean,
        collectContacts: Boolean,
    ): PaymentMethodMetadata {
        val contactMode = if (collectContacts) {
            PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
        } else {
            PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic
        }
        return PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf(paymentMethodCode),
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = contactMode,
                email = contactMode,
                phone = contactMode,
                address = addressCollectionMode,
                allowedCountries = setOf("US"),
            ),
            defaultBillingDetails = PaymentSheet.BillingDetails(
                address = PaymentSheet.Address(country = "US"),
            ),
            checkoutSessionResponse = if (automaticTaxEnabled) {
                CheckoutSessionResponseFactory.create(
                    automaticTaxEnabled = true,
                    taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
                )
            } else {
                null
            },
        )
    }

    @Test
    fun cashappShowsBillingFields() {
        paparazziRule.snapshot {
            val metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "cashapp"),
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                )
            )
            CreateTestScenario(paymentMethodCode = "cashapp", metadata = metadata)
        }
    }

    @Composable
    private fun CreateTestScenario(
        paymentMethodCode: PaymentMethodCode,
        metadata: PaymentMethodMetadata,
        isProcessing: Boolean = false,
        showsWalletHeader: Boolean = false,
    ) {
        ViewModelStoreOwnerContext {
            VerticalModeFormUI(
                interactor = FakeVerticalModeFormInteractor.create(
                    paymentMethodCode = paymentMethodCode,
                    metadata = metadata,
                    isProcessing = isProcessing,
                ),
                showsWalletHeader = showsWalletHeader,
            )
        }
    }
}
