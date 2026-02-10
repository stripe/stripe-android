package com.stripe.android.common.taptoadd

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal class FakeTapToAddHelper private constructor(
    override val hasPreviouslyAttemptedCollection: Boolean = false,
) : TapToAddHelper {
    private val registerCalls = Turbine<RegisterCall>()
    private val collectCalls = Turbine<PaymentMethodMetadata>()

    override val result: SharedFlow<TapToAddResult> =
        MutableSharedFlow<TapToAddResult>().asSharedFlow()

    override fun register(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
    ) {
        registerCalls.add(RegisterCall(activityResultCaller, lifecycleOwner))
    }

    override fun startPaymentMethodCollection(paymentMethodMetadata: PaymentMethodMetadata) {
        collectCalls.add(paymentMethodMetadata)
    }

    class RegisterCall(
        val activityResultCaller: ActivityResultCaller,
        val lifecycleOwner: LifecycleOwner,
    )

    class Scenario(
        val collectCalls: ReceiveTurbine<PaymentMethodMetadata>,
        val helper: TapToAddHelper,
    )

    class Factory private constructor() : TapToAddHelper.Factory {
        private val createCalls = Turbine<CreateCall>()

        override fun create(
            coroutineScope: CoroutineScope,
            tapToAddMode: TapToAddMode
        ): TapToAddHelper {
            createCalls.add(CreateCall(coroutineScope, tapToAddMode))

            return FakeTapToAddHelper.noOp()
        }

        class CreateCall(
            val coroutineScope: CoroutineScope,
            val tapToAddMode: TapToAddMode
        )

        class Scenario(
            val createCalls: ReceiveTurbine<CreateCall>,
            val tapToAddHelperFactory: TapToAddHelper.Factory,
        )

        companion object {
            suspend fun test(block: suspend Scenario.() -> Unit,) {
                val factory = Factory()

                block(
                    Scenario(
                        createCalls = factory.createCalls,
                        tapToAddHelperFactory = factory,
                    )
                )

                factory.createCalls.ensureAllEventsConsumed()
            }

            fun noOp() = Factory()
        }
    }

    companion object {
        suspend fun test(
            hasPreviouslyAttemptedCollection: Boolean = false,
            block: suspend Scenario.() -> Unit,
        ) {
            val helper = FakeTapToAddHelper(hasPreviouslyAttemptedCollection)

            block(
                Scenario(
                    helper = helper,
                    collectCalls = helper.collectCalls,
                )
            )

            helper.collectCalls.ensureAllEventsConsumed()
            helper.registerCalls.ensureAllEventsConsumed()
        }

        fun noOp() = FakeTapToAddHelper()
    }
}
