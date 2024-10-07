package com.stripe.android.customersheet.data

private typealias RetrieveSetupIntentClientSecretOperation = () -> CustomerSheetDataResult<String>

internal class FakeCustomerSheetIntentDataSource(
    override val canCreateSetupIntents: Boolean = true,
    private val onRetrieveSetupIntentClientSecret: RetrieveSetupIntentClientSecretOperation = {
        CustomerSheetDataResult.failure(
            displayMessage = "Not implemented!",
            cause = Exception("Not implemented!"),
        )
    },
) : CustomerSheetIntentDataSource {
    override suspend fun retrieveSetupIntentClientSecret(): CustomerSheetDataResult<String> {
        return onRetrieveSetupIntentClientSecret.invoke()
    }
}
