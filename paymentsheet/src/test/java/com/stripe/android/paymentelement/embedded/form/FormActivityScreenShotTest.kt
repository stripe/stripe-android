package com.stripe.android.paymentelement.embedded.form

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.common.taptoadd.FakeTapToAddHelper
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStateFixtures
import com.stripe.android.paymentelement.embedded.sheet.DefaultSheetActivityStateHolder
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedNavigator
import com.stripe.android.paymentelement.embedded.sheet.FakeSheetActivityConfirmationHelper
import com.stripe.android.paymentsheet.FakeCustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.ui.PaymentElementTheme
import com.stripe.android.paymentsheet.utils.ViewModelStoreOwnerContext
import com.stripe.android.paymentsheet.verticalmode.FakeSavedPaymentMethodConfirmInteractor
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.testing.LocaleTestRule
import com.stripe.android.utils.FakeIsNfcScanningAvailable
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.FakePaymentMethodMessagePromotionsHelper
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import java.util.Locale
import javax.inject.Provider

@OptIn(AppearanceAPIAdditionsPreview::class)
internal class FormActivityScreenShotTest {
    private val paparazziRule = PaparazziRule(
        PaymentSheetAppearance.entries,
        boxModifier = Modifier
            .padding(16.dp)
    )

    private val scopedThemePaparazziRule = PaparazziRule(
        SystemAppearance.entries,
        boxModifier = Modifier.padding(16.dp),
        includeStripeTheme = false,
    )

    private val localeRule = LocaleTestRule(Locale.US)

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(paparazziRule)
        .around(scopedThemePaparazziRule)
        .around(localeRule)

    @Test
    fun testFormActivity_enabled() {
        paparazziRule.snapshot {
            TestFormActivityUi(
                confirmationState = ConfirmationHandler.State.Idle,
                enabled = true
            )
        }
    }

    @Test
    fun testFormActivity_disabled() {
        paparazziRule.snapshot {
            TestFormActivityUi(
                confirmationState = ConfirmationHandler.State.Idle
            )
        }
    }

    @Test
    fun testFormActivity_processing() {
        paparazziRule.snapshot {
            TestFormActivityUi(
                confirmationState = confirmationStateConfirming(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
            )
        }
    }

    @Test
    fun testFormActivity_complete() {
        paparazziRule.snapshot {
            TestFormActivityUi(
                confirmationState = confirmationStateComplete(true)
            )
        }
    }

    @Test
    fun testFormActivity_error() {
        paparazziRule.snapshot {
            TestFormActivityUi(
                confirmationState = confirmationStateComplete(false)
            )
        }
    }

    @Test
    fun testFormActivity_usBankMandate() {
        paparazziRule.snapshot {
            TestFormActivityUi(
                confirmationState = ConfirmationHandler.State.Idle,
                usBankMandate = "This is a mandate".resolvableString
            )
        }
    }

    @Test
    fun testFormActivity_confirmSavedPaymentMethod() {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle())
        val confirmationHandler = FakeConfirmationHandler()
        val customerStateHolder = FakeCustomerStateHolder()
        val launchMode = EmbeddedLaunchMode.Form(
            selectedPaymentMethodCode = "card",
        )
        val stateHolder = DefaultSheetActivityStateHolder(
            paymentMethodMetadata = paymentMethodMetadata,
            selectionHolder = selectionHolder,
            configuration = EmbeddedConfirmationStateFixtures.defaultState().configuration,
            coroutineScope = TestScope(UnconfinedTestDispatcher()),
            onClickDelegate = OnClickDelegateOverrideImpl(),
            eventReporter = FakeEventReporter(),
            confirmationHandler = confirmationHandler,
            tapToAddHelper = FakeTapToAddHelper.noOp(),
            customerStateHolder = customerStateHolder,
            launchMode = launchMode,
            embeddedNavigatorProvider = Provider { error("Not expected") },
            savedPaymentMethodConfirmScreenFactoryProvider = Provider { error("Not expected") },
        )
        val screen = EmbeddedNavigator.Screen.SavedPaymentMethodConfirm(
            interactor = FakeSavedPaymentMethodConfirmInteractor(formEnabled = false),
            isLiveMode = paymentMethodMetadata.stripeIntent.isLiveMode,
            eventReporter = FakeEventReporter(),
            sheetActivityStateHolder = stateHolder,
            confirmationHelper = FakeSheetActivityConfirmationHelper(),
            embeddedSelectionHolder = selectionHolder,
            customerStateHolder = customerStateHolder,
            launchMode = launchMode,
        )

        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                screen.Content()
            }
        }
    }

    @Test
    fun testAutomaticTheme() {
        snapshotWithAppearance(PaymentSheet.Appearance())
    }

    @Test
    fun testAlwaysLightTheme() {
        snapshotWithAppearance(
            PaymentSheet.Appearance(themeMode = PaymentSheet.ThemeMode.AlwaysLight),
        )
    }

    @Test
    fun testAlwaysDarkTheme() {
        snapshotWithAppearance(
            PaymentSheet.Appearance(themeMode = PaymentSheet.ThemeMode.AlwaysDark),
        )
    }

    @Test
    fun testCustomAppearanceTheme() {
        snapshotWithAppearance(PaymentSheetAppearance.CrazyAppearance.appearance)
    }

    private fun snapshotWithAppearance(appearance: PaymentSheet.Appearance) {
        scopedThemePaparazziRule.snapshot {
            PaymentElementTheme(appearance = appearance) {
                Surface(color = MaterialTheme.colors.surface) {
                    TestFormActivityUi(
                        confirmationState = ConfirmationHandler.State.Idle,
                        enabled = true,
                    )
                }
            }
        }
    }

    @Composable
    private fun TestFormActivityUi(
        confirmationState: ConfirmationHandler.State,
        enabled: Boolean = false,
        usBankMandate: ResolvableString? = null,
    ) {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle())
        val confirmationHandler = FakeConfirmationHandler()
        confirmationHandler.state.value = confirmationState
        val stateHolder = DefaultSheetActivityStateHolder(
            paymentMethodMetadata = paymentMethodMetadata,
            selectionHolder = selectionHolder,
            configuration = EmbeddedConfirmationStateFixtures.defaultState().configuration,
            coroutineScope = TestScope(UnconfinedTestDispatcher()),
            onClickDelegate = OnClickDelegateOverrideImpl(),
            eventReporter = FakeEventReporter(),
            confirmationHandler = confirmationHandler,
            tapToAddHelper = FakeTapToAddHelper.noOp(),
            customerStateHolder = FakeCustomerStateHolder(),
            launchMode = EmbeddedLaunchMode.Form(
                selectedPaymentMethodCode = "card",
            ),
            embeddedNavigatorProvider = Provider { error("Not expected") },
            savedPaymentMethodConfirmScreenFactoryProvider = Provider { error("Not expected") },
        )
        val formHelperFactory = EmbeddedFormHelperFactory(
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
            embeddedSelectionHolder = selectionHolder,
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
            savedStateHandle = SavedStateHandle(),
            isNfcScanningAvailable = FakeIsNfcScanningAvailable(result = false),
        )
        val eventReporter = FakeEventReporter()
        val interactor = EmbeddedFormInteractorFactory(
            paymentMethodMetadata = paymentMethodMetadata,
            embeddedSelectionHolder = selectionHolder,
            embeddedFormHelperFactory = formHelperFactory,
            viewModelScope = TestScope(UnconfinedTestDispatcher()),
            sheetActivityStateHolder = stateHolder,
            tapToAddHelper = FakeTapToAddHelper.noOp(),
            eventReporter = eventReporter,
            paymentMethodMessagePromotionsHelper = FakePaymentMethodMessagePromotionsHelper()
        ).create(
            paymentMethodCode = "card",
            hasSavedPaymentMethods = false,
        )

        stateHolder.updateMandate(usBankMandate)
        val state by stateHolder.state.collectAsState()

        ViewModelStoreOwnerContext {
            Column {
                FormScreenContent(
                    interactor = interactor,
                    eventReporter = eventReporter,
                    onClick = {},
                    onProcessingCompleted = {},
                    state = state.copy(isEnabled = enabled),
                )
            }
        }
    }
}
