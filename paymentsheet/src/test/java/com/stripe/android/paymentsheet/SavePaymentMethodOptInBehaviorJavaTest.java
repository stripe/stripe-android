package com.stripe.android.paymentsheet;

import static com.google.common.truth.Truth.assertThat;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(TestParameterInjector.class)
public class SavePaymentMethodOptInBehaviorJavaTest {
    @Test
    public void builderAcceptsSaveBehaviorFromJava(
            @TestParameter PaymentSheet.SavePaymentMethodOptInBehavior behavior
    ) {
        PaymentSheet.Configuration configuration = new PaymentSheet.Configuration.Builder("Test")
                .savePaymentMethodOptInBehavior(behavior)
                .build();

        assertThat(configuration).isNotNull();
    }

    @Test
    public void existingConstructorRemainsAvailableFromJava() {
        assertThat(new PaymentSheet.Configuration("Test")).isNotNull();
    }
}
