package com.ecommerce.user_service.entity;
import jakarta.persistence.*;
@Entity @Table(name="user_addresses") public class UserAddress {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="user_id",nullable=false) private Long userId;
 @Column(nullable=false,length=60) private String label; @Column(nullable=false,length=250) private String line1; @Column(length=250) private String line2;
 @Column(nullable=false,length=100) private String city; @Column(nullable=false,length=100) private String state; @Column(name="postal_code",nullable=false,length=20) private String postalCode; @Column(nullable=false,length=100) private String country;@Column(name="phone_number",length=30)private String phoneNumber;@Column(name="sms_opt_in",nullable=false)private boolean smsOptIn;
 protected UserAddress(){} public UserAddress(Long uid,String label,String line1,String line2,String city,String state,String postalCode,String country,String phone,boolean smsOptIn){userId=uid;this.label=label;this.line1=line1;this.line2=line2;this.city=city;this.state=state;this.postalCode=postalCode;this.country=country;phoneNumber=phone;this.smsOptIn=smsOptIn;}
 public Long getId(){return id;} public Long getUserId(){return userId;} public String getLabel(){return label;} public String getLine1(){return line1;} public String getLine2(){return line2;} public String getCity(){return city;} public String getState(){return state;} public String getPostalCode(){return postalCode;} public String getCountry(){return country;}public String getPhoneNumber(){return phoneNumber;}public boolean isSmsOptIn(){return smsOptIn;}
 public void update(String l,String a,String b,String c,String s,String p,String co,String phone,boolean sms){label=l;line1=a;line2=b;city=c;state=s;postalCode=p;country=co;phoneNumber=phone;smsOptIn=sms;}
}
