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

internal class FakeTapToAddHelper(
    override val hasPreviouslyAttemptedCollection: Boolean = false,
) : TapToAddHelper {
    val registerCalls = Turbine<RegisterCall>()
    val collectCalls = Turbine<PaymentMethodMetadata>()

    private val _result = MutableSharedFlow<TapToAddResult>()
    override val result: SharedFlow<TapToAddResult> = _result.asSharedFlow()

    suspend fun emitResult(result: TapToAddResult) {
        _result.emit(result)
    }

    override fun register(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
    ) {
        registerCalls.add(RegisterCall(activityResultCaller, lifecycleOwner))
    }

    override fun startPaymentMethodCollection(paymentMethodMetadata: PaymentMethodMetadata) {
        collectCalls.add(paymentMethodMetadata)
    }

    fun validate() {
        registerCalls.ensureAllEventsConsumed()
        collectCalls.ensureAllEventsConsumed()
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
        private var createdHelper: FakeTapToAddHelper? = null

        override fun create(
            coroutineScope: CoroutineScope,
            tapToAddMode: TapToAddMode
        ): TapToAddHelper {
            val helper = FakeTapToAddHelper()
            createCalls.add(CreateCall(coroutineScope, tapToAddMode))

            createdHelper = helper
            return helper
        }

        fun getCreatedHelper(): FakeTapToAddHelper? = createdHelper

        class CreateCall(
            val coroutineScope: CoroutineScope,
            val tapToAddMode: TapToAddMode
        )

        class Scenario(
            val createCalls: ReceiveTurbine<CreateCall>,
            val tapToAddHelperFactory: Factory,
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

            helper.validate()
        }

        fun noOp() = FakeTapToAddHelper()
    }
}
