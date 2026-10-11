package com.example.shop.service;

import com.example.shop.client.PaymentClient;
import com.example.shop.client.WarehouseClient;
import com.example.shop.dto.ReturnRequest;
import com.example.shop.dto.ReturnResponse;
import com.example.shop.entity.Order;
import com.example.shop.entity.OrderItem;
import com.example.shop.entity.Return;
import com.example.shop.entity.ReturnItem;
import com.example.shop.exception.ResourceNotFoundException;
import com.example.shop.order.OrderEvent;
import com.example.shop.order.OrderStateMachine;
import com.example.shop.order.OrderStatus;
import com.example.shop.repository.OrderRepository;
import com.example.shop.repository.ReturnRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReturnService {
  private final OrderRepository orderRepository;
  private final ReturnRepository returnRepository;
  private final WarehouseClient warehouseClient;
  private final PaymentClient paymentClient;
  private final OrderStateMachine orderStateMachine;

  public ReturnService(
      OrderRepository orderRepository,
      ReturnRepository returnRepository,
      WarehouseClient warehouseClient,
      PaymentClient paymentClient,
      OrderStateMachine orderStateMachine) {
    this.orderRepository = orderRepository;
    this.returnRepository = returnRepository;
    this.warehouseClient = warehouseClient;
    this.paymentClient = paymentClient;
    this.orderStateMachine = orderStateMachine;
  }

  /** Auto-approves an eligible full-order return and produces a demo prepaid label. */
  @Transactional
  public ReturnResponse request(Long orderId, Long userId, ReturnRequest request) {
    Order order = orderRepository.findByIdAndUserId(orderId, userId)
        .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    Return existing = returnRepository.findByOrderId(orderId).orElse(null);
    if (existing != null) return ReturnResponse.from(existing);
    if (order.getStatus() != OrderStatus.PAID || !"PAID".equals(order.getPaymentStatus())) {
      throw new IllegalStateException("Only paid orders are eligible for a return.");
    }

    String reference = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    Return returnRequest = new Return();
    returnRequest.setOrder(order);
    returnRequest.setRmaNumber("RMA-" + reference);
    returnRequest.setStatus("APPROVED");
    returnRequest.setReason(request.reason());
    returnRequest.setCarrier("Demo Carrier");
    returnRequest.setTrackingNumber("DEMO-RETURN-" + reference);
    returnRequest.setLabelUrl("/api/returns/labels/" + reference + ".pdf");
    for (OrderItem orderItem : order.getItems()) {
      ReturnItem item = new ReturnItem();
      item.setProductId(orderItem.getProduct().getId());
      item.setProductName(orderItem.getProduct().getName());
      item.setQuantity(orderItem.getQuantity());
      returnRequest.addItem(item);
    }
    return ReturnResponse.from(returnRepository.save(returnRequest));
  }

  @Transactional(readOnly = true)
  public ReturnResponse byIdForUser(Long returnId, Long userId) {
    return returnRepository.findByIdAndOrderUserId(returnId, userId)
        .map(ReturnResponse::from)
        .orElseThrow(() -> new ResourceNotFoundException("Return not found"));
  }

  /** Warehouse-only completion: record inspection, restock sellable goods, then refund payment. */
  @Transactional
  public ReturnResponse inspectAndRefund(Long returnId, String disposition, String inspectionNote) {
    Return returnRequest = returnRepository.findById(returnId)
        .orElseThrow(() -> new ResourceNotFoundException("Return not found"));
    if ("REFUNDED".equals(returnRequest.getStatus())) return ReturnResponse.from(returnRequest);
    if (!"APPROVED".equals(returnRequest.getStatus())) {
      throw new IllegalStateException("Return is not awaiting warehouse receipt.");
    }
    if (!"SELLABLE".equals(disposition) && !"DAMAGED".equals(disposition)) {
      throw new IllegalArgumentException("disposition must be SELLABLE or DAMAGED");
    }

    returnRequest.setStatus("RECEIVED");
    returnRequest.setReceivedAt(Instant.now());
    returnRequest.setInspectionNote(inspectionNote);
    warehouseClient.inspectReturn(returnRequest, disposition, inspectionNote);

    PaymentClient.Refund refund = paymentClient.refund(
        returnRequest.getOrder().getId(), returnRequest.getId());
    if (!"SUCCEEDED".equals(refund.status())) {
      throw new IllegalStateException("The payment provider did not complete the refund.");
    }
    Order order = returnRequest.getOrder();
    order.setPaymentStatus("REFUNDED");
    orderStateMachine.transition(order, OrderEvent.REFUND_SUCCEEDED, refund.refundId());
    returnRequest.setStatus("REFUNDED");
    returnRequest.setRefundedAt(Instant.now());
    return ReturnResponse.from(returnRequest);
  }
}
