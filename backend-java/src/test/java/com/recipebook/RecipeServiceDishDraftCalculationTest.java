package com.recipebook;

import com.recipebook.api.RecipeService;
import com.recipebook.domain.CookingState;
import com.recipebook.domain.DataStore;
import com.recipebook.domain.DietFlag;
import com.recipebook.domain.DishIngredient;
import com.recipebook.domain.Nutrition;
import com.recipebook.domain.Product;
import com.recipebook.domain.ProductCategory;
import com.recipebook.storage.JsonFileStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * тесты для автоматического расчета калорийности в {@link RecipeService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecipeService.calculateDishDraft")
class RecipeServiceDishDraftCalculationTest {

    @Mock
    private JsonFileStorage storage;

    private RecipeService service;

    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        service = new RecipeService(storage);

        product1 = product("product1", 120.0, 10.0, 5.0, 8.0);
        product2 = product("product2", 350.0, 7.0, 1.0, 78.0);

        DataStore dataStore = new DataStore();
        dataStore.setProducts(List.of(product1, product2));

        when(storage.read()).thenReturn(dataStore);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(storage);
    }

    static Stream<Arguments> equivalentPartitionCases() {
        return Stream.of(
                Arguments.of(0.0, 0.0),
                Arguments.of(150.0, 180.0),
                Arguments.of(33.33, 40.0)
        );
    }
    @Nested
    @DisplayName("Equivalent partitioning")
    class EquivalentPartitioning {

        /**
         * разбивки по количеству ингредиентов:
         * нулевое количество, допустимое количество и дробное количество с округлением.
         */
        @ParameterizedTest(name = "[{index}] amount={0} g -> calories={1} kcal")
        @MethodSource("com.recipebook.RecipeServiceDishDraftCalculationTest#equivalentPartitionCases")
        @DisplayName("calculates calories for representative valid equivalence classes")
        void calculatesCaloriesForRepresentativeValidClasses(double amountGrams, double expectedCalories) {
            Nutrition nutrition = service.calculateDishDraft(List.of(new DishIngredient(product1.getId(), amountGrams)));

            Assertions.assertEquals(expectedCalories, nutrition.getCalories());
            verify(storage).read();
        }
    }

    static Stream<Arguments> boundaryValueCases() {
        return Stream.of(
                Arguments.of(0.0, 0.0),
                Arguments.of(0.01, 0.01),
                Arguments.of(1.0, 1.2),
                Arguments.of(100.0, 120.0)
        );
    }
    @Nested
    @DisplayName("Boundary value analysis")
    class BoundaryValueAnalysis {

        /**
         * граничные значения
         */
        @ParameterizedTest(name = "[{index}] amount={0} g -> calories={1} kcal")
        @MethodSource("com.recipebook.RecipeServiceDishDraftCalculationTest#boundaryValueCases")
        @DisplayName("calculates calories on boundary values")
        void calculatesCaloriesOnBoundaryValues(double amountGrams, double expectedCalories) {
            Nutrition nutrition = service.calculateDishDraft(List.of(new DishIngredient(product1.getId(), amountGrams)));

            Assertions.assertEquals(expectedCalories, nutrition.getCalories());
            verify(storage).read();
        }
    }

    /**
     * полностью КБЖУ
     */
    @Test
    @DisplayName("aggregates nutrition contributions from all ingredients in one portion")
    void aggregatesNutritionForMultipleIngredients() {
        Nutrition nutrition = service.calculateDishDraft(List.of(
                new DishIngredient(product1.getId(), 150.0),
                new DishIngredient(product2.getId(), 50.0)
        ));

        Assertions.assertAll(
                () -> Assertions.assertEquals(355.0, nutrition.getCalories()),
                () -> Assertions.assertEquals(18.5, nutrition.getProteins()),
                () -> Assertions.assertEquals(8.0, nutrition.getFats()),
                () -> Assertions.assertEquals(51.0, nutrition.getCarbs())
        );
        verify(storage).read();
    }

    /**
     * отсутствие продукта
     */
    @Test
    @DisplayName("throws an exception when an ingredient references an unknown product")
    void throwsWhenIngredientProductIsMissing() {
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.calculateDishDraft(List.of(new DishIngredient("missing-product", 100.0)))
        );

        Assertions.assertTrue(exception.getMessage().toLowerCase().contains("не найден"));
        verify(storage).read();
    }

    /**
     * отрицательные значения
     */
    static Stream<Arguments> negativeValues() {
        return Stream.of(
                Arguments.of(-1.0),
                Arguments.of(-10.0),
                Arguments.of(-0.01)
        );
    }
    @ParameterizedTest
    @MethodSource("negativeValues")
    void rejectsNegativeIngredientAmount(double amount) {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.calculateDishDraft(
                        List.of(new DishIngredient(product1.getId(), amount))
                )
        );
    }

    /**
     * 100гр каждого продукта
     */
    @Test
    @DisplayName("calculates nutrition correctly when each ingredient is 100g")
    void calculatesNutritionFor100GramsEachIngredient() {

        Nutrition nutrition = service.calculateDishDraft(List.of(
                new DishIngredient(product1.getId(), 100.0),
                new DishIngredient(product2.getId(), 100.0)
        ));

        Assertions.assertAll(
                () -> Assertions.assertEquals(470.0, nutrition.getCalories()),
                () -> Assertions.assertEquals(17.0, nutrition.getProteins()),
                () -> Assertions.assertEquals(6.0, nutrition.getFats()),
                () -> Assertions.assertEquals(86.0, nutrition.getCarbs())
        );

        verify(storage).read();
    }

    /**
     * 1гр каждого продукта
     */
    @Test
    @DisplayName("calculates nutrition correctly when each ingredient is 1g")
    void calculatesNutritionFor1GramEachIngredient() {

        Nutrition nutrition = service.calculateDishDraft(List.of(
                new DishIngredient(product1.getId(), 1.0),
                new DishIngredient(product2.getId(), 1.0)
        ));

        Assertions.assertAll(
                () -> Assertions.assertEquals(4.7, nutrition.getCalories()),
                () -> Assertions.assertEquals(0.17, nutrition.getProteins()),
                () -> Assertions.assertEquals(0.06, nutrition.getFats()),
                () -> Assertions.assertEquals(0.86, nutrition.getCarbs())
        );

        verify(storage).read();
    }

    /**
     * отрицательные граммы каждого продукта
     */
    @Test
    @DisplayName("throws exception when ingredient amount is negative")
    void rejectsNegativeIngredientAmountInDish() {

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.calculateDishDraft(List.of(
                        new DishIngredient(product1.getId(), -1.0),
                        new DishIngredient(product2.getId(), -1.0)
                ))
        );

        verify(storage).read();
    }

    /**
     * пустой список продуктов
     */
    @Test
    @DisplayName("throws exception when ingredient list is empty")
    void rejectsEmptyIngredients() {

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.calculateDishDraft(List.of())
        );
    }

    private Product product(String id, double calories, double proteins, double fats, double carbs) {
        Product product = new Product();
        product.setId(id);
        product.setName(id);
        product.setCategory(ProductCategory.VEGETABLES);
        product.setCookingState(CookingState.READY_TO_EAT);
        product.setNutrition(new Nutrition(calories, proteins, fats, carbs));
        product.setFlags(EnumSet.of(DietFlag.VEGAN));
        return product;
    }

}
