package com.hedera.hashgraph.identity.hcs.example.appnet.dto;

import com.google.gson.annotations.Expose;
import java.util.List;

/**
 * DTO that represents a request body sent from clients to generate new driving license.
 */
public class DrivingLicenseRequest {
  @Expose
  private String issuer;

  @Expose
  private String owner;

  @Expose
  private String firstName;

  @Expose
  private String lastName;

  @Expose
  private List<String> drivingLicenseCategories;

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(final String issuer) {
    this.issuer = issuer;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(final String owner) {
    this.owner = owner;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(final String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(final String lastName) {
    this.lastName = lastName;
  }

  public List<String> getDrivingLicenseCategories() {
    return drivingLicenseCategories;
  }

  public void setDrivingLicenseCategories(final List<String> drivingLicenseCategories) {
    this.drivingLicenseCategories = drivingLicenseCategories;
  }
}
