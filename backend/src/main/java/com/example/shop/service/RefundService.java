package com.example.shop.service;

import com.example.shop.client.PaymentClient;
import com.example.shop.entity.Return;
import com.example.shop.exception.ResourceNotFoundException;
import com.example.shop.repository.ReturnRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Commerce owns the decision to request a refund; Payment owns the Stripe interaction. */
@Service
public class RefundService {
  private final ReturnRepository returnRepository;
  private final PaymentClient paymentClient;

  public RefundService(ReturnRepository returnRepository, PaymentClient paymentClient) {
    this.returnRepository = returnRepository;
    this.paymentClient = paymentClient;
  }

  @Transactional
  public void requestAfterInspection(Long returnId, Long orderId) {
    Return returnRequest =
        returnRepository
            .findById(returnId)
            .orElseThrow(() -> new ResourceNotFoundException("Return not found"));
    if (!returnRequest.getOrder().getId().equals(orderId)) {
      throw new IllegalArgumentException("Return does not belong to the warehouse event order");
    }
    if ("REFUND_PENDING".equals(returnRequest.getStatus())
        || "REFUNDED".equals(returnRequest.getStatus())) {
      return;
    }
    if (!"RECEIVED".equals(returnRequest.getStatus())) {
      throw new IllegalStateException("Return has not been received and inspected");
    }

    paymentClient.refund(orderId, returnId);
    returnRequest.setStatus("REFUND_PENDING");
  }
}
