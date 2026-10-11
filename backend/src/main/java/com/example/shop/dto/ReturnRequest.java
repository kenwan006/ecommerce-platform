package com.example.shop.dto;

import jakarta.validation.constraints.Size;

public record ReturnRequest(@Size(max = 500) String reason) {}
