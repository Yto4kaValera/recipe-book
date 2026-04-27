import { expect } from "@playwright/test";

export class DishPage {
  constructor(page) {
    this.page = page;
    this.createButton = page.getByRole("button", { name: "Создать блюдо" });
    this.form = page.getByTestId("dish-form");
    this.cards = page.getByTestId("dish-cards");
  }

  get nameInput() { return this.form.getByTestId("dish-name"); }
  get portionSizeInput() { return this.form.getByTestId("dish-portion-size"); }
  get categorySelect() { return this.form.getByTestId("dish-category"); }
  get addIngredientButton() { return this.form.getByTestId("dish-add-ingredient"); }
  get submitButton() { return this.form.getByRole("button", { name: /Создать блюдо|Сохранить изменения/ }); }

  nutritionInput(field) {
    return this.form.getByTestId(`dish-nutrition-${field}`);
  }

  ingredientProduct(index) {
    return this.form.getByTestId(`dish-ingredient-product-${index}`);
  }

  ingredientAmount(index) {
    return this.form.getByTestId(`dish-ingredient-amount-${index}`);
  }

  flagCheckbox(flag) {
    return this.form.getByTestId(`dish-flag-${flag}`);
  }

  card(name) {
    return this.page.locator("[data-testid^='dish-card-']").filter({ hasText: name });
  }

  async openForm() {
    await this.createButton.click();
    await expect(this.form).toBeVisible();
  }

  async fillForm(data) {
    if (data.name !== undefined) await this.nameInput.fill(data.name);
    if (data.portionSize !== undefined) await this.portionSizeInput.fill(String(data.portionSize));
    if (data.category !== undefined) await this.categorySelect.selectOption(data.category);

    for (let index = 0; index < (data.ingredients?.length ?? 0); index += 1) {
      if (index > 0) await this.addIngredientButton.click();
      const ingredient = data.ingredients[index];
      if (ingredient.productName !== undefined) {
        await this.ingredientProduct(index).selectOption({ label: ingredient.productName });
      }
      if (ingredient.amountGrams !== undefined) {
        await this.ingredientAmount(index).fill(String(ingredient.amountGrams));
      }
    }

    if (data.nutrition) {
      if (data.nutrition.calories !== undefined) await this.nutritionInput("calories").fill(String(data.nutrition.calories));
      if (data.nutrition.proteins !== undefined) await this.nutritionInput("proteins").fill(String(data.nutrition.proteins));
      if (data.nutrition.fats !== undefined) await this.nutritionInput("fats").fill(String(data.nutrition.fats));
      if (data.nutrition.carbs !== undefined) await this.nutritionInput("carbs").fill(String(data.nutrition.carbs));
    }

    for (const flag of data.flags ?? []) {
      await this.flagCheckbox(flag).check();
    }
  }

  async submit() {
    await this.submitButton.click();
  }

  async openCard(name) {
    await this.card(name).getByRole("button", { name: "Открыть" }).click();
    await expect(this.page.getByRole("heading", { name: "Просмотр блюда" })).toBeVisible();
  }

  async openEdit(name) {
    await this.card(name).getByRole("button", { name: "Редактировать" }).click();
    await expect(this.form).toBeVisible();
  }

  async delete(name) {
    await this.card(name).getByRole("button", { name: "Удалить" }).click();
  }
}
