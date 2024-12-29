package com.stripe.android.financialconnections.features.consent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder


suspend fun verifyIntegrity(integrityToken: String, packageName: String) = withContext(Dispatchers.IO) {
    val url = URL("https://attestation-android.glitch.me/verify-integrity")
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "POST"
    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
    connection.setRequestProperty("Accept", "application/json")
    connection.doOutput = true
    connection.connectTimeout = 15000
    connection.readTimeout = 15000

    // Constructing URL-encoded form data
    val requestBody = "integrityToken=${URLEncoder.encode(integrityToken, "UTF-8")}" +
        "&packageName=${URLEncoder.encode(packageName, "UTF-8")}"

    println("Request Payload: $requestBody") // Log the URL-encoded form data

    connection.outputStream.use { os ->
        OutputStreamWriter(os, "UTF-8").use { writer ->
            writer.write(requestBody)
            writer.flush()
        }
    }

    val responseCode = connection.responseCode
    val responseMessage: String

    if (responseCode == HttpURLConnection.HTTP_OK) {
        responseMessage = connection.inputStream.bufferedReader().use { it.readText() }
    } else {
        responseMessage = connection.errorStream?.bufferedReader()?.use {
            it.readText()
        } ?: "Error occurred with response code: $responseCode"
        println("Error response code: $responseCode - $responseMessage")
    }

    connection.disconnect()

    println("Response Message: $responseMessage")

    responseMessage
}