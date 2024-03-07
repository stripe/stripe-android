package com.stripe.android.financialconnections.features.manualentry

import com.stripe.android.financialconnections.R
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class ManualEntryFormStateTest {

    private lateinit var form: ManualEntryFormState
    private val scope = TestScope()

    @Before
    fun setUp() {
        form = ManualEntryFormState(scope = scope)
    }

    @Test
    fun `initial error states should be null`() = runTest {
        assertTrue(form.routingError.first() == null)
        assertTrue(form.accountError.first() == null)
        assertTrue(form.accountConfirmError.first() == null)
    }

    @Test
    fun `routing number validation checks for US routing number pattern`() = runTest {
        scope.advanceUntilIdle()
        assertNull(form.routingError.first())

        form.routing = "123456789" // Invalid US routing number
        scope.advanceUntilIdle()
        assertEquals(R.string.stripe_validation_no_us_routing, form.routingError.first())
    }

    @Test
    fun `routing number must have correct length`() = runTest {
        scope.advanceUntilIdle()
        assertNull(form.routingError.first())

        form.routing = "12345"
        scope.advanceUntilIdle()
        assertEquals(R.string.stripe_validation_routing_too_short, form.routingError.first())
    }

    @Test
    fun `account number validation checks for required and max length`() = runTest {
        scope.advanceUntilIdle()
        assertNull(form.routingError.first())

        form.account = "12345678901234567" // 17 characters
        scope.advanceUntilIdle()
        assertNull(form.accountError.first())

        form.account = "123456789012345678" // more than 17 characters
        scope.advanceUntilIdle()
        assertEquals(R.string.stripe_validation_account_too_long, form.accountError.first())
    }

    @Test
    fun `account confirm validation checks for mismatch`() = runTest {
        scope.advanceUntilIdle()
        assertNull(form.routingError.first())

        form.account = "correctAccount"
        form.accountConfirm = "correctAccount"
        scope.advanceUntilIdle()
        assertNull(form.accountConfirmError.first())

        form.accountConfirm = "wrongAccount"
        scope.advanceUntilIdle()
        assertEquals(R.string.stripe_validation_account_confirm_mismatch, form.accountConfirmError.first())
    }

    @Test
    fun `form is valid if all fields are filled and there are no errors`() = runTest {
        scope.advanceUntilIdle()
        assertTrue(form.isValid.first().not())

        form.routing = "111000025" // Valid US routing number
        form.account = "12345678"
        form.accountConfirm = "12345678"
        scope.advanceUntilIdle()
        assertTrue(form.isValid.first())
    }
}
