package com.recipebook;

import com.recipebook.domain.CookingState;
import com.recipebook.domain.DietFlag;
import com.recipebook.domain.Dish;
import com.recipebook.domain.DishCategory;
import com.recipebook.domain.DishIngredient;
import com.recipebook.domain.Nutrition;
import com.recipebook.domain.Product;
import com.recipebook.domain.ProductCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Dishes API")
class DishesApi extends RecipeApiIntegrationTestSupport {

        /**
         * Макрос в названии блюда: устанавливает категорию, удаляется из имени
         */
        @Test
        @DisplayName("Macro in dish name sets category and is removed (EP: macro present & category omitted)")
        void createDish_macro_setsCategory_andRemovesMacro() {
            String appleId = (String) createProductOrFail(product(runPrefix + "Apple", EnumSet.of(DietFlag.VEGAN, DietFlag.GLUTEN_FREE, DietFlag.SUGAR_FREE))).get("id");

            Dish dish = new Dish();
            dish.setName("  !десерт  Apple pie  ");
            dish.setPhotos(List.of());
            dish.setNutrition(new Nutrition(0, 0, 0, 0));
            dish.setIngredients(List.of(new DishIngredient(appleId, 100)));
            dish.setPortionSize(200);
            dish.setCategory(null);
            dish.setFlags(EnumSet.noneOf(DietFlag.class));

            Map<String, Object> created = createDishOrFail(dish);
            assertThat(created.get("category")).isEqualTo("DESSERT");
            assertThat((String) created.get("name")).isEqualTo("Apple pie");
        }

        /**
         * Категория из запроса: переопределяет категорию из макроса
         */
        @Test
        @DisplayName("Category in payload overrides macro category (EP: macro present & category provided)")
        void createDish_macro_ignored_whenCategoryProvided() {
            String waterId = (String) createProductOrFail(product(runPrefix + "Water", EnumSet.allOf(DietFlag.class))).get("id");

            Dish dish = new Dish();
            dish.setName("!напиток Lemonade");
            dish.setPhotos(List.of());
            dish.setNutrition(new Nutrition(0, 0, 0, 0));
            dish.setIngredients(List.of(new DishIngredient(waterId, 250)));
            dish.setPortionSize(250);
            dish.setCategory(DishCategory.SNACK);
            dish.setFlags(EnumSet.noneOf(DietFlag.class));

            Map<String, Object> created = createDishOrFail(dish);
            assertThat(created.get("category")).isEqualTo("SNACK");
            assertThat((String) created.get("name")).isEqualTo("Lemonade");
        }

        @ParameterizedTest(name = "create dish name=''{0}'' -> HTTP {1}")
        @CsvSource({
                "A,400",
                "Ab,201"
        })
        void createDish_nameLength_bva(String name, int expectedStatus) {
            String id = (String) createProductOrFail(product(runPrefix + "Base", EnumSet.noneOf(DietFlag.class))).get("id");
            Dish dish = new Dish();
            dish.setName(expectedStatus == 201 ? (runPrefix + name) : name);
            dish.setPhotos(List.of());
            dish.setNutrition(new Nutrition(0, 0, 0, 0));
            dish.setIngredients(List.of(new DishIngredient(id, 100)));
            dish.setPortionSize(100);
            dish.setCategory(DishCategory.SECOND);
            dish.setFlags(EnumSet.noneOf(DietFlag.class));
            ResponseEntity<Map> response = http.postForEntity(url("/dishes"), new HttpEntity<>(dish, jsonHeaders()), Map.class);
            assertThat(response.getStatusCode().value()).isEqualTo(expectedStatus);
        }

        @ParameterizedTest(name = "create dish portionSize={0} -> HTTP {1}")
        @CsvSource({
                "0.0,400",
                "0.01,201"
        })
        void createDish_portionSize_bva(double portionSize, int expectedStatus) {
            String id = (String) createProductOrFail(product(runPrefix + "Base", EnumSet.noneOf(DietFlag.class))).get("id");
            Dish dish = new Dish();
            dish.setName(runPrefix + "Portion");
            dish.setPhotos(List.of());
            dish.setNutrition(new Nutrition(0, 0, 0, 0));
            dish.setIngredients(List.of(new DishIngredient(id, 100)));
            dish.setPortionSize(portionSize);
            dish.setCategory(DishCategory.SECOND);
            dish.setFlags(EnumSet.noneOf(DietFlag.class));
            ResponseEntity<Map> response = http.postForEntity(url("/dishes"), new HttpEntity<>(dish, jsonHeaders()), Map.class);
            assertThat(response.getStatusCode().value()).isEqualTo(expectedStatus);
        }

        @ParameterizedTest(name = "create dish photos={0} -> HTTP {1}")
        @CsvSource({
                "0,201",
                "5,201",
                "6,400"
        })
        void createDish_photosCount_bva(int photoCount, int expectedStatus) {
            String id = (String) createProductOrFail(product(runPrefix + "Base", EnumSet.noneOf(DietFlag.class))).get("id");
            Dish dish = new Dish();
            dish.setName(runPrefix + "Dish photos");
            dish.setPhotos(fakePhotos(photoCount));
            dish.setNutrition(new Nutrition(0, 0, 0, 0));
            dish.setIngredients(List.of(new DishIngredient(id, 100)));
            dish.setPortionSize(100);
            dish.setCategory(DishCategory.SECOND);
            dish.setFlags(EnumSet.noneOf(DietFlag.class));
            ResponseEntity<Map> response = http.postForEntity(url("/dishes"), new HttpEntity<>(dish, jsonHeaders()), Map.class);
            assertThat(response.getStatusCode().value()).isEqualTo(expectedStatus);
        }

        /**
         * Флаги блюда автоматически удаляются, если ингредиенты их не поддерживают
         */
        @Test
        @DisplayName("Dish flags are automatically cleaned if ingredients don't allow them (EP: requested flag not allowed)")
        void createDish_flags_autoCleaned() {
            String veganOnlyId = (String) createProductOrFail(product(runPrefix + "Tofu", EnumSet.of(DietFlag.VEGAN))).get("id");
            String nonVeganId = (String) createProductOrFail(product(runPrefix + "Egg", EnumSet.noneOf(DietFlag.class))).get("id");

            Dish dish = new Dish();
            dish.setName("Tofu & Egg");
            dish.setPhotos(List.of());
            dish.setNutrition(new Nutrition(0, 0, 0, 0));
            dish.setIngredients(List.of(new DishIngredient(veganOnlyId, 100), new DishIngredient(nonVeganId, 50)));
            dish.setPortionSize(150);
            dish.setCategory(DishCategory.SECOND);
            dish.setFlags(EnumSet.of(DietFlag.VEGAN));

            Map<String, Object> created = createDishOrFail(dish);
            assertThat(created.get("flags")).asList().doesNotContain("VEGAN");
        }

        /**
         * Флаги блюда сохраняются, если все ингредиенты их поддерживают
         */
        @Test
        @DisplayName("Dish flags remain when all ingredients allow them (EP: requested flag allowed)")
        void createDish_flags_allowed_kept() {
            String id1 = (String) createProductOrFail(product(runPrefix + "Lentils", EnumSet.of(DietFlag.VEGAN, DietFlag.GLUTEN_FREE))).get("id");
            String id2 = (String) createProductOrFail(product(runPrefix + "Spice", EnumSet.of(DietFlag.VEGAN, DietFlag.GLUTEN_FREE))).get("id");

            Dish dish = new Dish();
            dish.setName("Vegan GF");
            dish.setPhotos(List.of());
            dish.setNutrition(new Nutrition(0, 0, 0, 0));
            dish.setIngredients(List.of(new DishIngredient(id1, 100), new DishIngredient(id2, 10)));
            dish.setPortionSize(110);
            dish.setCategory(DishCategory.SECOND);
            dish.setFlags(EnumSet.of(DietFlag.VEGAN, DietFlag.GLUTEN_FREE));

            Map<String, Object> created = createDishOrFail(dish);
            assertThat(created.get("flags")).asList().contains("VEGAN", "GLUTEN_FREE");
        }

        /**
         * Автоматический расчёт нутриентов применяется при создании
         */
        @Test
        @DisplayName("Auto-calculated nutrition is applied on create when fields are not set")
        void createDish_nutrition_autoCalculated() {
            String oats = (String) createProductOrFail(productWithNutrition(runPrefix + "Oats", new Nutrition(370, 13, 7, 62), EnumSet.noneOf(DietFlag.class))).get("id");
            String milk = (String) createProductOrFail(productWithNutrition(runPrefix + "Milk", new Nutrition(60, 3, 3, 5), EnumSet.noneOf(DietFlag.class))).get("id");

            Dish dish = new Dish();
            dish.setName("Oatmeal");
            dish.setPhotos(List.of());
            dish.setNutrition(new Nutrition(0, 0, 0, 0));
            dish.setIngredients(List.of(new DishIngredient(oats, 50), new DishIngredient(milk, 200)));
            dish.setPortionSize(250);
            dish.setCategory(DishCategory.SECOND);
            dish.setFlags(EnumSet.noneOf(DietFlag.class));

            Map<String, Object> created = createDishOrFail(dish);
            assertThat(((Number) ((Map<?, ?>) created.get("nutrition")).get("calories")).doubleValue()).isEqualTo(305.0);
            assertThat(((Number) ((Map<?, ?>) created.get("nutrition")).get("proteins")).doubleValue()).isEqualTo(12.5);
        }

        /**
         * Ручные значения нутриентов сохраняются
         */
        @Test
        @DisplayName("Manual nutrition values are preserved (TZ 2.2 allows user correction)")
        void createDish_nutrition_manual_override_kept() {
            String id = (String) createProductOrFail(productWithNutrition(runPrefix + "Something", new Nutrition(100, 10, 10, 10), EnumSet.noneOf(DietFlag.class))).get("id");
            Dish dish = new Dish();
            dish.setName("Manual");
            dish.setPhotos(List.of());
            dish.setNutrition(new Nutrition(999, 1, 2, 3));
            dish.setIngredients(List.of(new DishIngredient(id, 100)));
            dish.setPortionSize(100);
            dish.setCategory(DishCategory.SECOND);
            dish.setFlags(EnumSet.noneOf(DietFlag.class));

            Map<String, Object> created = createDishOrFail(dish);
            Map<?, ?> n = (Map<?, ?>) created.get("nutrition");
            assertThat(((Number) n.get("calories")).doubleValue()).isEqualTo(999.0);
            assertThat(((Number) n.get("proteins")).doubleValue()).isEqualTo(1.0);
        }

        /**
         * Список блюд:
         * поддерживает фильтры (поиск / категория / флаги)
         * сортируется по имени без учёта регистра
         */
        @Test
        @DisplayName("List dishes supports search/category/flags filters and is sorted by name case-insensitively")
        void listDishes_filters_and_sorting() {
            String veganId = (String) createProductOrFail(product(runPrefix + "Veg", EnumSet.of(DietFlag.VEGAN, DietFlag.GLUTEN_FREE))).get("id");
            String nonVeganId = (String) createProductOrFail(product(runPrefix + "NonVeg", EnumSet.noneOf(DietFlag.class))).get("id");

            createDishOrFail(dish(runPrefix + "bbb soup", DishCategory.SOUP, EnumSet.of(DietFlag.VEGAN), veganId));
            createDishOrFail(dish(runPrefix + "AAA salad", DishCategory.SALAD, EnumSet.of(DietFlag.VEGAN), veganId));
            createDishOrFail(dish(runPrefix + "ccc second", DishCategory.SECOND, EnumSet.noneOf(DietFlag.class), nonVeganId));

            ResponseEntity<List<Map<String, Object>>> all = http.exchange(
                    url("/dishes?search=" + runPrefix),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );
            assertThat(all.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(all.getBody()).extracting(d -> (String) d.get("name"))
                    .containsExactly(runPrefix + "AAA salad", runPrefix + "bbb soup", runPrefix + "ccc second");

            ResponseEntity<List<Map<String, Object>>> filtered = http.exchange(
                    // "bbb soup" contains runPrefix + "bbb", so this guarantees a match and still keeps the scope.
                    url("/dishes?search=" + runPrefix + "bbb&category=SOUP&flags=VEGAN"),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );
            assertThat(filtered.getBody()).extracting(d -> (String) d.get("name")).containsExactly(runPrefix + "bbb soup");
        }

        /**
         * количество ингредиентов в /dishes/calculate:
         * граничные значения:
         * 0 — невалидно
         * 0.01 — валидно
         */
        @ParameterizedTest(name = "calculateDraft amountGrams={0} -> HTTP {1}")
        @CsvSource({
                "0.0,400",
                "0.01,200"
        })
        void calculateDraft_amount_bva(double amountGrams, int expectedStatus) {
            String id = (String) createProductOrFail(product(runPrefix + "Sugar", EnumSet.of(DietFlag.SUGAR_FREE))).get("id");

            Map<String, Object> payload = Map.of(
                    "ingredients", List.of(Map.of("productId", id, "amountGrams", amountGrams))
            );

            ResponseEntity<Map> response = http.postForEntity(url("/dishes/calculate"), new HttpEntity<>(payload, jsonHeaders()), Map.class);
            assertThat(response.getStatusCode().value()).isEqualTo(expectedStatus);
        }

        /**
         * расчет правильности формулы КБЖУ и округление значений
         */
        @Test
        @DisplayName("Calculate draft follows TZ formula and rounds to 2 decimals")
        void calculateDraft_formula_and_rounding() {
            String p1 = (String) createProductOrFail(productWithNutrition(runPrefix + "P1", new Nutrition(123.456, 10, 20, 30), EnumSet.noneOf(DietFlag.class))).get("id");
            String p2 = (String) createProductOrFail(productWithNutrition(runPrefix + "P2", new Nutrition(10, 1.5, 2.25, 3.333), EnumSet.noneOf(DietFlag.class))).get("id");

            Map<String, Object> payload = Map.of(
                    "ingredients", List.of(
                            Map.of("productId", p1, "amountGrams", 50),
                            Map.of("productId", p2, "amountGrams", 33)
                    )
            );

            ResponseEntity<Map> response = http.postForEntity(url("/dishes/calculate"), new HttpEntity<>(payload, jsonHeaders()), Map.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(((Number) response.getBody().get("calories")).doubleValue()).isEqualTo(65.03);
            assertThat(((Number) response.getBody().get("carbs")).doubleValue()).isEqualTo(16.1);
        }

        /**
         * расчет правильности формулы КБЖУ и округление значений если продукта не существует
         */
        @Test
        @DisplayName("Calculate draft fails when productId does not exist (EP: invalid reference)")
        void calculateDraft_unknownProduct_400() {
            Map<String, Object> payload = Map.of(
                    "ingredients", List.of(Map.of("productId", "missing", "amountGrams", 10))
            );
            ResponseEntity<Map> response = http.postForEntity(url("/dishes/calculate"), new HttpEntity<>(payload, jsonHeaders()), Map.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        /**
         * Создание блюда падает,
         * если список ингредиентов пуст
         */
        @Test
        @DisplayName("Create dish fails when ingredients list is empty (EP: invalid partition)")
        void createDish_emptyIngredients_400() {
            Dish dish = new Dish();
            dish.setName("Empty");
            dish.setPhotos(List.of());
            dish.setNutrition(new Nutrition(0, 0, 0, 0));
            dish.setIngredients(List.of());
            dish.setPortionSize(100);
            dish.setCategory(DishCategory.SECOND);
            dish.setFlags(EnumSet.noneOf(DietFlag.class));
            ResponseEntity<Map> response = http.postForEntity(url("/dishes"), new HttpEntity<>(dish, jsonHeaders()), Map.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        private Product product(String name, EnumSet<DietFlag> flags) {
            Product product = new Product();
            product.setName(name);
            product.setPhotos(List.of());
            product.setNutrition(new Nutrition(10, 1, 1, 1));
            product.setComposition(null);
            product.setCategory(ProductCategory.VEGETABLES);
            product.setCookingState(CookingState.READY_TO_EAT);
            product.setFlags(flags);
            return product;
        }

        private Product productWithNutrition(String name, Nutrition nutrition, EnumSet<DietFlag> flags) {
            Product product = new Product();
            product.setName(name);
            product.setPhotos(List.of());
            product.setNutrition(nutrition);
            product.setComposition(null);
            product.setCategory(ProductCategory.VEGETABLES);
            product.setCookingState(CookingState.READY_TO_EAT);
            product.setFlags(flags);
            return product;
        }

        private Dish dish(String name, DishCategory category, EnumSet<DietFlag> flags, String productId) {
            Dish dish = new Dish();
            dish.setName(name);
            dish.setPhotos(List.of());
            dish.setNutrition(new Nutrition(0, 0, 0, 0));
            dish.setIngredients(List.of(new DishIngredient(productId, 100)));
            dish.setPortionSize(100);
            dish.setCategory(category);
            dish.setFlags(flags);
            return dish;
        }

        private List<String> fakePhotos(int count) {
            List<String> photos = new ArrayList<>();
            for (int i = 1; i <= count; i++) {
                photos.add("dish-photo-" + i + ".jpg");
            }
            return photos;
        }
    

    @Nested
    @DisplayName("GET/PUT/DELETE dish")
    class DishesApiGetPutDelete {
        /**
                 * GET/PUT/DELETE блюда
                 */
                @Test
                @DisplayName("GET/PUT/DELETE dish happy-path, including flags re-clean on update (TZ 2.4)")
                void dish_crud_and_flags_recomputed_on_update() {
                    String veganOnly = (String) createProductOrFail(product(runPrefix + "Tofu", EnumSet.of(DietFlag.VEGAN))).get("id");
                    String nonVegan = (String) createProductOrFail(product(runPrefix + "Egg", EnumSet.noneOf(DietFlag.class))).get("id");
        
                    Dish create = new Dish();
                    create.setName(runPrefix + "Initial");
                    create.setPhotos(List.of());
                    create.setNutrition(new Nutrition(0, 0, 0, 0));
                    create.setIngredients(List.of(new DishIngredient(veganOnly, 100)));
                    create.setPortionSize(100);
                    create.setCategory(DishCategory.SECOND);
                    create.setFlags(EnumSet.of(DietFlag.VEGAN));
        
                    Map<String, Object> created = createDishOrFail(create);
                    String dishId = (String) created.get("id");
                    assertThat(created.get("flags")).asList().contains("VEGAN");
        
                    ResponseEntity<Map> get = http.getForEntity(url("/dishes/" + dishId), Map.class);
                    assertThat(get.getStatusCode()).isEqualTo(HttpStatus.OK);
        
                    Dish update = new Dish();
                    update.setName(runPrefix + "Updated");
                    update.setPhotos(List.of());
                    update.setNutrition(new Nutrition(0, 0, 0, 0));
                    update.setIngredients(List.of(new DishIngredient(veganOnly, 100), new DishIngredient(nonVegan, 50)));
                    update.setPortionSize(150);
                    update.setCategory(DishCategory.SECOND);
                    update.setFlags(EnumSet.of(DietFlag.VEGAN));
        
                    ResponseEntity<Map> put = http.exchange(url("/dishes/" + dishId), HttpMethod.PUT, new HttpEntity<>(update, jsonHeaders()), Map.class);
                    assertThat(put.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(put.getBody()).containsEntry("id", dishId);
                    assertThat(put.getBody()).containsEntry("name", runPrefix + "Updated");
                    assertThat(put.getBody()).containsKey("updatedAt");
                    assertThat(put.getBody().get("flags")).asList().doesNotContain("VEGAN");
        
                    ResponseEntity<Map> delete = http.exchange(url("/dishes/" + dishId), HttpMethod.DELETE, null, Map.class);
                    assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
                }
    }
}
