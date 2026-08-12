package com.example.shop.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.shop.dto.CheckoutRequest;
import com.example.shop.dto.CheckoutResponse;
import com.example.shop.service.CheckoutService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class CheckoutControllerTest {
  @Test
  void createsCheckoutUsingTheRequestProvidedByTheClient() {
    CheckoutService service = mock(CheckoutService.class);
    Authentication authentication = mock(Authentication.class);
    CheckoutController controller = new CheckoutController(service);
    CheckoutRequest request = new CheckoutRequest(
        7L, "checkout-123", List.of(new CheckoutRequest.Item(9L, 2)));
    CheckoutResponse expected = new CheckoutResponse(
        101L, "pi_original", "secret_original", new BigDecimal("25.00"), "usd", "PENDING");
    when(service.createCheckout(request)).thenReturn(expected);
    when(authentication.getName()).thenReturn("7");

    CheckoutResponse response = controller.create(request, authentication);

    assertThat(response).isSameAs(expected);
    verify(service).createCheckout(request);
  }
}
