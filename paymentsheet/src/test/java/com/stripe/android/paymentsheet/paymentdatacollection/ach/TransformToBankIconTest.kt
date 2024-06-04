package com.stripe.android.paymentsheet.paymentdatacollection.ach

import org.junit.Assert
import org.junit.Test
import com.stripe.android.R as StripeR
import com.stripe.android.financialconnections.R as FinancialConnectionsR

class TransformToBankIconTest {
    @Test
    fun `given valid bank name, transform returns the correct bank icon`() {
        // bank of america
        Assert.assertEquals(TransformToBankIcon("Bank of America"), StripeR.drawable.stripe_ic_bank_boa)
        Assert.assertEquals(TransformToBankIcon("bank of america"), StripeR.drawable.stripe_ic_bank_boa)
        Assert.assertEquals(TransformToBankIcon("BANKof AMERICA"), FinancialConnectionsR.drawable.stripe_ic_bank)

        // capital one
        Assert.assertEquals(TransformToBankIcon("Capital One"), StripeR.drawable.stripe_ic_bank_capitalone)
        Assert.assertEquals(TransformToBankIcon("capital one"), StripeR.drawable.stripe_ic_bank_capitalone)
        Assert.assertEquals(TransformToBankIcon("Capital      One"), FinancialConnectionsR.drawable.stripe_ic_bank)

        // compass
        Assert.assertEquals(TransformToBankIcon("BBVA"), StripeR.drawable.stripe_ic_bank_compass)
        Assert.assertEquals(TransformToBankIcon("bbva"), StripeR.drawable.stripe_ic_bank_compass)
        Assert.assertEquals(TransformToBankIcon("Compass"), StripeR.drawable.stripe_ic_bank_compass)
        Assert.assertEquals(TransformToBankIcon("compass"), StripeR.drawable.stripe_ic_bank_compass)
        Assert.assertEquals(TransformToBankIcon("b b v a"), FinancialConnectionsR.drawable.stripe_ic_bank)

        // citi
        Assert.assertEquals(TransformToBankIcon("Citibank"), StripeR.drawable.stripe_ic_bank_citi)
        Assert.assertEquals(TransformToBankIcon("citibank"), StripeR.drawable.stripe_ic_bank_citi)
        Assert.assertEquals(TransformToBankIcon("Citi Bank"), FinancialConnectionsR.drawable.stripe_ic_bank)

        // morgan chase
        Assert.assertEquals(TransformToBankIcon("MORGAN CHASE"), StripeR.drawable.stripe_ic_bank_morganchase)
        Assert.assertEquals(TransformToBankIcon("morgan chase"), StripeR.drawable.stripe_ic_bank_morganchase)
        Assert.assertEquals(TransformToBankIcon("JP MORGAN"), StripeR.drawable.stripe_ic_bank_morganchase)
        Assert.assertEquals(TransformToBankIcon("jp morgan"), StripeR.drawable.stripe_ic_bank_morganchase)
        Assert.assertEquals(TransformToBankIcon("Chase"), StripeR.drawable.stripe_ic_bank_morganchase)
        Assert.assertEquals(TransformToBankIcon("chase"), StripeR.drawable.stripe_ic_bank_morganchase)

        // nfcu
        Assert.assertEquals(TransformToBankIcon("NAVY FEDERAL CREDIT UNION"), StripeR.drawable.stripe_ic_bank_nfcu)
        Assert.assertEquals(TransformToBankIcon("navy federal credit union"), StripeR.drawable.stripe_ic_bank_nfcu)
        Assert.assertEquals(TransformToBankIcon("NFCU"), FinancialConnectionsR.drawable.stripe_ic_bank)

        // pnc
        Assert.assertEquals(TransformToBankIcon("PNC BANK"), StripeR.drawable.stripe_ic_bank_pnc)
        Assert.assertEquals(TransformToBankIcon("pnc bank"), StripeR.drawable.stripe_ic_bank_pnc)
        Assert.assertEquals(TransformToBankIcon("PNC Bank"), StripeR.drawable.stripe_ic_bank_pnc)
        Assert.assertEquals(TransformToBankIcon("PNC  BANK"), FinancialConnectionsR.drawable.stripe_ic_bank)

        // suntrust
        Assert.assertEquals(TransformToBankIcon("SUNTRUST"), StripeR.drawable.stripe_ic_bank_suntrust)
        Assert.assertEquals(TransformToBankIcon("suntrust"), StripeR.drawable.stripe_ic_bank_suntrust)
        Assert.assertEquals(TransformToBankIcon("SunTrust Bank"), StripeR.drawable.stripe_ic_bank_suntrust)
        Assert.assertEquals(TransformToBankIcon("suntrust bank"), StripeR.drawable.stripe_ic_bank_suntrust)

        // svb
        Assert.assertEquals(TransformToBankIcon("Silicon Valley Bank"), StripeR.drawable.stripe_ic_bank_svb)
        Assert.assertEquals(TransformToBankIcon("silicon valley bank"), StripeR.drawable.stripe_ic_bank_svb)
        Assert.assertEquals(TransformToBankIcon("svb"), FinancialConnectionsR.drawable.stripe_ic_bank)

        // stripe
        Assert.assertEquals(TransformToBankIcon("stripe"), StripeR.drawable.stripe_ic_bank_stripe)
        Assert.assertEquals(TransformToBankIcon("Stripe"), StripeR.drawable.stripe_ic_bank_stripe)
        Assert.assertEquals(TransformToBankIcon("stripe"), StripeR.drawable.stripe_ic_bank_stripe)
        Assert.assertEquals(TransformToBankIcon("TestInstitution"), StripeR.drawable.stripe_ic_bank_stripe)
        Assert.assertEquals(TransformToBankIcon("testinstitution"), StripeR.drawable.stripe_ic_bank_stripe)

        // td
        Assert.assertEquals(TransformToBankIcon("TD Bank"), StripeR.drawable.stripe_ic_bank_td)
        Assert.assertEquals(TransformToBankIcon("td bank"), StripeR.drawable.stripe_ic_bank_td)
        Assert.assertEquals(TransformToBankIcon("TD"), FinancialConnectionsR.drawable.stripe_ic_bank)

        // usaa
        Assert.assertEquals(TransformToBankIcon("USAA FEDERAL SAVINGS BANK"), StripeR.drawable.stripe_ic_bank_usaa)
        Assert.assertEquals(TransformToBankIcon("usaa federal savings bank"), StripeR.drawable.stripe_ic_bank_usaa)
        Assert.assertEquals(TransformToBankIcon("USAA Bank"), StripeR.drawable.stripe_ic_bank_usaa)
        Assert.assertEquals(TransformToBankIcon("usaa bank"), StripeR.drawable.stripe_ic_bank_usaa)
        Assert.assertEquals(TransformToBankIcon("USAA Savings Bank"), FinancialConnectionsR.drawable.stripe_ic_bank)

        // us bank
        Assert.assertEquals(TransformToBankIcon("U.S. BANK"), StripeR.drawable.stripe_ic_bank_usbank)
        Assert.assertEquals(TransformToBankIcon("u.s. bank"), StripeR.drawable.stripe_ic_bank_usbank)
        Assert.assertEquals(TransformToBankIcon("u.s. Bank"), StripeR.drawable.stripe_ic_bank_usbank)
        Assert.assertEquals(TransformToBankIcon("US Bank"), StripeR.drawable.stripe_ic_bank_usbank)
        Assert.assertEquals(TransformToBankIcon("us bank"), StripeR.drawable.stripe_ic_bank_usbank)
        Assert.assertEquals(TransformToBankIcon("usbank"), FinancialConnectionsR.drawable.stripe_ic_bank)

        // wells fargo
        Assert.assertEquals(TransformToBankIcon("Wells Fargo"), StripeR.drawable.stripe_ic_bank_wellsfargo)
        Assert.assertEquals(TransformToBankIcon("wells fargo"), StripeR.drawable.stripe_ic_bank_wellsfargo)
        Assert.assertEquals(TransformToBankIcon("WELLS FARGO"), StripeR.drawable.stripe_ic_bank_wellsfargo)
        Assert.assertEquals(TransformToBankIcon("Well's Fargo"), FinancialConnectionsR.drawable.stripe_ic_bank)
    }
}
