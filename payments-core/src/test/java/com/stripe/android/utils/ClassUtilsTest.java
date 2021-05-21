package com.stripe.android.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ClassUtilsTest {

    @Test
    public void findField_withValidAllowedFields_shouldReturnField() {
        final Field nameField = ClassUtils.findField(FakeClass.class, new HashSet<>(
            Arrays.asList("mInvalid", "mName")
        ));
        assertNotNull(nameField);
        assertEquals("mName", nameField.getName());
        assertTrue(nameField.isAccessible());
    }

    @Test
    public void findField_withEmptyAllowedFields_shouldReturnNull() {
        final Field nameField = ClassUtils.findField(FakeClass.class, new HashSet<>());
        assertNull(nameField);
    }

    @Test
    public void findMethod_withValidAllowedMethods_shouldReturnMethod() {
        final Method method = ClassUtils.findMethod(FakeClass.class, new HashSet<>(
                Arrays.asList("walk", "run")
        ));
        assertNotNull(method);
        assertEquals("run", method.getName());
    }

    @Test
    public void getInternalObject_shouldReturnExpectedObject() {
        final FakeClass fake = new FakeClass();
        final OuterFakeClass outerClass = new OuterFakeClass(fake);
        final Object obj = ClassUtils.getInternalObject(OuterFakeClass.class, new HashSet<>(
                Arrays.asList("mInvalid", "mFakeClass")
        ), outerClass);
        assertEquals(fake, obj);
    }

    private static class OuterFakeClass {
        @SuppressWarnings("unused")
        private final FakeClass mFakeClass;

        OuterFakeClass(FakeClass fakeClass) {
            mFakeClass = fakeClass;
        }
    }

    private static class FakeClass {
        @SuppressWarnings("unused")
        private String mName;

        public void run() {

        }
    }
}
