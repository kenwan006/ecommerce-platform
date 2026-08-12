package com.example.shop.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.shop.dto.CheckoutRequest;
import com.example.shop.entity.Order;
import com.example.shop.entity.Product;
import com.example.shop.entity.User;
import com.example.shop.repository.OrderRepository;
import com.example.shop.repository.ProductRepository;
import com.example.shop.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class OrderServiceTest {
  private final UserRepository users = mock(UserRepository.class);
  private final ProductRepository products = mock(ProductRepository.class);
  private final OrderRepository orders = mock(OrderRepository.class);
  private final OrderService service = new OrderService(users, products, orders);

  @Test
  void reusesExistingOrderForTheSameCheckoutIdWithoutReservingStockAgain() {
    CheckoutRequest request = new CheckoutRequest(7L, "checkout-123", List.of(new CheckoutRequest.Item(9L, 2)));
    AtomicReference<Order> storedOrder = new AtomicReference<>();
    when(orders.findByUserIdAndCheckoutId(7L, "checkout-123"))
        .thenAnswer(invocation -> Optional.ofNullable(storedOrder.get()));

    User user = new User();
    Product product = new Product();
    setField(product, "price", new BigDecimal("12.50"));
    setField(product, "stock", 5);
    when(users.findById(7L)).thenReturn(Optional.of(user));
    when(products.findById(9L)).thenReturn(Optional.of(product));
    when(orders.save(any(Order.class))).thenAnswer(invocation -> {
      Order order = invocation.getArgument(0);
      storedOrder.set(order);
      return order;
    });

    Order first = service.getOrCreateCheckout(request);
    Order retry = service.getOrCreateCheckout(request);

    assertThat(retry).isSameAs(first);
    assertThat(first.getCheckoutId()).isEqualTo("checkout-123");
    assertThat(first.getTotal()).isEqualByComparingTo("25.00");
    assertThat(first.getItems()).hasSize(1);
    assertThat(product.getStock()).isEqualTo(3);
    verify(orders).save(first);
    verify(products).findById(9L);
    verify(users).findById(7L);
  }

  @Test
  void doesNotCreateAnOrderWhenTheCheckoutAlreadyExists() {
    CheckoutRequest request = new CheckoutRequest(7L, "checkout-123", List.of(new CheckoutRequest.Item(9L, 1)));
    Order existing = new Order();
    when(orders.findByUserIdAndCheckoutId(7L, "checkout-123")).thenReturn(Optional.of(existing));

    assertThat(service.getOrCreateCheckout(request)).isSameAs(existing);

    verify(users, never()).findById(any());
    verify(products, never()).findById(any());
    verify(orders, never()).save(any());
  }

  private static void setField(Object target, String fieldName, Object value) {
    try {
      var field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (ReflectiveOperationException exception) {
      throw new AssertionError(exception);
    }
  }
}
