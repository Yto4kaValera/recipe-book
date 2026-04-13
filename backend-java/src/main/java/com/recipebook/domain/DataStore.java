package com.recipebook.domain;

import java.util.ArrayList;
import java.util.List;

public class DataStore {
    private List<Product> products = new ArrayList<>();
    private List<Dish> dishes = new ArrayList<>();

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    public List<Dish> getDishes() {
        return dishes;
    }

    public void setDishes(List<Dish> dishes) {
        this.dishes = dishes;
    }
}
