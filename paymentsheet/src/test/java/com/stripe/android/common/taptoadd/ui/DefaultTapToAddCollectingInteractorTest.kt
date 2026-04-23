package com.stripe.android.common.taptoadd.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.spms.FakeLinkInlineSignupAvailability
import com.stripe.android.common.spms.LinkInlineSignupAvailability
import com.stripe.android.common.taptoadd.FakeTapToAddCollectionHandler
import com.stripe.android.common.taptoadd.TapToAddCollectionHandler
import com.stripe.android.common.taptoadd.TapToAddErrorMessage
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.TestFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.testing.FakeLogger
import com.stripe.android.testing.PaymentMethodFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class DefaultTapToAddCollectingInteractorTest {
    @Test
    fun `create launches collection with payment method metadata`() {
        val metadata = PaymentMethodMetadataFactory.create(isTapToAddSupported = true)

        runScenario(
            collectResult = TapToAddCollectionHandler.CollectionState.Collected(
                PaymentMethodFactory.card(last4 = "4242")
            ),
        ) {
            assertThat(collectionHandlerScenario.collectCalls.awaitItem()).isEqualTo(metadata)
            assertThat(fakeEventReporter.tapToAddStartedCalls.awaitItem()).isNotNull()
            assertThat(fakeEventReporter.tapToAddCardAddedCalls.awaitItem()).isFalse()
        }
    }

    @Test
    fun `onCollected is invoked when collection succeeds`() {
        val paymentMethod = PaymentMethodFactory.card(last4 = "4242")

        runScenario(
            collectResult = TapToAddCollectionHandler.CollectionState.Collected(paymentMethod),
        ) {
            assertThat(collectionHandlerScenario.collectCalls.awaitItem()).isNotNull()
            assertThat(fakeEventReporter.tapToAddStartedCalls.awaitItem()).isNotNull()
            assertThat(fakeEventReporter.tapToAddCardAddedCalls.awaitItem()).isFalse()
            assertThat(onCollected.awaitItem()).isEqualTo(paymentMethod)
        }
    }

    @Test
    fun `onCardAdded reports canCollectLinkInput when inline link signup is available`() {
        runScenario(
            collectResult = TapToAddCollectionHandler.CollectionState.Collected(
                PaymentMethodFactory.card(last4 = "4242")
            ),
            linkSignupAvailability = LinkInlineSignupAvailability.Result.Available(
                TestFactory.LINK_CONFIGURATION,
            ),
        ) {
            assertThat(collectionHandlerScenario.collectCalls.awaitItem()).isNotNull()
            assertThat(fakeEventReporter.tapToAddStartedCalls.awaitItem()).isNotNull()
            assertThat(fakeEventReporter.tapToAddCardAddedCalls.awaitItem()).isTrue()
        }
    }

    @Test
    fun `onFailedCollection is invoked with errorMessage when collection fails`() {
        val underlying = RuntimeException("underlying")
        val userError = TapToAddErrorMessage(
            title = "Connection failed".resolvableString,
            action = "Try again".resolvableString,
        )
        runScenario(
            collectResult = TapToAddCollectionHandler.CollectionState.FailedCollection(
                error = underlying,
                errorCode = TEST_ERROR_CODE,
                errorMessage = userError,
            ),
        ) {
            assertThat(collectionHandlerScenario.collectCalls.awaitItem()).isNotNull()
            assertThat(fakeEventReporter.tapToAddStartedCalls.awaitItem()).isNotNull()
            assertThat(fakeEventReporter.failedToAddCardWithTapToAddCalls.awaitItem())
                .isEqualTo(TEST_ERROR_CODE.value)
            assertThat(onFailedCollection.awaitItem()).isEqualTo(userError)
        }
    }

    @Test
    fun `onCanceled is invoked when collection is canceled`() {
        runScenario(
            collectResult = TapToAddCollectionHandler.CollectionState.Canceled,
        ) {
            assertThat(collectionHandlerScenario.collectCalls.awaitItem()).isNotNull()
            assertThat(fakeEventReporter.tapToAddStartedCalls.awaitItem()).isNotNull()
            assertThat(fakeEventReporter.tapToAddCanceledCalls.awaitItem())
                .isEqualTo(EventReporter.TapToAddCancelSource.CardCollection)
            assertThat(onCanceled.awaitItem()).isNotNull()
        }
    }

    @Test
    fun `onTapToAddNotSupported is invoked with errorMessage when device is unsupported`() {
        val exception = IllegalStateException("unsupported")
        val userError = TapToAddErrorMessage(
            title = "Unsupported device".resolvableString,
            action = "Try another device".resolvableString,
        )
        runScenario(
            collectResult = TapToAddCollectionHandler.CollectionState.UnsupportedDevice(
                error = exception,
                errorMessage = userError,
            ),
        ) {
            assertThat(collectionHandlerScenario.collectCalls.awaitItem()).isNotNull()
            assertThat(fakeEventReporter.tapToAddStartedCalls.awaitItem()).isNotNull()
            assertThat(fakeEventReporter.tapToAddAttemptWithUnsupportedDeviceCalls.awaitItem()).isNotNull()
            assertThat(onTapToAddNotSupported.awaitItem()).isEqualTo(userError)
        }
    }

    @Test
    fun `close should stop any responses from collection handler completion`() {
        val continueController = CompletableDeferred<Unit>()

        runScenario(
            continueController = continueController,
        ) {
            assertThat(collectionHandlerScenario.collectCalls.awaitItem()).isNotNull()
            assertThat(fakeEventReporter.tapToAddStartedCalls.awaitItem()).isNotNull()

            interactor.close()

            continueController.complete(Unit)

            onCollected.expectNoEvents()
            onFailedCollection.expectNoEvents()
            onTapToAddNotSupported.expectNoEvents()
            onCanceled.expectNoEvents()
            fakeEventReporter.tapToAddCardAddedCalls.expectNoEvents()
            fakeEventReporter.failedToAddCardWithTapToAddCalls.expectNoEvents()
            fakeEventReporter.tapToAddCanceledCalls.expectNoEvents()
            fakeEventReporter.tapToAddAttemptWithUnsupportedDeviceCalls.expectNoEvents()
        }
    }

    private fun runScenario(
        metadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(isTapToAddSupported = true),
        collectResult: TapToAddCollectionHandler.CollectionState =
            TapToAddCollectionHandler.CollectionState.Collected(PaymentMethodFactory.card(last4 = "4242")),
        continueController: Deferred<Unit> = CompletableDeferred(Unit),
        linkSignupAvailability: LinkInlineSignupAvailability.Result =
            LinkInlineSignupAvailability.Result.Unavailable,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val onCollected = Turbine<PaymentMethod>()
        val onFailedCollection = Turbine<TapToAddErrorMessage>()
        val onTapToAddNotSupported = Turbine<TapToAddErrorMessage>()
        val onCanceled = Turbine<Unit>()
        val fakeEventReporter = FakeEventReporter()

        FakeTapToAddCollectionHandler.test(collectResult, continueController) {
            val scenario = Scenario(
                interactor = DefaultTapToAddCollectingInteractor(
                    paymentMethodMetadata = metadata,
                    uiContext = coroutineContext,
                    ioContext = coroutineContext,
                    tapToAddCollectionHandler = handler,
                    linkInlineSignupAvailability = FakeLinkInlineSignupAvailability(linkSignupAvailability),
                    eventReporter = fakeEventReporter,
                    onCollected = { onCollected.add(it) },
                    onFailedCollection = { onFailedCollection.add(it) },
                    onTapToAddNotSupported = { onTapToAddNotSupported.add(it) },
                    onCanceled = { onCanceled.add(Unit) },
                    logger = FakeLogger(),
                ),
                onCollected = onCollected,
                onFailedCollection = onFailedCollection,
                onTapToAddNotSupported = onTapToAddNotSupported,
                onCanceled = onCanceled,
                collectionHandlerScenario = this,
                fakeEventReporter = fakeEventReporter,
            )
            block(scenario)
            fakeEventReporter.validate()
        }
    }

    private class Scenario(
        val interactor: TapToAddCollectingInteractor,
        val onCollected: ReceiveTurbine<PaymentMethod>,
        val onFailedCollection: ReceiveTurbine<TapToAddErrorMessage>,
        val onTapToAddNotSupported: ReceiveTurbine<TapToAddErrorMessage>,
        val onCanceled: ReceiveTurbine<Unit>,
        val collectionHandlerScenario: FakeTapToAddCollectionHandler.Scenario,
        val fakeEventReporter: FakeEventReporter,
    )

    private companion object {
        val TEST_ERROR_CODE = object : TapToAddCollectionHandler.ErrorCode {
            override val value: String = "test_error_code"
        }
    }
}
