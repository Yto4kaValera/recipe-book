package com.recipebook;

import com.recipebook.api.RecipeService;
import com.recipebook.domain.CookingState;
import com.recipebook.domain.DietFlag;
import com.recipebook.domain.Dish;
import com.recipebook.domain.DishIngredient;
import com.recipebook.domain.Nutrition;
import com.recipebook.domain.Product;
import com.recipebook.domain.ProductCategory;
import com.recipebook.storage.JsonFileStorage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;

class RecipeServiceTest {
    @Test
    void calculatesDishDraft() throws Exception {
        Path tempFile = Files.createTempFile("recipe-book", ".json");
        RecipeService service = new RecipeService(new JsonFileStorage(tempFile.toString()));

        Product product = new Product();
        product.setName("Tofu");
        product.setCategory(ProductCategory.VEGETABLES);
        product.setCookingState(CookingState.READY_TO_EAT);
        product.setNutrition(new Nutrition(120, 10, 5, 8));
        product.setFlags(EnumSet.of(DietFlag.VEGAN));
        service.createProduct(product);

        Nutrition nutrition = service.calculateDishDraft(List.of(new DishIngredient(product.getId(), 150)));

        Assertions.assertEquals(180.0, nutrition.getCalories());
        Assertions.assertEquals(15.0, nutrition.getProteins());
    }

    @Test
    void blocksDeletingUsedProduct() throws Exception {
        Path tempFile = Files.createTempFile("recipe-book", ".json");
        RecipeService service = new RecipeService(new JsonFileStorage(tempFile.toString()));

        Product product = new Product();
        product.setName("Tomato");
        product.setCategory(ProductCategory.VEGETABLES);
        product.setCookingState(CookingState.READY_TO_EAT);
        product.setNutrition(new Nutrition(18, 1, 0, 3));
        product.setFlags(EnumSet.of(DietFlag.VEGAN));
        service.createProduct(product);

        Dish dish = new Dish();
        dish.setName("!салат Tomato salad");
        dish.setNutrition(new Nutrition(18, 1, 0, 3));
        dish.setPortionSize(100);
        dish.setIngredients(List.of(new DishIngredient(product.getId(), 100)));
        dish.setFlags(EnumSet.of(DietFlag.VEGAN));
        service.createDish(dish);

        Assertions.assertThrows(IllegalStateException.class, () -> service.deleteProduct(product.getId()));
    }
}
