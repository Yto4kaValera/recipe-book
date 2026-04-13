package com.recipebook.api;

import com.recipebook.domain.CookingState;
import com.recipebook.domain.DietFlag;
import com.recipebook.domain.Dish;
import com.recipebook.domain.DishCategory;
import com.recipebook.domain.DishDraftRequest;
import com.recipebook.domain.Nutrition;
import com.recipebook.domain.Product;
import com.recipebook.domain.ProductCategory;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api")
public class RecipeController {
    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/products")
    public List<Product> listProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(required = false) CookingState cookingState,
            @RequestParam(required = false) List<DietFlag> flags,
            @RequestParam(defaultValue = "name") String sortBy
    ) {
        return recipeService.listProducts(search, category, cookingState, flags, sortBy);
    }

    @GetMapping("/products/{id}")
    public Product getProduct(@PathVariable String id) {
        return recipeService.getProduct(id);
    }

    @PostMapping("/products")
    @ResponseStatus(CREATED)
    public Product createProduct(@Valid @RequestBody Product product) {
        return recipeService.createProduct(product);
    }

    @PutMapping("/products/{id}")
    public Product updateProduct(@PathVariable String id, @Valid @RequestBody Product product) {
        return recipeService.updateProduct(id, product);
    }

    @DeleteMapping("/products/{id}")
    @ResponseStatus(NO_CONTENT)
    public void deleteProduct(@PathVariable String id) {
        recipeService.deleteProduct(id);
    }

    @GetMapping("/dishes")
    public List<Dish> listDishes(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) DishCategory category,
            @RequestParam(required = false) List<DietFlag> flags
    ) {
        return recipeService.listDishes(search, category, flags);
    }

    @GetMapping("/dishes/{id}")
    public Dish getDish(@PathVariable String id) {
        return recipeService.getDish(id);
    }

    @PostMapping("/dishes")
    @ResponseStatus(CREATED)
    public Dish createDish(@Valid @RequestBody Dish dish) {
        return recipeService.createDish(dish);
    }

    @PutMapping("/dishes/{id}")
    public Dish updateDish(@PathVariable String id, @Valid @RequestBody Dish dish) {
        return recipeService.updateDish(id, dish);
    }

    @DeleteMapping("/dishes/{id}")
    @ResponseStatus(NO_CONTENT)
    public void deleteDish(@PathVariable String id) {
        recipeService.deleteDish(id);
    }

    @PostMapping("/dishes/calculate")
    public Nutrition calculateDraft(@Valid @RequestBody DishDraftRequest request) {
        return recipeService.calculateDishDraft(request.ingredients());
    }
}
