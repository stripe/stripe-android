package com.stripe.tta.demo.network

import com.stripe.tta.demo.network.model.CreateSetupIntentRequest
import com.stripe.tta.demo.network.model.CreateSetupIntentResponse
import kotlin.coroutines.CoroutineContext

internal class CreateSetupIntentRequester(
    ioContext: CoroutineContext,
) : BasePlaygroundRequester<CreateSetupIntentRequest, CreateSetupIntentResponse>(
    path = "create_setup_intent",
    ioContext = ioContext,
    requestSerializer = CreateSetupIntentRequest.serializer(),
    responseDeserializer = CreateSetupIntentResponse.serializer(),
)
