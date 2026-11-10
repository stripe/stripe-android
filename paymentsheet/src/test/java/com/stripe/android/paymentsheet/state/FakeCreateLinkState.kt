package com.stripe.android.paymentsheet.state

import app.cash.turbine.Turbine
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.link.TestFactory
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.ElementsSession

internal class FakeCreateLinkState(
    private val result: LinkStateResult = LinkState(
        configuration = TestFactory.LINK_CONFIGURATION,
        loginState = LinkState.LoginState.LoggedIn,
        signupModeResult = LinkSignupModeResult.AlreadyRegistered,
    ),
) : CreateLinkState {
    val calls = Turbine<Call>()

    override suspend fun invoke(
        elementsSession: ElementsSession,
        configuration: CommonConfiguration,
        initializationMode: PaymentElementLoader.InitializationMode,
        customerMetadata: CustomerMetadata?,
        clientAttributionMetadata: ClientAttributionMetadata,
        apiConfiguration: ApiConfiguration.State,
    ): LinkStateResult {
        calls.add(
            Call(
                elementsSession = elementsSession,
                configuration = configuration,
                initializationMode = initializationMode,
                customerMetadata = customerMetadata,
                clientAttributionMetadata = clientAttributionMetadata,
                apiConfiguration = apiConfiguration,
            )
        )
        return result
    }

    fun ensureAllEventsConsumed() {
        calls.ensureAllEventsConsumed()
    }

    data class Call(
        val elementsSession: ElementsSession,
        val configuration: CommonConfiguration,
        val initializationMode: PaymentElementLoader.InitializationMode,
        val customerMetadata: CustomerMetadata?,
        val clientAttributionMetadata: ClientAttributionMetadata,
        val apiConfiguration: ApiConfiguration.State,
    )
}
