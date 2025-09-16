package com.stripe.android.paymentelement.embedded.form

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

internal class FormActivityViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `inject calls component injection and bootstraps confirmation handler`() = runTest {
        createScenario(customViewModelScope = this) {
            viewModel.inject(activity)

            assertThat(subcomponent.injectTurbine.awaitItem()).isEqualTo(activity)
            assertThat(confirmationHandler.bootstrapTurbine.awaitItem().paymentMethodMetadata)
                .isEqualTo(paymentMethodMetadata)
        }
    }

    @Test
    fun `inject only bootstraps confirmation handler once`() = runTest {
        createScenario(customViewModelScope = this) {
            // Call inject twice
            viewModel.inject(activity)
            viewModel.inject(activity)

            // Verify component injection is called twice
            assertThat(subcomponent.injectTurbine.awaitItem()).isEqualTo(activity)
            assertThat(subcomponent.injectTurbine.awaitItem()).isEqualTo(activity)

            // But bootstrap should only be called once
            assertThat(confirmationHandler.bootstrapTurbine.awaitItem().paymentMethodMetadata)
                .isEqualTo(paymentMethodMetadata)
            confirmationHandler.bootstrapTurbine.ensureAllEventsConsumed()
        }
    }

    private suspend fun createScenario(
        customViewModelScope: CoroutineScope,
        confirmationHandler: FakeConfirmationHandler = FakeConfirmationHandler(),
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        block: suspend Scenario.() -> Unit
    ) {
        val activity = mock<FormActivity>()
        val subcomponent = FakeFormActivitySubComponent()
        val subcomponentFactory = FakeSubcomponentFactory(subcomponent)
        val component = FakeFormActivityViewModelComponent(subcomponentFactory)

        val viewModel = FormActivityViewModel(
            component = component,
            customViewModelScope = customViewModelScope,
            confirmationHandler = confirmationHandler,
            paymentMethodMetadata = paymentMethodMetadata
        )

        val scenario = Scenario(
            viewModel = viewModel,
            component = component,
            subcomponent = subcomponent,
            activity = activity,
            confirmationHandler = confirmationHandler,
            paymentMethodMetadata = paymentMethodMetadata
        )
        block(scenario)
    }

    private data class Scenario(
        val viewModel: FormActivityViewModel,
        val component: FormActivityViewModelComponent,
        val subcomponent: FakeFormActivitySubComponent,
        val activity: FormActivity,
        val confirmationHandler: FakeConfirmationHandler,
        val paymentMethodMetadata: PaymentMethodMetadata,
    )

    private class FakeSubcomponentFactory(
        private val subcomponent: FormActivitySubcomponent
    ) : FormActivitySubcomponent.Factory {
        val buildTurbine = Turbine<BuildCall>()

        override fun build(
            activityResultCaller: ActivityResultCaller,
            lifecycleOwner: LifecycleOwner
        ): FormActivitySubcomponent {
            buildTurbine.add(BuildCall(activityResultCaller, lifecycleOwner))
            return subcomponent
        }

        data class BuildCall(
            val activityResultCaller: ActivityResultCaller,
            val lifecycleOwner: LifecycleOwner,
        )
    }

    private class FakeFormActivitySubComponent : FormActivitySubcomponent {
        val injectTurbine = Turbine<FormActivity>()

        override fun inject(activity: FormActivity) {
            injectTurbine.add(activity)
        }
    }

    private class FakeFormActivityViewModelComponent(
        private val fakeSubcomponentFactory: FormActivitySubcomponent.Factory
    ) : FormActivityViewModelComponent {
        override val viewModel: FormActivityViewModel
            get() = throw UnsupportedOperationException("Not used in tests")
        override val selectionHolder: EmbeddedSelectionHolder
            get() = EmbeddedSelectionHolder(SavedStateHandle())
        override val subcomponentFactory: FormActivitySubcomponent.Factory
            get() = fakeSubcomponentFactory
    }
}
