package com.recipebook.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class Dish {
    private String id = UUID.randomUUID().toString();

    @NotBlank
    @Size(min = 2)
    private String name;

    @Size(max = 5)
    private List<String> photos = new ArrayList<>();

    @Valid
    @NotNull
    private Nutrition nutrition;

    @Valid
    @Size(min = 1)
    private List<DishIngredient> ingredients = new ArrayList<>();

    @DecimalMin(value = "0.0", inclusive = false)
    private double portionSize;

    private DishCategory category;

    @NotNull
    private EnumSet<DietFlag> flags = EnumSet.noneOf(DietFlag.class);

    private Instant createdAt = Instant.now();
    private Instant updatedAt;

    public void normalize() {
        if (name != null) {
            name = name.trim();
        }
        if (photos == null) {
            photos = new ArrayList<>();
        }
        if (photos.size() > 5) {
            throw new IllegalArgumentException("У блюда не может быть больше 5 фотографий.");
        }
        if (ingredients == null || ingredients.isEmpty()) {
            throw new IllegalArgumentException("Состав блюда должен содержать хотя бы один продукт.");
        }
        if (flags == null) {
            flags = EnumSet.noneOf(DietFlag.class);
        }
        if (nutrition == null) {
            throw new IllegalArgumentException("КБЖУ блюда обязательно.");
        }
        nutrition.validateMacroSum();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getPhotos() {
        return photos;
    }

    public void setPhotos(List<String> photos) {
        this.photos = photos;
    }

    public Nutrition getNutrition() {
        return nutrition;
    }

    public void setNutrition(Nutrition nutrition) {
        this.nutrition = nutrition;
    }

    public List<DishIngredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<DishIngredient> ingredients) {
        this.ingredients = ingredients;
    }

    public double getPortionSize() {
        return portionSize;
    }

    public void setPortionSize(double portionSize) {
        this.portionSize = portionSize;
    }

    public DishCategory getCategory() {
        return category;
    }

    public void setCategory(DishCategory category) {
        this.category = category;
    }

    public EnumSet<DietFlag> getFlags() {
        return flags;
    }

    public void setFlags(EnumSet<DietFlag> flags) {
        this.flags = flags;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
