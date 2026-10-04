package com.example.shop.service;

import com.example.shop.dto.CartItemRequest;
import com.example.shop.dto.CartQuantityRequest;
import com.example.shop.dto.CartResponse;
import com.example.shop.entity.Cart;
import com.example.shop.entity.CartItem;
import com.example.shop.entity.Product;
import com.example.shop.entity.User;
import com.example.shop.repository.CartRepository;
import com.example.shop.repository.ProductRepository;
import com.example.shop.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Persistent customer cart. Inventory is deliberately reserved only at checkout. */
@Service
public class CartService {
  private final CartRepository cartRepository;
  private final UserRepository userRepository;
  private final ProductRepository productRepository;

  public CartService(
      CartRepository cartRepository,
      UserRepository userRepository,
      ProductRepository productRepository) {
    this.cartRepository = cartRepository;
    this.userRepository = userRepository;
    this.productRepository = productRepository;
  }

  @Transactional
  public CartResponse getCart(Long userId) {
    return CartResponse.from(findOrCreateCart(userId));
  }

  @Transactional
  public CartResponse addItem(Long userId, CartItemRequest request) {
    Cart cart = findOrCreateCart(userId);
    Product product =
        productRepository
            .findById(request.productId())
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + request.productId()));

    CartItem existingItem =
        cart.getItems().stream()
            .filter(item -> item.getProduct().getId().equals(product.getId()))
            .findFirst()
            .orElse(null);
    if (existingItem == null) {
      CartItem item = new CartItem();
      item.setProduct(product);
      item.setQuantity(request.quantity());
      cart.addItem(item);
    } else {
      existingItem.setQuantity(existingItem.getQuantity() + request.quantity());
      cart.touch();
    }
    return CartResponse.from(cartRepository.save(cart));
  }

  @Transactional
  public CartResponse removeItem(Long userId, Long productId) {
    Cart cart = findOrCreateCart(userId);
    cart.getItems().stream()
        .filter(item -> item.getProduct().getId().equals(productId))
        .findFirst()
        .ifPresent(cart::removeItem);
    return CartResponse.from(cartRepository.save(cart));
  }

  @Transactional
  public CartResponse updateQuantity(Long userId, Long productId, CartQuantityRequest request) {
    Cart cart = findOrCreateCart(userId);
    CartItem item =
        cart.getItems().stream()
            .filter(candidate -> candidate.getProduct().getId().equals(productId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Product is not in the cart: " + productId));
    item.setQuantity(request.quantity());
    cart.touch();
    return CartResponse.from(cartRepository.save(cart));
  }

  @Transactional
  public void clearCart(Long userId) {
    Cart cart = findOrCreateCart(userId);
    cart.clearItems();
    cartRepository.save(cart);
  }

  private Cart findOrCreateCart(Long userId) {
    return cartRepository
        .findByUserId(userId)
        .orElseGet(
            () -> {
              User user =
                  userRepository
                      .findById(userId)
                      .orElseThrow(() -> new IllegalArgumentException("User not found"));
              Cart cart = new Cart();
              cart.setUser(user);
              return cartRepository.save(cart);
            });
  }
}
