package com.recipebook.api;

import com.recipebook.domain.CookingState;
import com.recipebook.domain.DataStore;
import com.recipebook.domain.DietFlag;
import com.recipebook.domain.Dish;
import com.recipebook.domain.DishCategory;
import com.recipebook.domain.DishIngredient;
import com.recipebook.domain.Nutrition;
import com.recipebook.domain.Product;
import com.recipebook.domain.ProductCategory;
import com.recipebook.storage.JsonFileStorage;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

@Service
public class RecipeService {
    private static final Map<String, DishCategory> MACROS = createMacros();

    private final JsonFileStorage storage;

    public RecipeService(JsonFileStorage storage) {
        this.storage = storage;
    }

    public List<Product> listProducts(String search, ProductCategory category, CookingState cookingState, List<DietFlag> flags, String sortBy) {
        Predicate<Product> predicate = product -> true;
        if (search != null && !search.isBlank()) {
            String term = search.toLowerCase(Locale.ROOT);
            predicate = predicate.and(product -> product.getName().toLowerCase(Locale.ROOT).contains(term));
        }
        if (category != null) {
            predicate = predicate.and(product -> product.getCategory() == category);
        }
        if (cookingState != null) {
            predicate = predicate.and(product -> product.getCookingState() == cookingState);
        }
        if (flags != null) {
            for (DietFlag flag : flags) {
                predicate = predicate.and(product -> product.getFlags().contains(flag));
            }
        }

        return storage.read().getProducts().stream()
                .filter(predicate)
                .sorted(productComparator(sortBy))
                .toList();
    }

    public Product getProduct(String id) {
        return storage.read().getProducts().stream()
                .filter(product -> product.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Продукт не найден."));
    }

    public Product createProduct(Product product) {
        product.normalize();
        product.setId(java.util.UUID.randomUUID().toString());
        product.setCreatedAt(Instant.now());
        product.setUpdatedAt(null);
        DataStore dataStore = storage.read();
        dataStore.getProducts().add(product);
        storage.write(dataStore);
        return product;
    }

    public Product updateProduct(String id, Product request) {
        request.normalize();
        DataStore dataStore = storage.read();
        Product existing = findProduct(dataStore, id);
        request.setId(existing.getId());
        request.setCreatedAt(existing.getCreatedAt());
        request.setUpdatedAt(Instant.now());
        replaceProduct(dataStore, request);
        storage.write(dataStore);
        return request;
    }

    public void deleteProduct(String id) {
        DataStore dataStore = storage.read();
        List<String> dishNames = dataStore.getDishes().stream()
                .filter(dish -> dish.getIngredients().stream().anyMatch(ingredient -> ingredient.getProductId().equals(id)))
                .map(Dish::getName)
                .toList();
        if (!dishNames.isEmpty()) {
            throw new IllegalStateException("Нельзя удалить продукт. Он используется в блюдах: " + String.join(", ", dishNames));
        }

        boolean removed = dataStore.getProducts().removeIf(product -> product.getId().equals(id));
        if (!removed) {
            throw new IllegalArgumentException("Продукт не найден.");
        }
        storage.write(dataStore);
    }

    public List<Dish> listDishes(String search, DishCategory category, List<DietFlag> flags) {
        Predicate<Dish> predicate = dish -> true;
        if (search != null && !search.isBlank()) {
            String term = search.toLowerCase(Locale.ROOT);
            predicate = predicate.and(dish -> dish.getName().toLowerCase(Locale.ROOT).contains(term));
        }
        if (category != null) {
            predicate = predicate.and(dish -> dish.getCategory() == category);
        }
        if (flags != null) {
            for (DietFlag flag : flags) {
                predicate = predicate.and(dish -> dish.getFlags().contains(flag));
            }
        }

        return storage.read().getDishes().stream()
                .filter(predicate)
                .sorted(Comparator.comparing(Dish::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public Dish getDish(String id) {
        return storage.read().getDishes().stream()
                .filter(dish -> dish.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Блюдо не найдено."));
    }

    public Dish createDish(Dish dish) {
        DataStore dataStore = storage.read();
        prepareDish(dish, dataStore.getProducts());
        dish.setId(java.util.UUID.randomUUID().toString());
        dish.setCreatedAt(Instant.now());
        dish.setUpdatedAt(null);
        dataStore.getDishes().add(dish);
        storage.write(dataStore);
        return dish;
    }

    public Dish updateDish(String id, Dish request) {
        DataStore dataStore = storage.read();
        Dish existing = findDish(dataStore, id);
        prepareDish(request, dataStore.getProducts());
        request.setId(existing.getId());
        request.setCreatedAt(existing.getCreatedAt());
        request.setUpdatedAt(Instant.now());
        replaceDish(dataStore, request);
        storage.write(dataStore);
        return request;
    }

    public void deleteDish(String id) {
        DataStore dataStore = storage.read();
        boolean removed = dataStore.getDishes().removeIf(dish -> dish.getId().equals(id));
        if (!removed) {
            throw new IllegalArgumentException("Блюдо не найдено.");
        }
        storage.write(dataStore);
    }

    public Nutrition calculateDishDraft(List<DishIngredient> ingredients) {
        return calculateNutrition(ingredients, storage.read().getProducts());
    }

    private void prepareDish(Dish dish, List<Product> products) {
        dish.normalize();
        DishCategory categoryFromMacro = extractMacroCategory(dish);
        if (dish.getCategory() == null) {
            dish.setCategory(categoryFromMacro);
        }
        if (dish.getCategory() == null) {
            throw new IllegalArgumentException("Категория блюда обязательна.");
        }
        // Auto-calculate draft nutrition from ingredients (TZ 2.2).
        // If user adjusted any nutrition field manually, keep the provided value(s).
        Nutrition calculated = calculateNutrition(dish.getIngredients(), products);
        Nutrition provided = dish.getNutrition();
        if (provided == null) {
            dish.setNutrition(calculated);
        } else {
            if (isUnsetDraft(provided.getCalories())) {
                provided.setCalories(calculated.getCalories());
            }
            if (isUnsetDraft(provided.getProteins())) {
                provided.setProteins(calculated.getProteins());
            }
            if (isUnsetDraft(provided.getFats())) {
                provided.setFats(calculated.getFats());
            }
            if (isUnsetDraft(provided.getCarbs())) {
                provided.setCarbs(calculated.getCarbs());
            }
        }
        dish.setFlags(cleanDishFlags(dish.getFlags(), dish.getIngredients(), products));
    }

    private boolean isUnsetDraft(double value) {
        return Math.abs(value) < 0.0000001;
    }

    private EnumSet<DietFlag> cleanDishFlags(EnumSet<DietFlag> flags, List<DishIngredient> ingredients, List<Product> products) {
        Map<String, Product> productMap = buildProductMap(products);
        EnumSet<DietFlag> allowed = EnumSet.allOf(DietFlag.class);
        for (DishIngredient ingredient : ingredients) {
            Product product = Optional.ofNullable(productMap.get(ingredient.getProductId()))
                    .orElseThrow(() -> new IllegalArgumentException("Продукт в составе блюда не найден: " + ingredient.getProductId()));
            allowed.retainAll(product.getFlags());
        }
        if (flags == null || flags.isEmpty()) {
            return EnumSet.noneOf(DietFlag.class);
        }
        EnumSet<DietFlag> cleaned = EnumSet.copyOf(flags);
        cleaned.retainAll(allowed);
        return cleaned;
    }

    private Nutrition calculateNutrition(List<DishIngredient> ingredients, List<Product> products) {
        Map<String, Product> productMap = buildProductMap(products);
        double calories = 0;
        double proteins = 0;
        double fats = 0;
        double carbs = 0;
        if (ingredients == null || ingredients.isEmpty()) {
            throw new IllegalArgumentException("Список ингредиентов пуст.");
        }

        for (DishIngredient ingredient : ingredients) {
            if (ingredient.getAmountGrams() < 0) {
                throw new IllegalArgumentException("Количество ингредиента должно быть больше 0");
            }
            Product product = Optional.ofNullable(productMap.get(ingredient.getProductId()))
                    .orElseThrow(() -> new IllegalArgumentException("Продукт в составе блюда не найден: " + ingredient.getProductId()));
            double ratio = ingredient.getAmountGrams() / 100.0;
            calories += product.getNutrition().getCalories() * ratio;
            proteins += product.getNutrition().getProteins() * ratio;
            fats += product.getNutrition().getFats() * ratio;
            carbs += product.getNutrition().getCarbs() * ratio;
        }

        return new Nutrition(round(calories), round(proteins), round(fats), round(carbs));
    }

    private DishCategory extractMacroCategory(Dish dish) {
        String originalName = dish.getName();
        String lowered = originalName.toLowerCase(Locale.ROOT);
        int firstIndex = Integer.MAX_VALUE;
        String firstMacro = null;
        DishCategory firstCategory = null;

        for (Map.Entry<String, DishCategory> macro : MACROS.entrySet()) {
            int index = lowered.indexOf(macro.getKey());
            if (index >= 0 && index < firstIndex) {
                firstIndex = index;
                firstMacro = macro.getKey();
                firstCategory = macro.getValue();
            }
        }

        if (firstMacro != null) {
            String cleaned = (originalName.substring(0, firstIndex) + originalName.substring(firstIndex + firstMacro.length())).trim();
            dish.setName(cleaned.replaceAll("\\s+", " "));
            return firstCategory;
        }

        dish.setName(originalName.trim());
        return null;
    }

    private Product findProduct(DataStore dataStore, String id) {
        return dataStore.getProducts().stream()
                .filter(product -> product.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Продукт не найден."));
    }

    private Dish findDish(DataStore dataStore, String id) {
        return dataStore.getDishes().stream()
                .filter(dish -> dish.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Блюдо не найдено."));
    }

    private void replaceProduct(DataStore dataStore, Product product) {
        List<Product> products = new ArrayList<>(dataStore.getProducts());
        for (int index = 0; index < products.size(); index++) {
            if (products.get(index).getId().equals(product.getId())) {
                products.set(index, product);
                dataStore.setProducts(products);
                return;
            }
        }
    }

    private void replaceDish(DataStore dataStore, Dish dish) {
        List<Dish> dishes = new ArrayList<>(dataStore.getDishes());
        for (int index = 0; index < dishes.size(); index++) {
            if (dishes.get(index).getId().equals(dish.getId())) {
                dishes.set(index, dish);
                dataStore.setDishes(dishes);
                return;
            }
        }
    }

    private Map<String, Product> buildProductMap(List<Product> products) {
        Map<String, Product> productMap = new HashMap<>();
        for (Product product : products) {
            productMap.put(product.getId(), product);
        }
        return productMap;
    }

    private Comparator<Product> productComparator(String sortBy) {
        if ("calories".equalsIgnoreCase(sortBy)) {
            return Comparator.comparingDouble(product -> product.getNutrition().getCalories());
        }
        if ("proteins".equalsIgnoreCase(sortBy)) {
            return Comparator.comparingDouble(product -> product.getNutrition().getProteins());
        }
        if ("fats".equalsIgnoreCase(sortBy)) {
            return Comparator.comparingDouble(product -> product.getNutrition().getFats());
        }
        if ("carbs".equalsIgnoreCase(sortBy)) {
            return Comparator.comparingDouble(product -> product.getNutrition().getCarbs());
        }
        return Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static Map<String, DishCategory> createMacros() {
        Map<String, DishCategory> macros = new LinkedHashMap<>();
        macros.put("!десерт", DishCategory.DESSERT);
        macros.put("!первое", DishCategory.FIRST);
        macros.put("!второе", DishCategory.SECOND);
        macros.put("!напиток", DishCategory.DRINK);
        macros.put("!салат", DishCategory.SALAD);
        macros.put("!суп", DishCategory.SOUP);
        macros.put("!перекус", DishCategory.SNACK);
        return macros;
    }
}
