package com.example.shop.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
public class Cart {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(optional = false)
  @JoinColumn(name = "user_id", unique = true)
  private User user;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<CartItem> items = new ArrayList<>();

  public Long getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public List<CartItem> getItems() {
    return items;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public void addItem(CartItem item) {
    items.add(item);
    item.setCart(this);
    touch();
  }

  public void removeItem(CartItem item) {
    items.remove(item);
    touch();
  }

  public void clearItems() {
    items.clear();
    touch();
  }

  public void touch() {
    updatedAt = Instant.now();
  }
}
