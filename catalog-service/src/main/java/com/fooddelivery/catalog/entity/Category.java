package com.fooddelivery.catalog.entity;
import jakarta.persistence.*;
@Entity @Table(name="categories") public class Category {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=100) private String name;
 @Column(name="image_url",length=1000) private String imageUrl;
 protected Category(){} public Category(String name,String imageUrl){this.name=name;this.imageUrl=imageUrl;} public Long getId(){return id;} public String getName(){return name;} public String getImageUrl(){return imageUrl;} public void update(String name,String imageUrl){this.name=name;this.imageUrl=imageUrl;}
}
