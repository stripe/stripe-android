package com.stripe.android.view;

import android.content.Intent;

import com.stripe.android.BuildConfig;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.R;
import com.stripe.android.model.Address;
import com.stripe.android.model.ShippingInformation;
import com.stripe.android.model.ShippingMethod;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class ShippingFlowActivityTest {
    private ActivityController<ShippingFlowActivity> mActivityController;
    private ShadowActivity mShadowActivity;
    private ShippingInfoWidget mShippingInfoWidget;

    @Test
    public void intentBuilder_withEmptyConstructor_buildsCorrectly() {
        Intent emptyStateIntent = new ShippingFlowActivity.IntentBuilder().build(RuntimeEnvironment.application);
        List<String> hiddenAddressFields = new ArrayList<>();
        List<String> optionalAddressFields = new ArrayList<>();
        ShippingInformation emptyAddress = new Address.Builder().build();
        ShippingFlowConfig emptyShippingFlowConfig = new ShippingFlowConfig(hiddenAddressFields, optionalAddressFields, emptyAddress, false, false);
        assertEquals(emptyShippingFlowConfig, emptyStateIntent.getParcelableExtra(ShippingFlowActivity.EXTRA_SHIPPING_FLOW_CONFIG));
    }

    @Test
    public void intentBuilder_withPopulatedConstructor_buildsCorrectly() {
        List<String> hiddenAddressFields = new ArrayList<>();
        hiddenAddressFields.add(ShippingInfoWidget.PHONE_FIELD);
        List<String> optionalAddressFields = new ArrayList<>();
        optionalAddressFields.add(ShippingInfoWidget.POSTAL_CODE_FIELD);
        ShippingInformation address = getExampleAddress();
        Intent emptyStateIntent = new ShippingFlowActivity.IntentBuilder()
                .setHiddenAddressFields(hiddenAddressFields)
                .setOptionalAddressFields(optionalAddressFields)
                .setPrepopulatedShippingInfo(address)
                .setHideAddressScreen(true)
                .setHideShippingScreen(true)
                .build(RuntimeEnvironment.application);
        ShippingFlowConfig shippingFlowConfig = new ShippingFlowConfig(hiddenAddressFields, optionalAddressFields, address, true, true);
        assertEquals(shippingFlowConfig, emptyStateIntent.getParcelableExtra(ShippingFlowActivity.EXTRA_SHIPPING_FLOW_CONFIG));
    }

    @Test
    public void launchShippingFlowActivity_withHideAddressConfig_hidesAddressView() {
        initializePaymentConfig();
        Intent intent = new ShippingFlowActivity.IntentBuilder()
                .setHideAddressScreen(true).build(RuntimeEnvironment.application);
        mActivityController = Robolectric.buildActivity(ShippingFlowActivity.class, intent)
                .create().start().resume().visible();
        assertNull(mActivityController.get().findViewById(R.id.set_shipping_info_widget));
        assertNotNull(mActivityController.get().findViewById(R.id.select_shipping_method_widget));
    }

    @Test
    public void launchShippingFlowActivity_withHideShippingMethodConfig_hidesShippingMethodView() {
        Intent intent = new ShippingFlowActivity.IntentBuilder()
                .setHideShippingScreen(true).build(RuntimeEnvironment.application);
        mActivityController = Robolectric.buildActivity(ShippingFlowActivity.class, intent)
                .create().start().resume().visible();
        mShadowActivity = shadowOf(mActivityController.get());
        mShippingInfoWidget = mActivityController.get().findViewById(R.id.set_shipping_info_widget);
        assertNotNull(mShippingInfoWidget);
        mShippingInfoWidget.populateShippingInfo(getExampleAddress());
        ShippingFlowActivity shippingFlowActivity = mActivityController.get();
        shippingFlowActivity.onActionSave();
        assertTrue(mShadowActivity.isFinishing());
    }

    @Test
    public void onAddressSave_whenAddressNotPopulated_doesNotFinish() {
        Intent intent = new ShippingFlowActivity.IntentBuilder()
                .setHideShippingScreen(true).build(RuntimeEnvironment.application);
        mActivityController = Robolectric.buildActivity(ShippingFlowActivity.class, intent)
                .create().start().resume().visible();
        mShadowActivity = shadowOf(mActivityController.get());
        mShippingInfoWidget = mActivityController.get().findViewById(R.id.set_shipping_info_widget);
        assertNotNull(mShippingInfoWidget);
        ShippingFlowActivity shippingFlowActivity = mActivityController.get();
        shippingFlowActivity.onActionSave();
        assertFalse(mShadowActivity.isFinishing());
    }

    @Test
    public void onAddressSave_whenAddressNotPopulated_doesNotContinue() {
        Intent intent = new ShippingFlowActivity.IntentBuilder()
                .build(RuntimeEnvironment.application);
        mActivityController = Robolectric.buildActivity(ShippingFlowActivity.class, intent)
                .create().start().resume().visible();
        mShadowActivity = shadowOf(mActivityController.get());
        mShippingInfoWidget = mActivityController.get().findViewById(R.id.set_shipping_info_widget);
        assertNotNull(mShippingInfoWidget);
        ShippingFlowActivity shippingFlowActivity = mActivityController.get();
        shippingFlowActivity.onActionSave();
        assertFalse(mShadowActivity.isFinishing());
        assertNotNull(mActivityController.get().findViewById(R.id.set_shipping_info_widget));
    }

    @Test
    public void onAddressSave_whenAddressPopulated_showsShippingMethod() {
        initializePaymentConfig();
        Intent intent = new ShippingFlowActivity.IntentBuilder()
                .build(RuntimeEnvironment.application);
        mActivityController = Robolectric.buildActivity(ShippingFlowActivity.class, intent)
                .create().start().resume().visible();
        mShadowActivity = shadowOf(mActivityController.get());
        mShippingInfoWidget = mActivityController.get().findViewById(R.id.set_shipping_info_widget);
        assertNotNull(mShippingInfoWidget);
        ShippingFlowActivity shippingFlowActivity = mActivityController.get();
        mShippingInfoWidget.populateShippingInfo(getExampleAddress());
        assertFalse(mShadowActivity.isFinishing());
        shippingFlowActivity.onActionSave();
        assertNotNull(mActivityController.get().findViewById(R.id.select_shipping_method_widget));
        shippingFlowActivity.onActionSave();
        assertTrue(mShadowActivity.isFinishing());
    }

    private void initializePaymentConfig() {
        List<ShippingMethod> shippingMethods = new ArrayList<>();
        shippingMethods.add(new ShippingMethod("UPS Ground", "ups-ground", "Arrives in 3-5 days", 0, "USD"));
        shippingMethods.add(new ShippingMethod("FedEx", "fedex", "Arrives tomorrow", 599, "USD"));
        PaymentConfiguration.init("FAKE PUBLISHABLE KEY");
        PaymentConfiguration.getInstance().setShippingMethods(shippingMethods);
    }

    private ShippingInformation getExampleAddress() {
        return new Address.Builder()
                .setCity("San Francisco")
                .setCountry("US")
                .setLine1("123 Market St")
                .setLine2("#345")
                .setPostalCode("94107")
                .setState("CA")
                .setName("Fake Name")
                .setPhoneNumber("(123) 456 - 7890")
                .build();

    }
}
