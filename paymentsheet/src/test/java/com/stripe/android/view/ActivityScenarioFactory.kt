package com.stripe.android.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.model.PaymentMethod

internal class ActivityScenarioFactory(
    private val context: Context
) {
    internal inline fun <reified T : Activity> create(
        args: ActivityStarter.Args
    ): ActivityScenario<T> {
        return ActivityScenario.launch(
            Intent(context, T::class.java)
                .putExtra(ActivityStarter.Args.EXTRA, args)
        )
    }

    @Suppress("Deprecation")
    fun createAddPaymentMethodActivity() = create<AddPaymentMethodActivity>(
        AddPaymentMethodActivityStarter.Args.Builder()
            .setPaymentMethodType(PaymentMethod.Type.Card)
            .setPaymentConfiguration(PaymentConfiguration.getInstance(context))
            .setBillingAddressFields(BillingAddressFields.PostalCode)
            .build()
    )

    /**
     * Return a view created with an `Activity` context.
     */
    fun <ViewType : View> createView(
        viewFactory: (Activity) -> ViewType
    ): ViewType {
        var view: ViewType? = null

        createAddPaymentMethodActivity()
            .use { activityScenario ->
                activityScenario.onActivity { activity ->
                    activity.setTheme(R.style.StripePaymentSheetDefaultTheme)

                    activity.findViewById<ViewGroup>(R.id.add_payment_method_card).let { root ->
                        root.removeAllViews()

                        view = viewFactory(activity).also {
                            root.addView(it)
                        }
                    }
                }
            }

        return requireNotNull(view)
    }
}
