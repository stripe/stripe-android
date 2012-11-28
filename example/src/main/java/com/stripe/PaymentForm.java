package com.stripe;

public interface PaymentForm {
    public String getCardNumber();
    public String getCvc();
    public String getExpMonth();
    public String getExpYear();
}
