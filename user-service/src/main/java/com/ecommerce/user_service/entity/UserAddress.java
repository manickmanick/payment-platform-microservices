package com.ecommerce.user_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_addresses")
public class UserAddress {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 @Column(name = "user_id", nullable = false)
 private Long userId;

 @Column(nullable = false, length = 60)
 private String label;

 @Column(nullable = false, length = 250)
 private String line1;

 @Column(length = 250)
 private String line2;

 @Column(nullable = false, length = 100)
 private String city;

 @Column(nullable = false, length = 100)
 private String state;

 @Column(name = "postal_code", nullable = false, length = 20)
 private String postalCode;

 @Column(nullable = false, length = 100)
 private String country;

 @Column(name = "phone_number", length = 30)
 private String phoneNumber;

 @Column(name = "sms_opt_in", nullable = false)
 private boolean smsOptIn;

 @Column(name = "primary_address", nullable = false)
 private boolean primaryAddress = false;

 protected UserAddress() {
 }

 public UserAddress(
         Long userId,
         String label,
         String line1,
         String line2,
         String city,
         String state,
         String postalCode,
         String country,
         String phoneNumber,
         boolean smsOptIn,
         boolean primaryAddress
 ) {
  this.userId = userId;
  this.label = label;
  this.line1 = line1;
  this.line2 = line2;
  this.city = city;
  this.state = state;
  this.postalCode = postalCode;
  this.country = country;
  this.phoneNumber = phoneNumber;
  this.smsOptIn = smsOptIn;
  this.primaryAddress = primaryAddress;
 }

 public Long getId() {
  return id;
 }

 public Long getUserId() {
  return userId;
 }

 public String getLabel() {
  return label;
 }

 public String getLine1() {
  return line1;
 }

 public String getLine2() {
  return line2;
 }

 public String getCity() {
  return city;
 }

 public String getState() {
  return state;
 }

 public String getPostalCode() {
  return postalCode;
 }

 public String getCountry() {
  return country;
 }

 public String getPhoneNumber() {
  return phoneNumber;
 }

 public boolean isSmsOptIn() {
  return smsOptIn;
 }

 public boolean isPrimaryAddress() {
  return primaryAddress;
 }

 public void update(
         String label,
         String line1,
         String line2,
         String city,
         String state,
         String postalCode,
         String country,
         String phoneNumber,
         boolean smsOptIn,
         boolean primaryAddress
 ) {
  this.label = label;
  this.line1 = line1;
  this.line2 = line2;
  this.city = city;
  this.state = state;
  this.postalCode = postalCode;
  this.country = country;
  this.phoneNumber = phoneNumber;
  this.smsOptIn = smsOptIn;
  this.primaryAddress = primaryAddress;
 }
}