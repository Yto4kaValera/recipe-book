import { expect } from "@playwright/test";

export class ProductPage {
  constructor(page) {
    this.page = page;
    this.createButton = page.getByRole("button", { name: "Создать продукт" });
    this.form = page.getByTestId("product-form");
    this.cards = page.getByTestId("product-cards");
    this.searchInput = page.getByTestId("product-search");
    this.categoryFilter = page.getByTestId("product-category-filter");
    this.cookingFilter = page.getByTestId("product-cooking-filter");
    this.sortFilter = page.getByTestId("product-sort-filter");
  }

  get nameInput() { return this.form.getByTestId("product-name"); }
  get caloriesInput() { return this.form.getByTestId("product-nutrition-calories"); }
  get proteinsInput() { return this.form.getByTestId("product-nutrition-proteins"); }
  get fatsInput() { return this.form.getByTestId("product-nutrition-fats"); }
  get carbsInput() { return this.form.getByTestId("product-nutrition-carbs"); }
  get compositionInput() { return this.form.getByTestId("product-composition"); }
  get categorySelect() { return this.form.getByTestId("product-category"); }
  get cookingStateSelect() { return this.form.getByTestId("product-cooking-state"); }
  get submitButton() { return this.form.getByRole("button", { name: /Создать продукт|Сохранить изменения/ }); }
  get closeButton() { return this.form.getByRole("button", { name: "Закрыть" }); }
  get photoInput() { return this.form.locator("input[type='file']"); }
  deletePhotoButtons() { return this.form.getByRole("button", { name: "Удалить фото" }); }

  flagCheckbox(flag) {
    return this.form.getByTestId(`product-flag-${flag}`);
  }

  filterFlag(flag) {
    return this.page.getByTestId(`product-filter-flag-${flag}`);
  }

  card(name) {
    return this.page.locator("[data-testid^='product-card-']").filter({ hasText: name });
  }

  async openForm() {
    await this.createButton.click();
    await expect(this.form).toBeVisible();
  }

  async fillForm(data) {
    if (data.name !== undefined) await this.nameInput.fill(data.name);
    if (data.nutrition?.calories !== undefined) await this.caloriesInput.fill(String(data.nutrition.calories));
    if (data.nutrition?.proteins !== undefined) await this.proteinsInput.fill(String(data.nutrition.proteins));
    if (data.nutrition?.fats !== undefined) await this.fatsInput.fill(String(data.nutrition.fats));
    if (data.nutrition?.carbs !== undefined) await this.carbsInput.fill(String(data.nutrition.carbs));
    if (data.composition !== undefined) await this.compositionInput.fill(data.composition);
    if (data.category) await this.categorySelect.selectOption(data.category);
    if (data.cookingState) await this.cookingStateSelect.selectOption(data.cookingState);
    for (const flag of data.flags ?? []) {
      await this.flagCheckbox(flag).check();
    }
  }

  async submit() {
    await this.submitButton.click();
  }

  async attachPhotos(files) {
    await this.photoInput.setInputFiles(files);
  }

  async openCard(name) {
    await this.card(name).getByRole("button", { name: "Открыть" }).click();
    await expect(this.page.getByRole("heading", { name: "Просмотр продукта" })).toBeVisible();
  }

  async openEdit(name) {
    await this.card(name).getByRole("button", { name: "Редактировать" }).click();
    await expect(this.form).toBeVisible();
  }

  async delete(name) {
    await this.card(name).getByRole("button", { name: "Удалить" }).click();
  }

  async filter(data) {
    if (data.search !== undefined) await this.searchInput.fill(data.search);
    if (data.category !== undefined) await this.categoryFilter.selectOption(data.category);
    if (data.cookingState !== undefined) await this.cookingFilter.selectOption(data.cookingState);
    if (data.sortBy !== undefined) await this.sortFilter.selectOption(data.sortBy);
    for (const [flag, checked] of Object.entries(data.flags ?? {})) {
      if (checked) {
        await this.filterFlag(flag).check();
      } else {
        await this.filterFlag(flag).uncheck();
      }
    }
  }

  async cardTitles() {
    return this.page.locator("[data-testid^='product-card-'] h3").allTextContents();
  }
}
