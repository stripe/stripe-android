package com.stripe.android.connectsdk

import android.R
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(PrivateBetaConnectSDK::class)
internal class EmbeddedComponentActivity : AppCompatActivity() {

    private val configuration by lazy {
        requireNotNull(intent.getParcelableExtra<EmbeddedComponentManager.Configuration>(CONFIGURATION_EXTRA))
    }
    private val component by lazy {
        requireNotNull(intent.extras?.get(COMPONENT_EXTRA) as EmbeddedComponent)
    }


    private lateinit var embeddedComponentFragment: EmbeddedComponentFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        embeddedComponentFragment = supportFragmentManager.findFragmentByTag(PAYOUT_FRAGMENT_TAG) as? PayoutComponentFragment
            ?: kotlin.run {
                EmbeddedComponentFragment(component).also { newFragment ->
                    supportFragmentManager.beginTransaction().apply {
                        add(R.id.content, newFragment, PAYOUT_FRAGMENT_TAG)
                        commitNow()
                    }
                }
            }

        GlobalScope.launch {
            delay(1000)
            MainScope().launch{
                embeddedComponentFragment.load(configuration)
            }
        }
    }

    companion object {
        private const val COMPONENT_EXTRA = "component"
        private const val CONFIGURATION_EXTRA = "configuration"
        const val PAYOUT_FRAGMENT_TAG = "payout_fragment"

        @OptIn(PrivateBetaConnectSDK::class)
        internal fun newIntent(
            activity: ComponentActivity,
            component: EmbeddedComponent,
            configuration: EmbeddedComponentManager.Configuration,
        ): Intent {
            return Intent(activity, EmbeddedComponentActivity::class.java).apply {
                putExtra(COMPONENT_EXTRA, component)
                putExtra(CONFIGURATION_EXTRA, configuration)
            }
        }
    }
}

internal enum class EmbeddedComponent(val urlName: String) {
    AccountOnboarding("account-onboarding"),
    Payouts("payouts"),
}
