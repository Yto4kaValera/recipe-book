import { expect, test } from "@playwright/test";
import { createDish, createProduct, resetData } from "./helpers/api";
import { AppPage } from "./page-objects/app.po";
import { ProductPage } from "./page-objects/product.po";

function buildPhotoFiles(count) {
  return Array.from({ length: count }, (_, index) => ({
    name: `photo-${index + 1}.svg`,
    mimeType: "image/svg+xml",
    buffer: Buffer.from(
      `<svg xmlns="http://www.w3.org/2000/svg" width="2" height="2"><rect width="2" height="2" fill="rgb(${index},0,0)"/></svg>`,
      "utf8"
    )
  }));
}

test.describe("UI: products", () => {
  test.beforeEach(async ({ page, request }) => {
    await resetData(request);
    const app = new AppPage(page);
    await app.openProducts();
  });

  test.describe("Boundary value analysis", () => {
    /**
     * Проверяет граничные значения имени продукта и суммы БЖУ:
     * имя из 1 символа не проходит HTML-валидацию,
     * сумма БЖУ 100.01 отклоняется прикладной валидацией.
     */
    test("validates product lower boundaries for name length and upper boundary for macro sum", async ({ page }) => {
      const app = new AppPage(page);
      const products = new ProductPage(page);

      await products.openForm();
      await products.fillForm({
        name: "A",
        nutrition: { calories: 20, proteins: 10, fats: 10, carbs: 10 },
        composition: "Тестовые данные",
        category: "VEGETABLES",
        cookingState: "READY_TO_EAT",
        flags: []
      });
      await products.submit();
      await expect(products.form).toBeVisible();
      expect(await products.nameInput.evaluate((element) => !element.checkValidity())).toBeTruthy();

      await products.closeButton.click();
      await products.openForm();
      await products.fillForm({
        name: "Манго",
        nutrition: { calories: 60, proteins: 50, fats: 50, carbs: 0.01 },
        composition: "Тестовые данные",
        category: "VEGETABLES",
        cookingState: "READY_TO_EAT",
        flags: []
      });
      await products.submit();
      await app.expectBanner("Сумма БЖУ на 100 грамм не может превышать 100.");
    });

    /**
     * Проверяет границы количества фото продукта:
     * 0 фото допустимо, 5 фото допустимо, при 6 фото срабатывает ограничение.
     */
    test("shows photo limit behavior for 0, 5 and 6 files", async ({ page }) => {
      const app = new AppPage(page);
      const products = new ProductPage(page);

      await products.openForm();
      await expect(products.deletePhotoButtons()).toHaveCount(0);

      await products.attachPhotos(buildPhotoFiles(5));
      await expect(products.deletePhotoButtons()).toHaveCount(5);

      await products.closeButton.click();
      await products.openForm();
      await products.attachPhotos(buildPhotoFiles(6));
      await app.expectBanner("Можно загрузить не больше 5 фотографий.");
      await expect(products.deletePhotoButtons()).toHaveCount(5);
    });
  });

  test.describe("Equivalent partitioning", () => {
    /**
     * Проверяет комбинированную фильтрацию по поиску, категории, готовности и флагу.
     */
    test("filters by search, category, cooking state and flags", async ({ page, request }) => {
      const products = new ProductPage(page);

      await createProduct(request, {
        name: "Гречка ядрица",
        photos: [],
        nutrition: { calories: 313, proteins: 12.6, fats: 3.3, carbs: 62.1 },
        composition: null,
        category: "GRAINS",
        cookingState: "NEEDS_COOKING",
        flags: ["VEGAN"]
      });
      await createProduct(request, {
        name: "Курица",
        photos: [],
        nutrition: { calories: 190, proteins: 20, fats: 12, carbs: 0 },
        composition: null,
        category: "MEAT",
        cookingState: "NEEDS_COOKING",
        flags: []
      });
      await createProduct(request, {
        name: "Гранола",
        photos: [],
        nutrition: { calories: 450, proteins: 10, fats: 16, carbs: 64 },
        composition: null,
        category: "GRAINS",
        cookingState: "READY_TO_EAT",
        flags: ["VEGAN"]
      });

      await page.reload();
      const app = new AppPage(page);
      await app.openProducts();
      await products.filter({
        search: "греч",
        category: "GRAINS",
        cookingState: "NEEDS_COOKING",
        flags: { VEGAN: true }
      });

      await expect(products.cards).toContainText("Гречка ядрица");
      await expect(products.cards).not.toContainText("Курица");
      await expect(products.cards).not.toContainText("Гранола");
    });

    /**
     * Проверяет сортировку списка продуктов по всем поддерживаемым полям.
     */
    test("sorts products by every supported field", async ({ page, request }) => {
      const products = new ProductPage(page);

      await createProduct(request, {
        name: "Бета",
        photos: [],
        nutrition: { calories: 20, proteins: 2, fats: 2, carbs: 2 },
        composition: null,
        category: "GRAINS",
        cookingState: "NEEDS_COOKING",
        flags: []
      });
      await createProduct(request, {
        name: "Альфа",
        photos: [],
        nutrition: { calories: 10, proteins: 1, fats: 3, carbs: 4 },
        composition: null,
        category: "GRAINS",
        cookingState: "NEEDS_COOKING",
        flags: []
      });
      await createProduct(request, {
        name: "Гамма",
        photos: [],
        nutrition: { calories: 30, proteins: 0.5, fats: 1, carbs: 1 },
        composition: null,
        category: "GRAINS",
        cookingState: "NEEDS_COOKING",
        flags: []
      });

      await page.reload();
      const app = new AppPage(page);
      await app.openProducts();
      const cases = [
        { sortBy: "name", expected: ["Альфа", "Бета", "Гамма"] },
        { sortBy: "calories", expected: ["Альфа", "Бета", "Гамма"] },
        { sortBy: "proteins", expected: ["Гамма", "Альфа", "Бета"] },
        { sortBy: "fats", expected: ["Гамма", "Бета", "Альфа"] },
        { sortBy: "carbs", expected: ["Гамма", "Бета", "Альфа"] }
      ];

      for (const sortCase of cases) {
        await products.filter({ sortBy: sortCase.sortBy });
        await expect.poll(async () => products.cardTitles()).toEqual(sortCase.expected);
      }
    });
  });

  /**
   * Проверяет просмотр карточки продукта через UI со всеми важными атрибутами.
   */
  test("opens product details view", async ({ page, request }) => {
    const app = new AppPage(page);
    const products = new ProductPage(page);

    await createProduct(request, {
      name: "Томат",
      photos: [],
      nutrition: { calories: 18, proteins: 1, fats: 0.2, carbs: 3.9 },
      composition: "Свежий томат",
      category: "VEGETABLES",
      cookingState: "READY_TO_EAT",
      flags: ["VEGAN"]
    });

    await page.reload();
    await app.openProducts();
    await products.openCard("Томат");
    await expect(page.getByText("Свежий томат")).toBeVisible();
    await expect(page.getByText("Веган")).toBeVisible();
  });

  /**
   * Проверяет редактирование продукта через UI и отображение изменённых данных после сохранения.
   */
  test("edits product and shows updated values", async ({ page, request }) => {
    const app = new AppPage(page);
    const products = new ProductPage(page);

    await createProduct(request, {
      name: "Кефир",
      photos: [],
      nutrition: { calories: 50, proteins: 3, fats: 2, carbs: 4 },
      composition: "Молочный продукт",
      category: "LIQUID",
      cookingState: "READY_TO_EAT",
      flags: []
    });

    await page.reload();
    await app.openProducts();
    await products.openEdit("Кефир");
    await products.fillForm({
      name: "Кефир 1%",
      nutrition: { calories: 40 },
      composition: "Обновленный состав"
    });
    const responsePromise = page.waitForResponse((response) => response.request().method() === "PUT" && response.url().includes("/api/products/"));
    await products.submit();
    await responsePromise;

    await app.expectBanner("Продукт сохранён.");
    await expect(products.cards).toContainText("Кефир 1%");
    await products.openCard("Кефир 1%");
    await expect(page.getByText("Обновленный состав")).toBeVisible();
    await expect(page.getByText("40 ккал / 100 г")).toBeVisible();
  });

  /**
   * Проверяет, что пустое имя продукта не проходит обязательную HTML-валидацию.
   */
  test("shows validation for empty product name", async ({ page }) => {
    const products = new ProductPage(page);

    await products.openForm();
    await products.nameInput.fill("");
    await products.submit();
    expect(await products.nameInput.evaluate((element) => !element.checkValidity())).toBeTruthy();
  });

  /**
   * Проверяет бизнес-правило: продукт, входящий в состав блюда, нельзя удалить через UI.
   */
  test("prevents deleting a product used in a dish", async ({ page, request }) => {
    const app = new AppPage(page);
    const products = new ProductPage(page);

    const rice = await createProduct(request, {
      name: "Рис",
      photos: [],
      nutrition: { calories: 330, proteins: 7, fats: 0.7, carbs: 78 },
      composition: null,
      category: "GRAINS",
      cookingState: "NEEDS_COOKING",
      flags: []
    });

    await createDish(request, {
      name: "Рисовая тарелка",
      photos: [],
      nutrition: { calories: 0, proteins: 0, fats: 0, carbs: 0 },
      ingredients: [{ productId: rice.id, amountGrams: 150 }],
      portionSize: 200,
      category: "SECOND",
      flags: []
    });

    await page.reload();
    await app.openProducts();
    await products.delete("Рис");

    await app.expectBanner("Нельзя удалить продукт. Он используется в блюдах: Рисовая тарелка");
    await expect(products.cards).toContainText("Рис");
  });
});
