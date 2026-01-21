package com.stripe.android.textfield.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

data class CheckoutResponse(
    val publishableKey: String,
    val paymentIntentClientSecret: String,
    val customerId: String?,
    val ephemeralKeySecret: String?
)

object CheckoutRequester {

    private const val BACKEND_URL = "https://stp-mobile-playground-backend-v7.stripedemos.com/checkout"

    suspend fun fetchCheckoutData(): Result<CheckoutResponse> = withContext(Dispatchers.IO) {
        try {
            val url = URL(BACKEND_URL)
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            // Request body matching playground backend format
            val requestBody = JSONObject().apply {
                put("currency", "usd")
                put("mode", "payment")
                put("customer_session_component_name", "mobile_payment_element")
            }

            connection.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray())
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(response)

                Result.success(
                    CheckoutResponse(
                        publishableKey = json.getString("publishableKey"),
                        paymentIntentClientSecret = json.getString("intentClientSecret"),
                        customerId = json.optString("customerId", null),
                        ephemeralKeySecret = json.optString("customerEphemeralKeySecret", null)
                    )
                )
            } else {
                Result.failure(Exception("HTTP ${connection.responseCode}: ${connection.responseMessage}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
