package com.stripe.example;

public interface PaymentForm {
    String getCardNumber();
    String getCvc();
    Integer getExpMonth();
    Integer getExpYear();
    String getCurrency();
}
