import { expect } from "@playwright/test";

export class AppPage {
  constructor(page) {
    this.page = page;
    this.productsTab = page.getByRole("button", { name: "Продукты" });
    this.dishesTab = page.getByRole("button", { name: "Блюда" });
    this.banner = page.getByTestId("app-banner");
    this.title = page.getByRole("heading", { name: "Продукты и блюда" });
  }

  async open() {
    await this.page.goto("/");
    await expect(this.title).toBeVisible();
  }

  async openProducts() {
    await this.open();
    await this.productsTab.click();
  }

  async openDishes() {
    await this.open();
    await this.dishesTab.click();
  }

  async expectBanner(text) {
    await expect(this.banner).toContainText(text);
  }
}
