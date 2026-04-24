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

@DisplayName("Products API")
class ProductsApi extends RecipeApiIntegrationTestSupport {
        /**
         * длина имени продукта:
         * невалидный класс: длина 0..1
         * валидный класс: длина ≥ 2
         * граничные значения:
         * 1 — невалидно
         * 2 — валидно
         */
        @ParameterizedTest(name = "create product name=''{0}'' -> HTTP {1}")
        @CsvSource({
                "A,400",
                "Ab,201"
        })
        void createProduct_nameLength_bva(String name, int expectedStatus) {
            if (expectedStatus == 201) {
                name = runPrefix + name;
            }
            Product product = baseProduct(name, new Nutrition(10, 10, 10, 10));
            ResponseEntity<Map> response = http.postForEntity(url("/products"), new HttpEntity<>(product, jsonHeaders()), Map.class);
            assertThat(response.getStatusCode().value()).isEqualTo(expectedStatus);
        }

        /**
         * ограничения суммы БЖУ (белки + жиры + углеводы ≤ 100)
         */
        @ParameterizedTest(name = "create product macroSum={0} -> HTTP {1}")
        @CsvSource({
                "100.00,201",
                "100.01,400"
        })
        void createProduct_macroSum_bva(double macroSum, int expectedStatus) {
            // Keep each macro <= 100 to test sum constraint, not @Max(100) on a single field.
            Nutrition nutrition = (macroSum <= 100.0)
                    ? new Nutrition(123, 50, 50, 0.0)   // sum=100
                    : new Nutrition(123, 50, 50, 0.01); // sum=100.01
            Product product = baseProduct(runPrefix + "MacroSum", nutrition);

            ResponseEntity<Map> response = http.postForEntity(url("/products"), new HttpEntity<>(product, jsonHeaders()), Map.class);
            assertThat(response.getStatusCode().value()).isEqualTo(expectedStatus);
        }

        /**
         * количество фотографий:
         * 0–5 — валидно
         * 6 — невалидно
         */
        @ParameterizedTest(name = "create product photos={0} -> HTTP {1}")
        @CsvSource({
                "0,201",
                "5,201",
                "6,400"
        })
        void createProduct_photosCount_bva(int photoCount, int expectedStatus) {
            Product product = baseProduct(runPrefix + "Photos", new Nutrition(10, 10, 10, 10));
            product.setPhotos(fakePhotos(photoCount));
            ResponseEntity<Map> response = http.postForEntity(url("/products"), new HttpEntity<>(product, jsonHeaders()), Map.class);
            assertThat(response.getStatusCode().value()).isEqualTo(expectedStatus);
        }

        /**
         *подстрока без учёта регистра
         * */
        @Test
        @DisplayName("Search is case-insensitive substring (EP: match vs no match)")
        void listProducts_search_caseInsensitiveSubstring() {
            createProductOrFail(baseProduct(runPrefix + "Tomato", new Nutrition(18, 1, 0.2, 3.9)));
            createProductOrFail(baseProduct(runPrefix + "Cucumber", new Nutrition(16, 0.7, 0.1, 3.6)));

            ResponseEntity<List<Map<String, Object>>> response = http.exchange(
                    url("/products?search=" + runPrefix + "toMA"),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).extracting(p -> (String) p.get("name")).containsExactly(runPrefix + "Tomato");
        }

        /**
         * список продуктов поддерживает комбинированные фильтры: категория + состояние приготовления + флаги
         */
        @Test
        @DisplayName("List products supports combined filters: category + cookingState + flags (EP: matches all vs filtered out)")
        void listProducts_filters_combined() {
            createProductOrFail(product(ProductCategory.GRAINS, CookingState.NEEDS_COOKING, EnumSet.of(DietFlag.VEGAN), runPrefix + "Buckwheat", new Nutrition(330, 12, 3, 60)));
            createProductOrFail(product(ProductCategory.MEAT, CookingState.NEEDS_COOKING, EnumSet.noneOf(DietFlag.class), runPrefix + "Chicken", new Nutrition(200, 20, 10, 0)));
            createProductOrFail(product(ProductCategory.GRAINS, CookingState.READY_TO_EAT, EnumSet.of(DietFlag.VEGAN), runPrefix + "Granola", new Nutrition(450, 10, 15, 65)));

            ResponseEntity<List<Map<String, Object>>> response = http.exchange(
                    url("/products?search=" + runPrefix + "&category=GRAINS&cookingState=NEEDS_COOKING&flags=VEGAN"),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).extracting(p -> (String) p.get("name")).containsExactly(runPrefix + "Buckwheat");
        }

        /**
         * Сортировка работает по: калориям, белкам, жирам, углеводам, имени
         */
        @Test
        @DisplayName("Sorting works for calories/proteins/fats/carbs/name")
        void listProducts_sorting_allCriteria() {
            createProductOrFail(baseProduct(runPrefix + "bbb", new Nutrition(20, 2, 2, 2)));
            createProductOrFail(baseProduct(runPrefix + "aaa", new Nutrition(10, 1, 3, 4)));
            createProductOrFail(baseProduct(runPrefix + "ccc", new Nutrition(30, 0.5, 1, 1)));

            assertSorted(url("/products?search=" + runPrefix + "&sortBy=name"), List.of(runPrefix + "aaa", runPrefix + "bbb", runPrefix + "ccc"));
            assertSorted(url("/products?search=" + runPrefix + "&sortBy=calories"), List.of(runPrefix + "aaa", runPrefix + "bbb", runPrefix + "ccc"));
            assertSorted(url("/products?search=" + runPrefix + "&sortBy=proteins"), List.of(runPrefix + "ccc", runPrefix + "aaa", runPrefix + "bbb"));
            assertSorted(url("/products?search=" + runPrefix + "&sortBy=fats"), List.of(runPrefix + "ccc", runPrefix + "bbb", runPrefix + "aaa"));
            assertSorted(url("/products?search=" + runPrefix + "&sortBy=carbs"), List.of(runPrefix + "ccc", runPrefix + "bbb", runPrefix + "aaa"));
        }

        /**
         * удаление продукта, который используется в блюде
         */
        @Test
        @DisplayName("DELETE product used in a dish returns 409 and names dishes")
        void deleteProduct_usedInDish_conflict() {
            String productId = (String) createProductOrFail(baseProduct(runPrefix + "Rice", new Nutrition(350, 7, 0.7, 78))).get("id");

            Dish dish = new Dish();
            dish.setName(runPrefix + "Rice bowl");
            dish.setPhotos(List.of());
            dish.setNutrition(new Nutrition(0, 0, 0, 0));
            dish.setIngredients(List.of(new DishIngredient(productId, 150)));
            dish.setPortionSize(200);
            dish.setCategory(DishCategory.SECOND);
            dish.setFlags(EnumSet.noneOf(DietFlag.class));
            createDishOrFail(dish);

            ResponseEntity<Map> deleteResponse = http.exchange(url("/products/" + productId), HttpMethod.DELETE, null, Map.class);
            assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat((String) deleteResponse.getBody().get("message")).contains(runPrefix + "Rice bowl");
        }

        /**
         * удаление продукта, который нигде не используется
         */
        @Test
        @DisplayName("DELETE product not used in any dish is allowed (EP: deletable partition)")
        void deleteProduct_notUsed_ok() {
            String productId = (String) createProductOrFail(baseProduct(runPrefix + "Salt", new Nutrition(0, 0, 0, 0))).get("id");
            ResponseEntity<Map> delete = http.exchange(url("/products/" + productId), HttpMethod.DELETE, null, Map.class);
            assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            createdProductIds.remove(productId);
        }

        private Product baseProduct(String name, Nutrition nutrition) {
            Product product = new Product();
            product.setName(name);
            product.setPhotos(List.of());
            product.setNutrition(nutrition);
            product.setComposition(null);
            product.setCategory(ProductCategory.GRAINS);
            product.setCookingState(CookingState.NEEDS_COOKING);
            product.setFlags(EnumSet.noneOf(DietFlag.class));
            return product;
        }

        private Product product(ProductCategory category, CookingState cookingState, EnumSet<DietFlag> flags, String name, Nutrition nutrition) {
            Product product = baseProduct(name, nutrition);
            product.setCategory(category);
            product.setCookingState(cookingState);
            product.setFlags(flags);
            return product;
        }

        private void assertSorted(String fullUrl, List<String> expectedNamesInOrder) {
            ResponseEntity<List<Map<String, Object>>> response = http.exchange(
                    fullUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).extracting(p -> (String) p.get("name")).containsExactlyElementsOf(expectedNamesInOrder);
        }

        private List<String> fakePhotos(int count) {
            List<String> photos = new ArrayList<>();
            for (int i = 1; i <= count; i++) {
                photos.add("photo-" + i + ".jpg");
            }
            return photos;
        }
    

    @Nested
    @DisplayName("GET/PUT/DELETE product")
    class ProductsApiGetPutDelete {
        /**
                 * GET/PUT/DELETE продукта
                 */
                @Test
                @DisplayName("GET/PUT/DELETE product happy-path and not-found (EP)")
                void product_crud_and_notFound() {
                    Map<String, Object> created = createProductOrFail(baseProduct(runPrefix + "Milk", new Nutrition(60, 3, 3, 5)));
                    String id = (String) created.get("id");
        
                    ResponseEntity<Map> get = http.getForEntity(url("/products/" + id), Map.class);
                    assertThat(get.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(get.getBody()).containsEntry("name", runPrefix + "Milk");
        
                    Product update = baseProduct(runPrefix + "Milk 2%", new Nutrition(50, 3.1, 2.0, 4.8));
                    ResponseEntity<Map> put = http.exchange(url("/products/" + id), HttpMethod.PUT, new HttpEntity<>(update, jsonHeaders()), Map.class);
                    assertThat(put.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(put.getBody()).containsEntry("id", id);
                    assertThat(put.getBody()).containsEntry("name", runPrefix + "Milk 2%");
                    assertThat(put.getBody()).containsKey("updatedAt");
        
                    ResponseEntity<Map> delete = http.exchange(url("/products/" + id), HttpMethod.DELETE, null, Map.class);
                    assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        
                    createdProductIds.remove(id);
        
                    ResponseEntity<Map> getAfterDelete = http.getForEntity(url("/products/" + id), Map.class);
                    assertThat(getAfterDelete.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        
                    ResponseEntity<Map> getUnknown = http.getForEntity(url("/products/does-not-exist"), Map.class);
                    assertThat(getUnknown.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                }
    }
}
