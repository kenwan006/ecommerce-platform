package com.example.shop.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.shop.entity.Product;
import com.example.shop.repository.ProductRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProductControllerTest {
  private final ProductRepository products = mock(ProductRepository.class);
  private final ProductController controller = new ProductController(products);

  @Test
  void returnsAllProductsFromTheRepository() {
    List<Product> expected = List.of(mock(Product.class), mock(Product.class));
    when(products.findAll()).thenReturn(expected);

    assertThat(controller.all()).isSameAs(expected);

    verify(products).findAll();
  }

  @Test
  void returnsTheRequestedProduct() {
    Product expected = mock(Product.class);
    when(products.findById(9L)).thenReturn(Optional.of(expected));

    assertThat(controller.one(9L)).isSameAs(expected);

    verify(products).findById(9L);
  }

  @Test
  void rejectsAnUnknownProduct() {
    when(products.findById(404L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> controller.one(404L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Product not found");
  }
}
