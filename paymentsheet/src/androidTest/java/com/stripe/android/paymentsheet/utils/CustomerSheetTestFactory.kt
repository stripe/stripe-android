package com.stripe.android.paymentsheet.utils

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.CustomerEphemeralKey
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.CustomerSheetResultCallback
import com.stripe.android.customersheet.rememberCustomerSheet
import com.stripe.android.paymentsheet.ExperimentalCustomerSessionApi

@OptIn(ExperimentalCustomerSessionApi::class)
internal class CustomerSheetTestFactory(
    private val integrationType: IntegrationType,
    private val customerSheetTestType: CustomerSheetTestType,
    private val configuration: CustomerSheet.Configuration,
    private val resultCallback: CustomerSheetResultCallback,
) {

    fun make(activity: ComponentActivity): CustomerSheet {
        return when (integrationType) {
            IntegrationType.Activity -> forActivity(activity)
            IntegrationType.Compose -> forCompose(activity)
        }.apply {
            configure(configuration = configuration)
        }
    }

    private fun forActivity(
        activity: ComponentActivity
    ): CustomerSheet {
        return if (customerSheetTestType == CustomerSheetTestType.CustomerSession) {
            CustomerSheet.create(
                activity = activity,
                customerSessionProvider = createCustomerSessionProvider(),
                callback = resultCallback,
            )
        } else {
            CustomerSheet.create(
                activity = activity,
                customerAdapter = createCustomerAdapter(customerSheetTestType, activity),
                callback = resultCallback,
            )
        }
    }

    private fun forCompose(
        activity: ComponentActivity
    ): CustomerSheet {
        lateinit var customerSheet: CustomerSheet

        activity.setContent {
            customerSheet = if (customerSheetTestType == CustomerSheetTestType.CustomerSession) {
                rememberCustomerSheet(
                    customerSessionProvider = createCustomerSessionProvider(),
                    callback = resultCallback,
                )
            } else {
                rememberCustomerSheet(
                    customerAdapter = createCustomerAdapter(customerSheetTestType, activity),
                    callback = resultCallback,
                )
            }
        }

        return customerSheet
    }

    private fun createCustomerAdapter(
        customerSheetTestType: CustomerSheetTestType,
        activity: ComponentActivity
    ): CustomerAdapter {
        return CustomerAdapter.create(
            context = activity,
            customerEphemeralKeyProvider = {
                CustomerAdapter.Result.success(
                    CustomerEphemeralKey(
                        customerId = "cus_1",
                        ephemeralKey = "ek_test_123"
                    )
                )
            },
            setupIntentClientSecretProvider = when (customerSheetTestType) {
                CustomerSheetTestType.CustomerSession,
                CustomerSheetTestType.AttachToCustomer -> null
                CustomerSheetTestType.AttachToSetupIntent -> {
                    { CustomerAdapter.Result.success("seti_12345_secret_12345") }
                }
            }
        )
    }

    private fun createCustomerSessionProvider(): CustomerSheet.CustomerSessionProvider {
        return object : CustomerSheet.CustomerSessionProvider() {
            override suspend fun provideSetupIntentClientSecret(
                customerId: String
            ) = Result.success("seti_12345_secret_12345")

            override suspend fun providesCustomerSessionClientSecret() = Result.success(
                CustomerSheet.CustomerSessionClientSecret.create(
                    customerId = "cus_1",
                    clientSecret = "cuss_123_secret_123"
                )
            )
        }
    }
}
