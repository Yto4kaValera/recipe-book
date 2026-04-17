package com.recipebook.domain;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;

public class Nutrition {
    @DecimalMin("0.0")
    private double calories;

    @DecimalMin("0.0")
    @Max(100)
    private double proteins;

    @DecimalMin("0.0")
    @Max(100)
    private double fats;

    @DecimalMin("0.0")
    @Max(100)
    private double carbs;

    public Nutrition() {
    }

    public Nutrition(double calories, double proteins, double fats, double carbs) {
        this.calories = calories;
        this.proteins = proteins;
        this.fats = fats;
        this.carbs = carbs;
    }

    public void validateProductMacroSum() {
        if (proteins + fats + carbs > 100) {
            throw new IllegalArgumentException("Сумма БЖУ не может превышать 100.");
        }
    }

    public double getCalories() {
        return calories;
    }

    public void setCalories(double calories) {
        this.calories = calories;
    }

    public double getProteins() {
        return proteins;
    }

    public void setProteins(double proteins) {
        this.proteins = proteins;
    }

    public double getFats() {
        return fats;
    }

    public void setFats(double fats) {
        this.fats = fats;
    }

    public double getCarbs() {
        return carbs;
    }

    public void setCarbs(double carbs) {
        this.carbs = carbs;
    }
}
