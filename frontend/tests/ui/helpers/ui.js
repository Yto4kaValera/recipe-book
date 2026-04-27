import { expect } from "@playwright/test";

export async function openProductsPage(page) {
  await page.goto("/");
  await expect(page.getByRole("heading", { name: "Продукты и блюда" })).toBeVisible();
  await page.getByRole("button", { name: "Продукты" }).click();
}

export async function openDishesPage(page) {
  await openProductsPage(page);
  await page.getByRole("button", { name: "Блюда" }).click();
}

export async function openProductForm(page) {
  await page.getByRole("button", { name: "Создать продукт" }).click();
  await expect(page.getByTestId("product-form")).toBeVisible();
}

export async function openProductCard(page, name) {
  const card = page.locator("[data-testid^='product-card-']").filter({ hasText: name });
  await card.getByRole("button", { name: "Открыть" }).click();
  await expect(page.getByRole("heading", { name: "Просмотр продукта" })).toBeVisible();
}

export async function openProductEdit(page, name) {
  const card = page.locator("[data-testid^='product-card-']").filter({ hasText: name });
  await card.getByRole("button", { name: "Редактировать" }).click();
  await expect(page.getByTestId("product-form")).toBeVisible();
}

export async function submitProduct(page, product) {
  const form = page.getByTestId("product-form");
  await form.getByTestId("product-name").fill(product.name);
  await form.getByTestId("product-nutrition-calories").fill(String(product.nutrition.calories));
  await form.getByTestId("product-nutrition-proteins").fill(String(product.nutrition.proteins));
  await form.getByTestId("product-nutrition-fats").fill(String(product.nutrition.fats));
  await form.getByTestId("product-nutrition-carbs").fill(String(product.nutrition.carbs));
  if (product.composition !== undefined) {
    await form.getByTestId("product-composition").fill(product.composition);
  }
  if (product.category) {
    await form.getByTestId("product-category").selectOption(product.category);
  }
  if (product.cookingState) {
    await form.getByTestId("product-cooking-state").selectOption(product.cookingState);
  }
  for (const flag of product.flags ?? []) {
    await form.getByTestId(`product-flag-${flag}`).check();
  }
  await form.getByRole("button", { name: /Создать продукт|Сохранить изменения/ }).click();
}

export async function openDishForm(page) {
  await page.getByRole("button", { name: "Создать блюдо" }).click();
  await expect(page.getByTestId("dish-form")).toBeVisible();
}

export async function openDishCard(page, name) {
  const card = page.locator("[data-testid^='dish-card-']").filter({ hasText: name });
  await card.getByRole("button", { name: "Открыть" }).click();
  await expect(page.getByRole("heading", { name: "Просмотр блюда" })).toBeVisible();
}

export async function openDishEdit(page, name) {
  const card = page.locator("[data-testid^='dish-card-']").filter({ hasText: name });
  await card.getByRole("button", { name: "Редактировать" }).click();
  await expect(page.getByTestId("dish-form")).toBeVisible();
}

export async function submitDish(page, dish) {
  const form = page.getByTestId("dish-form");
  await form.getByTestId("dish-name").fill(dish.name);
  await form.getByTestId("dish-portion-size").fill(String(dish.portionSize));
  if (dish.category !== undefined) {
    await form.getByTestId("dish-category").selectOption(dish.category);
  }

  for (let index = 0; index < dish.ingredients.length; index += 1) {
    if (index > 0) {
      await form.getByTestId("dish-add-ingredient").click();
    }
    await form.getByTestId(`dish-ingredient-product-${index}`).selectOption({ label: dish.ingredients[index].productName });
    await form.getByTestId(`dish-ingredient-amount-${index}`).fill(String(dish.ingredients[index].amountGrams));
  }

  if (dish.nutrition) {
    await form.getByTestId("dish-nutrition-calories").fill(String(dish.nutrition.calories));
    await form.getByTestId("dish-nutrition-proteins").fill(String(dish.nutrition.proteins));
    await form.getByTestId("dish-nutrition-fats").fill(String(dish.nutrition.fats));
    await form.getByTestId("dish-nutrition-carbs").fill(String(dish.nutrition.carbs));
  }

  for (const flag of dish.flags ?? []) {
    await form.getByTestId(`dish-flag-${flag}`).check();
  }

  await form.getByRole("button", { name: /Создать блюдо|Сохранить изменения/ }).click();
}

export async function expectBanner(page, text) {
  await expect(page.getByTestId("app-banner")).toContainText(text);
}

export async function attachPhotos(form, count) {
  const files = Array.from({ length: count }, (_, index) => ({
    name: `photo-${index + 1}.svg`,
    mimeType: "image/svg+xml",
    buffer: Buffer.from(
      `<svg xmlns="http://www.w3.org/2000/svg" width="2" height="2"><rect width="2" height="2" fill="rgb(${index},0,0)"/></svg>`,
      "utf8"
    )
  }));

  await form.locator("input[type='file']").setInputFiles(files);
}
