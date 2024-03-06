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
class ManualEntryFormTest {

    private lateinit var form: ManualEntryForm
    private val scope = TestScope()

    @Before
    fun setUp() {
        form = ManualEntryForm(scope = scope)
    }

    @Test
    fun `initial error states should be null`() = runTest {
        assertTrue(form.routingError.first() == null)
        assertTrue(form.accountError.first() == null)
        assertTrue(form.accountConfirmError.first() == null)
    }

    @Test
    fun `routing number validation checks for US routing number pattern`() = runTest {
        form.routing = "111000025" // Valid US routing number
        scope.advanceUntilIdle()
        assertNull(form.routingError.first())
        form.routing = "123456789" // Invalid US routing number
        scope.advanceUntilIdle()
        assertEquals(R.string.stripe_validation_no_us_routing, form.routingError.first())
    }

    @Test
    fun `routing number must have correct length`() = runTest {
        form.routing = ""
        scope.advanceUntilIdle()
        assertNull(form.routingError.first())
        form.routing = "12345"
        scope.advanceUntilIdle()
        assertEquals(R.string.stripe_validation_routing_too_short, form.routingError.first())
    }

        @Test
    fun `account number validation checks for required and max length`() = runTest {
        form.account = ""
        assertEquals(R.string.stripe_validation_account_required, form.accountError.first())

        form.account = "12345678901234567" // 17 characters
        assertNull(form.accountError.first())

        form.account = "123456789012345678" // more than 17 characters
        assertEquals(R.string.stripe_validation_account_too_long, manualEntryForm.accountError.first())
    }

}

//

//

//
//

//
//    @Test
//    fun `account confirm validation checks for mismatch`() = runTest {
//        manualEntryForm.account = "correctAccount"
//        manualEntryForm.accountConfirm = "correctAccount"
//        assertNull(manualEntryForm.accountConfirmError.first())
//
//        manualEntryForm.accountConfirm = "wrongAccount"
//        assertEquals(R.string.stripe_validation_account_confirm_mismatch, manualEntryForm.accountConfirmError.first())
//    }
//
//    @Test
//    fun `form is valid if all fields are filled and there are no errors`() = runTest {
//        manualEntryForm.routing = "111000025" // Valid US routing number
//        manualEntryForm.account = "12345678"
//        manualEntryForm.accountConfirm = "12345678"
//
//        assertTrue(manualEntryForm.isValid.first())
//    }

//}