package com.stripe.detektrules

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class CustomRuleSetProvider : RuleSetProvider {
  override val ruleSetId: String = "custom-detekt-rules"
  override fun instance(config: Config)
      = RuleSet(ruleSetId, listOf(CompileOnlyCodeLeakRule(config)))
}