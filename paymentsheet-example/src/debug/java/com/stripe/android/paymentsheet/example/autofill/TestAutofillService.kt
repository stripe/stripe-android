package com.stripe.android.paymentsheet.example.autofill

import android.app.assist.AssistStructure
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews

class TestAutofillService : AutofillService() {

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val structure = request.fillContexts.lastOrNull()?.structure ?: run {
            callback.onSuccess(null)
            return
        }

        val fields = mutableMapOf<String, AutofillId>()
        parseStructure(structure, fields)

        if (fields.isEmpty()) {
            callback.onSuccess(null)
            return
        }

        val matchedFields = fields.filter { (hint, _) -> hint in TEST_DATA }
        if (matchedFields.isEmpty()) {
            callback.onSuccess(null)
            return
        }

        val dataset = Dataset.Builder()
        for ((hint, autofillId) in matchedFields) {
            val value = TEST_DATA.getValue(hint)
            val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
                setTextViewText(android.R.id.text1, "Test: $value")
            }
            dataset.setValue(autofillId, AutofillValue.forText(value), presentation)
        }

        val response = FillResponse.Builder()
            .addDataset(dataset.build())
            .build()

        callback.onSuccess(response)
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        callback.onSuccess()
    }

    private fun parseStructure(
        structure: AssistStructure,
        fields: MutableMap<String, AutofillId>
    ) {
        for (i in 0 until structure.windowNodeCount) {
            parseNode(structure.getWindowNodeAt(i).rootViewNode, fields)
        }
    }

    private fun parseNode(
        node: AssistStructure.ViewNode,
        fields: MutableMap<String, AutofillId>
    ) {
        val hints = node.autofillHints
        val autofillId = node.autofillId

        if (hints != null && autofillId != null) {
            for (hint in hints) {
                if (hint in TEST_DATA) {
                    fields[hint] = autofillId
                }
            }
        }

        for (i in 0 until node.childCount) {
            parseNode(node.getChildAt(i), fields)
        }
    }

    companion object {
        // Keys are the autofill hint strings that Compose ContentType maps to
        // via androidx.autofill.HintConstants
        private val TEST_DATA = mapOf(
            // Address fields
            "postalAddress" to "123 Main Street, Apt 4B, San Francisco, CA 94105",
            "streetAddress" to "123 Main Street",
            "extendedAddress" to "Apt 4B",
            "addressLocality" to "San Francisco",
            "addressRegion" to "CA",
            "addressCountry" to "US",
            "postalCode" to "94105",

            // Person name
            "personName" to "Jane Doe",
            "personGivenName" to "Jane",
            "personFamilyName" to "Doe",

            // Contact
            "emailAddress" to "test@example.com",
            "phoneNumber" to "+14155551234",
            "phoneNational" to "4155551234",

            // Payment
            "creditCardNumber" to "4242424242424242",
            "creditCardSecurityCode" to "123",
            "creditCardExpirationDate" to "12/28",
            "creditCardExpirationMonth" to "12",
            "creditCardExpirationYear" to "2028",
        )
    }
}
