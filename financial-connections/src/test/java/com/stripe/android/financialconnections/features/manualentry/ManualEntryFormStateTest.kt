package com.stripe.android.financialconnections.features.manualentry

import com.stripe.android.financialconnections.R
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class ManualEntryFormStateTest {

    @Test
    fun `initial error states should be null`() = runTest {
        val form = ManualEntryFormState()
        assertTrue(form.routingError == null)
        assertTrue(form.accountError == null)
        assertTrue(form.accountConfirmError == null)
    }

    @Test
    fun `routing number validation checks for US routing number pattern`() = runTest {
        ManualEntryFormState(
            routing = "123456789"
        ).apply {
            assertEquals(R.string.stripe_validation_no_us_routing, routingError)
        }
    }

    @Test
    fun `routing number must have correct length`() = runTest {
        ManualEntryFormState(
            routing = "12345"
        ).apply {
            assertEquals(R.string.stripe_validation_routing_too_short, routingError)
        }
    }

    @Test
    fun `account number validation checks for required and max length`() = runTest {
        ManualEntryFormState(
            account = "12345678901234567" // 17 characters
        ).apply {
            assertNull(accountError)
        }

        ManualEntryFormState(
            account = "123456789012345678" // 17 characters
        ).apply {
            assertEquals(R.string.stripe_validation_account_too_long, accountError)
        }
    }

    @Test
    fun `account confirm validation checks for mismatch`() = runTest {
        ManualEntryFormState(
            account = "correctAccount",
            accountConfirm = "correctAccount"
        ).apply {
            assertNull(accountConfirmError)
        }

        ManualEntryFormState(
            account = "correctAccount",
            accountConfirm = "wrongAccount"
        ).apply {
            assertEquals(R.string.stripe_validation_account_confirm_mismatch, accountConfirmError)
        }
    }

    @Test
    fun `form is invalid if any field is empty`() = runTest {
        val form = ManualEntryFormState()
        assertTrue(form.isValid.not())
    }

    @Test
    fun `form is valid if all fields are filled and there are no errors`() = runTest {
        ManualEntryFormState(
            routing = "111000025",
            account = "12345678",
            accountConfirm = "12345678"
        ).apply {
            assertTrue(isValid)
        }
    }
}
