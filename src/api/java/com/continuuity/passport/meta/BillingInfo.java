/*
 * Copyright 2012-2013 Continuuity,Inc. All Rights Reserved.
 */

package com.continuuity.passport.meta;

/**
 *  Billing info
 */
public class BillingInfo {

  private final String creditCardName;
  private final String creditCardNumber;
  private final String cvv;
  private final String expiration_date;

  public BillingInfo(String creditCardName, String creditCardNumber, String cvv, String expirationDate) {
    this.creditCardName = creditCardName;
    this.creditCardNumber = creditCardNumber;
    this.cvv = cvv;
    this.expiration_date = expirationDate;
  }

  public String getCreditCardName() {
    return creditCardName;
  }

  public String getCreditCardNumber() {
    return creditCardNumber;
  }

  public String getCvv() {
    return cvv;
  }

  public String getExpirationDate() {
    return expiration_date;
  }
}
