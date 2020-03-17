package com.stripe.example.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stripe.example.R
import com.stripe.example.databinding.FragmentsExampleActivityBinding

class FragmentExamplesActivity : AppCompatActivity() {
    private val viewBinding: FragmentsExampleActivityBinding by lazy {
        FragmentsExampleActivityBinding.inflate(layoutInflater)
    }

    private var fragment: FragmentExamplesFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        setTitle(R.string.launch_payment_session_from_fragment)

        if (savedInstanceState == null) {
            this.fragment = FragmentExamplesFragment().also {
                supportFragmentManager
                    .beginTransaction()
                    .replace(
                        viewBinding.fragmentContainer.id,
                        it,
                        FragmentExamplesFragment::class.java.simpleName
                    )
                    .commit()
            }
        } else {
            this.fragment = supportFragmentManager.getFragment(
                savedInstanceState,
                STATE_FRAGMENT
            ) as? FragmentExamplesFragment?
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        fragment?.let {
            supportFragmentManager.putFragment(outState, STATE_FRAGMENT, it)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        fragment?.onActivityResult(requestCode, resultCode, data)
    }

    private companion object {
        private const val STATE_FRAGMENT = "state_fragment"
    }
}
