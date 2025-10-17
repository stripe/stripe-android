package com.stripe.android.attestation

import android.content.Context
import android.content.Intent
import androidx.core.os.BundleCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.link.FakeIntegrityRequestManager
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.utils.InjectableActivityScenario
import com.stripe.android.utils.injectableActivityScenario
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AttestationActivityTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(testDispatcher)

    @Test
    fun `activity should dismiss with success result when attestation succeeds`() = runTest {
        val integrityRequestManager = FakeIntegrityRequestManager().apply {
            requestResult = Result.success("token")
        }

        val scenario = launchActivityForResult(integrityRequestManager)
        advanceUntilIdle()

        assertThat(scenario.getResult().resultCode).isEqualTo(AttestationActivity.RESULT_COMPLETE)

        val result = extractActivityResult(scenario)
        assertThat(result).isInstanceOf<AttestationActivityResult.Success>()
        val successResult = result as AttestationActivityResult.Success
        assertThat(successResult.token).isEqualTo("token")

        scenario.close()
    }

    @Test
    fun `activity should dismiss with failed result when attestation fails`() = runTest {
        val testError = Exception("Attestation failed")
        val integrityRequestManager = FakeIntegrityRequestManager().apply {
            requestResult = Result.failure(testError)
        }

        val scenario = launchActivityForResult(integrityRequestManager)
        advanceUntilIdle()

        assertThat(scenario.getResult().resultCode).isEqualTo(AttestationActivity.RESULT_COMPLETE)

        val result = extractActivityResult(scenario)
        assertThat(result).isInstanceOf<AttestationActivityResult.Failed>()
        val failedResult = result as AttestationActivityResult.Failed
        assertThat(failedResult.error).isEqualTo(testError)

        scenario.close()
    }

    @Test
    fun `createIntent should create proper intent with args`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val intent = AttestationActivity.createIntent(context, args)

        val intentArgs = intent.extras?.let {
            BundleCompat.getParcelable(it, AttestationActivity.EXTRA_ARGS, AttestationArgs::class.java)
        }
        assertThat(intentArgs).isEqualTo(args)
        assertThat(intentArgs?.publishableKey).isEqualTo("pk_test_123")
        assertThat(intentArgs?.productUsage).containsExactly("PaymentSheet")
    }

    @Test
    fun `getArgs should return args from SavedStateHandle when present`() {
        val savedStateHandle = SavedStateHandle().apply {
            set(AttestationActivity.EXTRA_ARGS, args)
        }

        val retrievedArgs = AttestationActivity.getArgs(savedStateHandle)

        assertThat(retrievedArgs).isEqualTo(args)
        assertThat(retrievedArgs?.publishableKey).isEqualTo("pk_test_123")
    }

    @Test
    fun `getArgs should return null when args not present in SavedStateHandle`() {
        val savedStateHandle = SavedStateHandle()

        val retrievedArgs = AttestationActivity.getArgs(savedStateHandle)

        assertThat(retrievedArgs).isNull()
    }

    private fun launchActivityForResult(
        integrityRequestManager: FakeIntegrityRequestManager
    ) = injectableActivityScenario<AttestationActivity> {
        injectActivity {
            viewModelFactory = createTestViewModelFactory(integrityRequestManager)
        }
    }.apply {
        launchForResult(createIntent())
    }

    private fun createIntent() = Intent(
        ApplicationProvider.getApplicationContext(),
        AttestationActivity::class.java
    ).apply {
        putExtra(AttestationActivity.EXTRA_ARGS, args)
    }

    private fun extractActivityResult(
        scenario: InjectableActivityScenario<AttestationActivity>
    ): AttestationActivityResult? {
        return scenario.getResult().resultData?.extras?.let {
            BundleCompat.getParcelable(
                it,
                AttestationActivityContract.EXTRA_RESULT,
                AttestationActivityResult::class.java
            )
        }
    }

    private fun createTestViewModelFactory(
        integrityRequestManager: FakeIntegrityRequestManager
    ): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AttestationViewModel(
                    integrityRequestManager = integrityRequestManager,
                    workContext = testDispatcher
                ) as T
            }
        }
    }

    companion object {
        private val args = AttestationArgs(
            publishableKey = "pk_test_123",
            productUsage = listOf("PaymentSheet")
        )
    }
}
