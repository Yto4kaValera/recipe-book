import { API_URL } from "./recipeBookConfig";

export async function request(path, options = {}) {
  let response;

  try {
    response = await fetch(`${API_URL}${path}`, {
      headers: { "Content-Type": "application/json" },
      ...options
    });
  } catch {
    throw new Error("Не удалось подключиться к серверу. Проверьте, что backend запущен и доступен.");
  }

  if (response.status === 204) {
    return null;
  }

  const data = await response.json().catch(() => null);
  if (!response.ok) {
    throw new Error(data?.message || "Не удалось выполнить запрос.");
  }

  return data;
}

export function appendListParams(params, key, values) {
  values.forEach((value) => params.append(key, value));
}

export function formatDate(value) {
  if (!value) {
    return "—";
  }
  return new Date(value).toLocaleString("ru-RU");
}
