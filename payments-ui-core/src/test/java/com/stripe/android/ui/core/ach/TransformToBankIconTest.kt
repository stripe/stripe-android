package com.stripe.android.ui.core.ach

import org.junit.Assert.assertEquals
import kotlin.test.Test
import com.stripe.android.R as StripeR
import com.stripe.android.financialconnections.R as FinancialConnectionsR

class TransformToBankIconTest {

    @Test
    fun `given valid bank name, transform returns the correct bank icon`() {
        // bank of america
        assertEquals(TransformToBankIcon("Bank of America"), StripeR.drawable.stripe_ic_bank_boa)
        assertEquals(TransformToBankIcon("bank of america"), StripeR.drawable.stripe_ic_bank_boa)
        assertEquals(TransformToBankIcon("BANKof AMERICA"), FinancialConnectionsR.drawable.stripe_ic_bank)

        // capital one
        assertEquals(TransformToBankIcon("Capital One"), StripeR.drawable.stripe_ic_bank_capitalone)
        assertEquals(TransformToBankIcon("capital one"), StripeR.drawable.stripe_ic_bank_capitalone)
        assertEquals(TransformToBankIcon("Capital      One"), FinancialConnectionsR.drawable.stripe_ic_bank)

        // compass
        assertEquals(TransformToBankIcon("BBVA"), StripeR.drawable.stripe_ic_bank_compass)
        assertEquals(TransformToBankIcon("bbva"), StripeR.drawable.stripe_ic_bank_compass)
        assertEquals(TransformToBankIcon("Compass"), StripeR.drawable.stripe_ic_bank_compass)
        assertEquals(TransformToBankIcon("compass"), StripeR.drawable.stripe_ic_bank_compass)
        assertEquals(TransformToBankIcon("b b v a"), FinancialConnectionsR.drawable.stripe_ic_bank)

        // citi
        assertEquals(TransformToBankIcon("Citibank"), StripeR.drawable.stripe_ic_bank_citi)
        assertEquals(TransformToBankIcon("citibank"), StripeR.drawable.stripe_ic_bank_citi)
        assertEquals(TransformToBankIcon("Citi Bank"), FinancialConnectionsR.drawable.stripe_ic_bank)

        // morgan chase
        assertEquals(TransformToBankIcon("MORGAN CHASE"), StripeR.drawable.stripe_ic_bank_morganchase)
        assertEquals(TransformToBankIcon("morgan chase"), StripeR.drawable.stripe_ic_bank_morganchase)
        assertEquals(TransformToBankIcon("JP MORGAN"), StripeR.drawable.stripe_ic_bank_morganchase)
        assertEquals(TransformToBankIcon("jp morgan"), StripeR.drawable.stripe_ic_bank_morganchase)
        assertEquals(TransformToBankIcon("Chase"), StripeR.drawable.stripe_ic_bank_morganchase)
        assertEquals(TransformToBankIcon("chase"), StripeR.drawable.stripe_ic_bank_morganchase)

        // nfcu
        assertEquals(TransformToBankIcon("NAVY FEDERAL CREDIT UNION"), StripeR.drawable.stripe_ic_bank_nfcu)
        assertEquals(TransformToBankIcon("navy federal credit union"), StripeR.drawable.stripe_ic_bank_nfcu)
        assertEquals(TransformToBankIcon("NFCU"), FinancialConnectionsR.drawable.stripe_ic_bank)

        // pnc
        assertEquals(TransformToBankIcon("PNC BANK"), StripeR.drawable.stripe_ic_bank_pnc)
        assertEquals(TransformToBankIcon("pnc bank"), StripeR.drawable.stripe_ic_bank_pnc)
        assertEquals(TransformToBankIcon("PNC Bank"), StripeR.drawable.stripe_ic_bank_pnc)
        assertEquals(TransformToBankIcon("PNC  BANK"), FinancialConnectionsR.drawable.stripe_ic_bank)

        // suntrust
        assertEquals(TransformToBankIcon("SUNTRUST"), StripeR.drawable.stripe_ic_bank_suntrust)
        assertEquals(TransformToBankIcon("suntrust"), StripeR.drawable.stripe_ic_bank_suntrust)
        assertEquals(TransformToBankIcon("SunTrust Bank"), StripeR.drawable.stripe_ic_bank_suntrust)
        assertEquals(TransformToBankIcon("suntrust bank"), StripeR.drawable.stripe_ic_bank_suntrust)

        // svb
        assertEquals(TransformToBankIcon("Silicon Valley Bank"), StripeR.drawable.stripe_ic_bank_svb)
        assertEquals(TransformToBankIcon("silicon valley bank"), StripeR.drawable.stripe_ic_bank_svb)
        assertEquals(TransformToBankIcon("svb"), FinancialConnectionsR.drawable.stripe_ic_bank)

        // stripe
        assertEquals(TransformToBankIcon("stripe"), StripeR.drawable.stripe_ic_bank_stripe)
        assertEquals(TransformToBankIcon("Stripe"), StripeR.drawable.stripe_ic_bank_stripe)
        assertEquals(TransformToBankIcon("stripe"), StripeR.drawable.stripe_ic_bank_stripe)
        assertEquals(TransformToBankIcon("TestInstitution"), StripeR.drawable.stripe_ic_bank_stripe)
        assertEquals(TransformToBankIcon("testinstitution"), StripeR.drawable.stripe_ic_bank_stripe)

        // td
        assertEquals(TransformToBankIcon("TD Bank"), StripeR.drawable.stripe_ic_bank_td)
        assertEquals(TransformToBankIcon("td bank"), StripeR.drawable.stripe_ic_bank_td)
        assertEquals(TransformToBankIcon("TD"), FinancialConnectionsR.drawable.stripe_ic_bank)

        // usaa
        assertEquals(TransformToBankIcon("USAA FEDERAL SAVINGS BANK"), StripeR.drawable.stripe_ic_bank_usaa)
        assertEquals(TransformToBankIcon("usaa federal savings bank"), StripeR.drawable.stripe_ic_bank_usaa)
        assertEquals(TransformToBankIcon("USAA Bank"), StripeR.drawable.stripe_ic_bank_usaa)
        assertEquals(TransformToBankIcon("usaa bank"), StripeR.drawable.stripe_ic_bank_usaa)
        assertEquals(TransformToBankIcon("USAA Savings Bank"), FinancialConnectionsR.drawable.stripe_ic_bank)

        // us bank
        assertEquals(TransformToBankIcon("U.S. BANK"), StripeR.drawable.stripe_ic_bank_usbank)
        assertEquals(TransformToBankIcon("u.s. bank"), StripeR.drawable.stripe_ic_bank_usbank)
        assertEquals(TransformToBankIcon("u.s. Bank"), StripeR.drawable.stripe_ic_bank_usbank)
        assertEquals(TransformToBankIcon("US Bank"), StripeR.drawable.stripe_ic_bank_usbank)
        assertEquals(TransformToBankIcon("us bank"), StripeR.drawable.stripe_ic_bank_usbank)
        assertEquals(TransformToBankIcon("usbank"), FinancialConnectionsR.drawable.stripe_ic_bank)

        // wells fargo
        assertEquals(TransformToBankIcon("Wells Fargo"), StripeR.drawable.stripe_ic_bank_wellsfargo)
        assertEquals(TransformToBankIcon("wells fargo"), StripeR.drawable.stripe_ic_bank_wellsfargo)
        assertEquals(TransformToBankIcon("WELLS FARGO"), StripeR.drawable.stripe_ic_bank_wellsfargo)
        assertEquals(TransformToBankIcon("Well's Fargo"), FinancialConnectionsR.drawable.stripe_ic_bank)
    }
}
