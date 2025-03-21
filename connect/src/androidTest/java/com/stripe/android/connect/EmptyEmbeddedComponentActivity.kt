package com.stripe.android.connect

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity

@OptIn(PrivateBetaConnectSDK::class)
class EmptyEmbeddedComponentActivity : FragmentActivity() {
    lateinit var manager: EmbeddedComponentManager
    lateinit var controller: AccountOnboardingController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EmbeddedComponentManager.onActivityCreate(this)

        val title = intent.extras?.getString(KEY_TITLE)

        manager = EmbeddedComponentManager(
            configuration = EmbeddedComponentManager.Configuration(
                publishableKey = "fake_pk"
            ),
            fetchClientSecretCallback = { it.onResult("fake_secret") },
        )
        controller = manager.createAccountOnboardingController(this, title)
    }

    companion object {
        private const val KEY_TITLE = "title"

        fun newIntent(context: Context, title: String? = null): Intent {
            return Intent(context, EmptyEmbeddedComponentActivity::class.java)
                .putExtra(KEY_TITLE, title)
        }
    }
}
