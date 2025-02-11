package com.stripe.android.financialconnections.webview

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

internal class HttpClient {

    suspend fun makeGetRequest(headers: Map<String, String>, url: String, params: Map<String, String>): JSONObject? {
        return withContext(Dispatchers.IO) {
            // Construct the full URL with query parameters
            val queryString = params.map { entry ->
                "${URLEncoder.encode(entry.key, "UTF-8")}=${URLEncoder.encode(entry.value, "UTF-8")}"
            }.joinToString("&")

            val fullUrl = "$url?$queryString"

            var connection: HttpURLConnection? = null
            try {
                connection = URL(fullUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                headers.forEach { (key, value) ->
                    connection.setRequestProperty(key, value)
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val response = StringBuilder()
                    reader.forEachLine { response.append(it) }
                    reader.close()
                    JSONObject(response.toString())
                } else {
                    Log.e("HTTP Error", "Response code: $responseCode")
                    Log.e("HTTP Error", "Response message: ${connection.responseMessage}")
                    JSONObject("{\"error\": \"Request failed with response code: $responseCode\"}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                connection?.disconnect()
            }
        }
    }

    suspend fun makePostRequest(url: String, params: Map<String, String>, contentType: String): JSONObject? {
        return withContext(Dispatchers.IO) {
            val formData = params.map { entry ->
                "${URLEncoder.encode(entry.key, "UTF-8")}=${URLEncoder.encode(entry.value, "UTF-8")}"
            }.joinToString("&")

            var connection: HttpURLConnection? = null
            try {
                // Open connection
                connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", contentType)
                connection.doOutput = true

                // Send the request body
                val outputStream: OutputStream = connection.outputStream
                outputStream.write(formData.toByteArray(Charsets.UTF_8))
                outputStream.close()

                // Read the response
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) { // Check for successful response
                    val inputStream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val response = StringBuilder()
                    reader.forEachLine { response.append(it) }
                    reader.close()
                    JSONObject(response.toString())
                } else {
                    Log.e("HTTP Error", "Response code: $responseCode")
                    Log.e("HTTP Error", "Response message: ${connection.responseMessage}")
                    JSONObject("{\"error\": \"Request failed with response code: $responseCode\"}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                connection?.disconnect()
            }
        }
    }
}