package com.stripe.android.paymentsheet.verticalmode;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/** Got flaky tests? Shampoo them away. */
public final class ShampooRule implements TestRule {
    private final int iterations;

    public ShampooRule(int iterations) {
        if (iterations < 1) throw new IllegalArgumentException("iterations < 1: " + iterations);
        this.iterations = iterations;
    }

    @Override public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override public void evaluate() throws Throwable {
                for (int i = 0; i < iterations; i++) {
                    base.evaluate();
                }
            }
        };
    }
}
