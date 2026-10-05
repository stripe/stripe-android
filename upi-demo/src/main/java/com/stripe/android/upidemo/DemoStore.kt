package com.stripe.android.upidemo

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

internal object DemoStore {
    private lateinit var preferences: SharedPreferences

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences("upi_demo", Context.MODE_PRIVATE)
    }

    @Synchronized
    fun newPayment(): String {
        val id = "pi_upidemo${UUID.randomUUID().toString().replace("-", "")}"
        preferences.edit().clear()
            .putString("id", id)
            .putString("status", "requires_payment_method")
            .commit()
        log("Created fake payment for INR 100.00")
        return "${id}_secret_demo"
    }

    @Synchronized
    fun confirm(id: String): JSONObject {
        requireCurrentPayment(id)
        preferences.edit().putString("status", "requires_action").remove("settles_at").commit()
        log("Confirm -> requires_action; launching app chooser")
        return paymentIntent(id)
    }

    @Synchronized
    fun retrieve(id: String): JSONObject {
        requireCurrentPayment(id)
        preferences.edit().putInt("retrieves", preferences.getInt("retrieves", 0) + 1).commit()
        val intent = paymentIntent(id)
        log("Retrieve #${preferences.getInt("retrieves", 0)} -> ${intent.getString("status")}")
        return intent
    }

    @Synchronized
    fun decide(id: String, decision: String): Boolean {
        if (id != preferences.getString("id", null)) return false
        when (decision) {
            "approve" -> preferences.edit()
                .putString("status", "processing")
                .putLong("settles_at", System.currentTimeMillis() + 3_000)
                .commit()
            "decline" -> preferences.edit()
                .putString("status", "requires_payment_method")
                .remove("settles_at")
                .commit()
            else -> return false
        }
        log("Fake bank decision: $decision" + if (decision == "approve") " (settles after 3 seconds)" else "")
        return true
    }

    @Synchronized
    fun elementsSession(id: String): JSONObject = JSONObject()
        .put("session_id", "elements_session_upidemo")
        .put("merchant_country", "IN")
        .put("google_pay_preference", "disabled")
        .put("payment_method_preference", JSONObject()
            .put("object", "payment_method_preference")
            .put("country_code", "IN")
            .put("ordered_payment_method_types", JSONArray().put("upi"))
            .put("type", "payment_intent")
            .put("payment_intent", paymentIntent(id)))

    @Synchronized
    fun paymentIntent(id: String): JSONObject {
        requireCurrentPayment(id)
        val status = status()
        val uri = Uri.Builder().scheme("stripe-upi-demo").authority("pay")
            .appendQueryParameter("pa", "merchant@demo")
            .appendQueryParameter("pn", "Demo Merchant")
            .appendQueryParameter("tr", id)
            .appendQueryParameter("am", "100.00")
            .appendQueryParameter("cu", "INR")
            .build()
        return JSONObject()
            .put("id", id)
            .put("object", "payment_intent")
            .put("client_secret", "${id}_secret_demo")
            .put("amount", 10_000)
            .put("currency", "inr")
            .put("created", 1_700_000_000)
            .put("livemode", false)
            .put("capture_method", "automatic")
            .put("confirmation_method", "automatic")
            .put("status", status)
            .put("payment_method_types", JSONArray().put("upi"))
            .put("payment_method", paymentMethod())
            .apply {
                if (status == "requires_action") {
                    put("next_action", JSONObject()
                        .put("type", "upi_handle_redirect_or_display_qr_code")
                        .put("upi_handle_redirect_or_display_qr_code", JSONObject()
                            .put("mobile_auth_url", uri.toString())))
                }
            }
    }

    fun paymentMethod(): JSONObject = JSONObject()
        .put("id", "pm_upidemo")
        .put("object", "payment_method")
        .put("type", "upi")
        .put("livemode", false)
        .put("created", 1_700_000_000)
        .put("upi", JSONObject())

    @Synchronized
    fun summary(): String = buildString {
        appendLine("Fake backend: ${status()}")
        appendLine("Intent retrievals: ${preferences.getInt("retrieves", 0)}")
        appendLine("PaymentSheet: ${preferences.getString("result", "not completed")}")
        appendLine()
        append(preferences.getString("log", "Start a fake payment to begin."))
    }

    @Synchronized
    fun recordResult(result: String) {
        preferences.edit().putString("result", result).commit()
        log("PaymentSheet result: $result")
    }

    @Synchronized
    fun log(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val line = "$time  $message"
        val lines = (preferences.getString("log", "").orEmpty().lines() + line).takeLast(50)
        preferences.edit().putString("log", lines.joinToString("\n").trim()).commit()
        Log.i("UpiDemo", message)
    }

    private fun status(): String {
        val status = preferences.getString("status", "not started").orEmpty()
        val settlesAt = preferences.getLong("settles_at", Long.MAX_VALUE)
        return if (status == "processing" && System.currentTimeMillis() >= settlesAt) {
            preferences.edit().putString("status", "succeeded").remove("settles_at").commit()
            "succeeded"
        } else {
            status
        }
    }

    private fun requireCurrentPayment(id: String) {
        require(id == preferences.getString("id", null)) { "Unknown fake payment" }
    }
}
