package com.stripe.android.payments.connections.reflection

internal interface IsConnectionsAvailable {
    operator fun invoke(): Boolean
}

internal class DefaultIsConnectionsAvailable : IsConnectionsAvailable {
    override fun invoke(): Boolean {
        return try {
            Class.forName("com.stripe.android.connections.ConnectionsSheet")
            true
        } catch (_: Exception) {
            false
        }
    }
}
