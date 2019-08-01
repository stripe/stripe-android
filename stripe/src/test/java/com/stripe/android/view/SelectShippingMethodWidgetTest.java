package com.stripe.android.view;

import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.model.ShippingMethod;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

/**
 * Test for {@link SelectShippingMethodWidget}
 */
@RunWith(RobolectricTestRunner.class)
public class SelectShippingMethodWidgetTest {

    private ShippingMethodAdapter mShippingMethodAdapter;
    private static final ShippingMethod UPS = new ShippingMethod(
            "UPS Ground",
            "ups-ground",
            "Arrives in 3-5 days",
            0,
            "USD"
    );
    private static final ShippingMethod FEDEX = new ShippingMethod(
            "FedEx",
            "fedex",
            "Arrives tomorrow",
            599,
            "USD"
    );
    private static final List<ShippingMethod> SHIPPING_METHODS =
            new ArrayList<>(Arrays.asList(UPS, FEDEX));

    @Before
    public void setup() {
        Locale.setDefault(Locale.US);
        final SelectShippingMethodWidget selectShippingMethodWidget =
                new SelectShippingMethodWidget(ApplicationProvider.getApplicationContext());
        selectShippingMethodWidget.setShippingMethods(SHIPPING_METHODS, UPS);
        mShippingMethodAdapter = selectShippingMethodWidget.mShippingMethodAdapter;
    }

    @Test
    public void selectShippingMethodWidget_whenSelected_selectionChanges() {
        assertEquals(mShippingMethodAdapter.getSelectedShippingMethod(), SHIPPING_METHODS.get(0));
        mShippingMethodAdapter.onShippingMethodSelected(1);
        assertEquals(mShippingMethodAdapter.getSelectedShippingMethod(), SHIPPING_METHODS.get(1));
    }
}
