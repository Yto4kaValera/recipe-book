import { expect, test } from "@playwright/test";
import { createDish, createProduct, resetData } from "./helpers/api";
import { AppPage } from "./page-objects/app.po";
import { DishPage } from "./page-objects/dish.po";

test.describe("UI: dishes", () => {
  test.beforeEach(async ({ page, request }) => {
    await resetData(request);
    const app = new AppPage(page);
    await app.openDishes();
  });

  test.describe("Equivalent partitioning and boundaries", () => {
    /**
     * Проверяет, что при изменении состава блюда недопустимый флаг становится недоступен и автоматически снимается.
     */
    test("recomputes available dish flags when ingredients change", async ({ page, request }) => {
      const app = new AppPage(page);
      const dishes = new DishPage(page);

      await createProduct(request, {
        name: "Тофу",
        photos: [],
        nutrition: { calories: 76, proteins: 8, fats: 4.8, carbs: 1.9 },
        composition: null,
        category: "MEAT",
        cookingState: "READY_TO_EAT",
        flags: ["VEGAN", "GLUTEN_FREE", "SUGAR_FREE"]
      });
      await createProduct(request, {
        name: "Яйцо",
        photos: [],
        nutrition: { calories: 155, proteins: 13, fats: 11, carbs: 1.1 },
        composition: null,
        category: "MEAT",
        cookingState: "READY_TO_EAT",
        flags: []
      });

      await app.openDishes();
      await dishes.openForm();
      await dishes.fillForm({
        name: "Тофу боул",
        portionSize: 150,
        category: "SECOND",
        ingredients: [{ productName: "Тофу", amountGrams: 150 }]
      });

      const veganFlag = dishes.flagCheckbox("VEGAN");
      await expect(veganFlag).toBeEnabled();
      await veganFlag.check();
      await expect(veganFlag).toBeChecked();

      await dishes.fillForm({
        ingredients: [
          { productName: "Тофу", amountGrams: 150 },
          { productName: "Яйцо", amountGrams: 20 }
        ]
      });

      await expect(veganFlag).toBeDisabled();
      await expect(veganFlag).not.toBeChecked();
    });

    /**
     * Проверяет, что пустое имя блюда не проходит обязательную HTML-валидацию формы.
     */
    test("shows validation for empty dish name", async ({ page, request }) => {
      const app = new AppPage(page);
      const dishes = new DishPage(page);

      await createProduct(request, {
        name: "Вода",
        photos: [],
        nutrition: { calories: 0, proteins: 0, fats: 0, carbs: 0 },
        composition: null,
        category: "LIQUID",
        cookingState: "READY_TO_EAT",
        flags: ["VEGAN", "GLUTEN_FREE", "SUGAR_FREE"]
      });

      await page.reload();
      await app.openDishes();
      await dishes.openForm();
      await dishes.fillForm({
        name: "",
        portionSize: 100,
        category: "DRINK",
        ingredients: [{ productName: "Вода", amountGrams: 100 }]
      });
      expect(await dishes.nameInput.evaluate((element) => !element.checkValidity())).toBeTruthy();
    });

    /**
     * Проверяет, что для каждого ингредиента блюда обязательно должен быть выбран продукт.
     */
    test("requires product selection for each ingredient", async ({ page, request }) => {
      const app = new AppPage(page);
      const dishes = new DishPage(page);

      await createProduct(request, {
        name: "Вода",
        photos: [],
        nutrition: { calories: 0, proteins: 0, fats: 0, carbs: 0 },
        composition: null,
        category: "LIQUID",
        cookingState: "READY_TO_EAT",
        flags: ["VEGAN", "GLUTEN_FREE", "SUGAR_FREE"]
      });

      await page.reload();
      await app.openDishes();
      await dishes.openForm();
      await dishes.fillForm({
        name: "Лимонад",
        portionSize: 100,
        category: "DRINK"
      });
      await dishes.submit();
      await app.expectBanner("Для каждого ингредиента нужно выбрать продукт.");
    });

    /**
     * Проверяет нижнюю границу размера порции блюда: значение 0 не проходит HTML-валидацию.
     */
    test("validates dish portion size lower boundary", async ({ page, request }) => {
      const app = new AppPage(page);
      const dishes = new DishPage(page);

      await createProduct(request, {
        name: "Вода",
        photos: [],
        nutrition: { calories: 0, proteins: 0, fats: 0, carbs: 0 },
        composition: null,
        category: "LIQUID",
        cookingState: "READY_TO_EAT",
        flags: ["VEGAN", "GLUTEN_FREE", "SUGAR_FREE"]
      });

      await page.reload();
      await app.openDishes();
      await dishes.openForm();
      await dishes.fillForm({
        name: "Лимонад",
        portionSize: 0,
        category: "DRINK",
        ingredients: [{ productName: "Вода", amountGrams: 100 }],
        nutrition: { calories: 0, proteins: 0, fats: 0, carbs: 0 }
      });
      await dishes.submit();
      expect(await dishes.portionSizeInput.evaluate((element) => !element.checkValidity())).toBeTruthy();
    });
  });

  /**
   * Проверяет просмотр карточки блюда через UI, включая состав и размер порции.
   */
  test("opens dish details view", async ({ page, request }) => {
    const app = new AppPage(page);
    const dishes = new DishPage(page);

    const tomato = await createProduct(request, {
      name: "Томат",
      photos: [],
      nutrition: { calories: 18, proteins: 1, fats: 0.2, carbs: 3.9 },
      composition: null,
      category: "VEGETABLES",
      cookingState: "READY_TO_EAT",
      flags: ["VEGAN"]
    });

    await createDish(request, {
      name: "Салат",
      photos: [],
      nutrition: { calories: 18, proteins: 1, fats: 0.2, carbs: 3.9 },
      ingredients: [{ productId: tomato.id, amountGrams: 100 }],
      portionSize: 100,
      category: "SALAD",
      flags: ["VEGAN"]
    });

    await page.reload();
    await app.openDishes();
    await dishes.openCard("Салат");
    await expect(page.getByText("Томат")).toBeVisible();
    await expect(page.getByText("100 г", { exact: true })).toBeVisible();
  });

  /**
   * Проверяет редактирование блюда через UI и отображение новых значений после сохранения.
   */
  test("edits dish and shows updated values", async ({ page, request }) => {
    const app = new AppPage(page);
    const dishes = new DishPage(page);

    const tomato = await createProduct(request, {
      name: "Томат",
      photos: [],
      nutrition: { calories: 18, proteins: 1, fats: 0.2, carbs: 3.9 },
      composition: null,
      category: "VEGETABLES",
      cookingState: "READY_TO_EAT",
      flags: ["VEGAN"]
    });

    await createDish(request, {
      name: "Салат",
      photos: [],
      nutrition: { calories: 18, proteins: 1, fats: 0.2, carbs: 3.9 },
      ingredients: [{ productId: tomato.id, amountGrams: 100 }],
      portionSize: 100,
      category: "SALAD",
      flags: ["VEGAN"]
    });

    await page.reload();
    await app.openDishes();
    await dishes.openEdit("Салат");
    await dishes.fillForm({ name: "Салат обновленный", portionSize: 120 });
    const updateResponsePromise = page.waitForResponse((response) => response.request().method() === "PUT" && response.url().includes("/api/dishes/"));
    await dishes.submit();
    await updateResponsePromise;

    await app.expectBanner("Блюдо сохранено.");
    await expect(dishes.cards).toContainText("Салат обновленный");
    await dishes.openCard("Салат обновленный");
    await expect(page.getByText("120 г")).toBeVisible();
  });

  /**
   * Проверяет удаление блюда через UI и исчезновение карточки из списка.
   */
  test("deletes dish from the list", async ({ page, request }) => {
    const app = new AppPage(page);
    const dishes = new DishPage(page);

    const tomato = await createProduct(request, {
      name: "Томат",
      photos: [],
      nutrition: { calories: 18, proteins: 1, fats: 0.2, carbs: 3.9 },
      composition: null,
      category: "VEGETABLES",
      cookingState: "READY_TO_EAT",
      flags: ["VEGAN"]
    });

    await createDish(request, {
      name: "Салат",
      photos: [],
      nutrition: { calories: 18, proteins: 1, fats: 0.2, carbs: 3.9 },
      ingredients: [{ productId: tomato.id, amountGrams: 100 }],
      portionSize: 100,
      category: "SALAD",
      flags: ["VEGAN"]
    });

    await page.reload();
    await app.openDishes();
    const deleteResponsePromise = page.waitForResponse((response) => response.request().method() === "DELETE" && response.url().includes("/api/dishes/"));
    await dishes.delete("Салат");
    await deleteResponsePromise;

    await app.expectBanner("Блюдо удалено.");
    await expect(dishes.cards).not.toContainText("Салат");
  });
});
