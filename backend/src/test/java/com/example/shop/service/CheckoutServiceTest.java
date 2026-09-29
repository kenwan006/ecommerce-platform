package com.example.shop.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.shop.client.StripeClient;
import com.example.shop.client.WarehouseClient;
import com.example.shop.client.PaymentClient;
import com.example.shop.dto.CheckoutRequest;
import com.example.shop.dto.CheckoutResponse;
import com.example.shop.entity.Order;
import com.example.shop.fraud.FraudAssessmentService;
import com.example.shop.fraud.model.FraudDecision;
import com.example.shop.fraud.model.FraudDecisionResult;
import com.example.shop.inventory.InventoryService;
import com.example.shop.repository.OrderRepository;
import com.stripe.model.PaymentIntent;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CheckoutServiceTest {
  @Test
  void retryUsesTheOriginalOrderIdAndCheckoutIdForStripe() throws Exception {
    OrderRepository orders = mock(OrderRepository.class);
    StripeClient stripe = mock(StripeClient.class);
    OrderService orderService = mock(OrderService.class);
    FraudAssessmentService fraudAssessmentService = mock(FraudAssessmentService.class);
    InventoryService inventoryService = mock(InventoryService.class);
    WarehouseClient warehouseClient = mock(WarehouseClient.class);
    PaymentClient paymentClient = mock(PaymentClient.class);
    CheckoutService service = new CheckoutService(
        orders, stripe, orderService, fraudAssessmentService, inventoryService, warehouseClient, paymentClient);
    CheckoutRequest request = new CheckoutRequest(7L, "checkout-123", List.of(new CheckoutRequest.Item(9L, 1)));

    Order originalOrder = new Order();
    setField(originalOrder, "id", 101L);
    originalOrder.setCheckoutId("checkout-123");
    originalOrder.setTotal(new BigDecimal("25.00"));
    when(orderService.getOrCreateCheckout(request)).thenReturn(originalOrder);
    when(fraudAssessmentService.assess(101L)).thenReturn(
        new FraudDecisionResult(FraudDecision.APPROVE, 0.1, List.of("RISK_SCORE_ACCEPTABLE")));

    PaymentIntent paymentIntent = new PaymentIntent();
    paymentIntent.setId("pi_original");
    paymentIntent.setClientSecret("secret_original");
    when(stripe.createPaymentIntent(2500L, "usd", "101", "7", "checkout-123"))
        .thenReturn(paymentIntent);

    CheckoutResponse first = service.createCheckout(request);
    CheckoutResponse retry = service.createCheckout(request);

    assertThat(first.orderId()).isEqualTo(101L);
    assertThat(retry.paymentIntentId()).isEqualTo("pi_original");
    verify(stripe, times(2)).createPaymentIntent(2500L, "usd", "101", "7", "checkout-123");
    verify(orders, times(2)).save(originalOrder);
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
