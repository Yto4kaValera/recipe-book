import { expect } from "@playwright/test";

const apiBaseUrl = "http://127.0.0.1:8081/api";

export async function resetData(request) {
  const dishes = await getJson(request, "/dishes");
  for (const dish of dishes) {
    await request.delete(`${apiBaseUrl}/dishes/${dish.id}`);
  }

  const products = await getJson(request, "/products");
  for (const product of products) {
    await request.delete(`${apiBaseUrl}/products/${product.id}`);
  }
}

export async function createProduct(request, product) {
  const response = await request.post(`${apiBaseUrl}/products`, { data: product });
  expect(response.ok(), "Product should be created through API fixture").toBeTruthy();
  return response.json();
}

export async function createDish(request, dish) {
  const response = await request.post(`${apiBaseUrl}/dishes`, { data: dish });
  expect(response.ok(), "Dish should be created through API fixture").toBeTruthy();
  return response.json();
}

async function getJson(request, path) {
  const response = await request.get(`${apiBaseUrl}${path}`);
  expect(response.ok(), `GET ${path} should succeed`).toBeTruthy();
  return response.json();
}
