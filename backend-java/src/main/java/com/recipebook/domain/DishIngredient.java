package com.recipebook.domain;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

public class DishIngredient {
    @NotBlank
    private String productId;

    @DecimalMin(value = "0.0", inclusive = false)
    private double amountGrams;

    public DishIngredient() {
    }

    public DishIngredient(String productId, double amountGrams) {
        this.productId = productId;
        this.amountGrams = amountGrams;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public double getAmountGrams() {
        return amountGrams;
    }

    public void setAmountGrams(double amountGrams) {
        this.amountGrams = amountGrams;
    }
}
