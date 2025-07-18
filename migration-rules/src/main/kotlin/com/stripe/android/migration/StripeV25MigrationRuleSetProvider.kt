package com.stripe.android.migration

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class StripeV25MigrationRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "stripe-v25-migration"

    override fun instance(config: Config) = RuleSet(
        id = ruleSetId,
        rules = listOf(
            RememberPaymentSheetMigration(config),
            RememberPaymentSheetFlowControllerMigration(config),
            ClassMigration(config),
        )
    )
}