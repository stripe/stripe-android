package com.stripe.android.paymentsheet

internal class PaymentSheetFlowController private constructor(
    private val args: PaymentSheetActivityStarter.Args
) {

    internal companion object {
        fun create(
            clientSecret: String,
            ephemeralKey: String,
            customerId: String,
            onComplete: (PaymentSheetFlowController) -> Unit
        ) {
            val flowController = PaymentSheetFlowController(
                PaymentSheetActivityStarter.Args.Default(
                    clientSecret,
                    ephemeralKey,
                    customerId
                )
            )

            // load payment methods
            // load default payment option

            onComplete(flowController)
        }

        fun create(
            clientSecret: String,
            onComplete: (PaymentSheetFlowController) -> Unit
        ) {
            val flowController = PaymentSheetFlowController(
                PaymentSheetActivityStarter.Args.Guest(
                    clientSecret
                )
            )

            // load default payment option

            onComplete(flowController)
        }
    }
}
