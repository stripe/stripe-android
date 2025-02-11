package com.stripe.android.financialconnections.domain

/**
 * Manages the verdict of the integrity check. If the verdict is failed, the user will be switched to web flow.
 *
 * The scope of this is the application session. Subsequent launches of the AuthFlow within the hosting app after
 * a verdict failure will directly launch the web flow.
 */
internal class IntegrityVerdictManager {

    private var verdictFailed: Boolean = false

    fun setVerdictFailed() {
        verdictFailed = true
    }

    fun verdictFailed(): Boolean {
        return verdictFailed
    }
}
