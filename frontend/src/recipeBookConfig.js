export const API_URL = import.meta.env.VITE_API_URL || "/api";

export const productCategoryOptions = [
  { value: "FROZEN", label: "Замороженный" },
  { value: "MEAT", label: "Мясной" },
  { value: "VEGETABLES", label: "Овощи" },
  { value: "GREENS", label: "Зелень" },
  { value: "SPICES", label: "Специи" },
  { value: "GRAINS", label: "Крупы" },
  { value: "CANNED", label: "Консервы" },
  { value: "LIQUID", label: "Жидкость" },
  { value: "SWEETS", label: "Сладости" }
];

export const cookingStateOptions = [
  { value: "READY_TO_EAT", label: "Готовый к употреблению" },
  { value: "SEMI_FINISHED", label: "Полуфабрикат" },
  { value: "NEEDS_COOKING", label: "Требует приготовления" }
];

export const dishCategoryOptions = [
  { value: "DESSERT", label: "Десерт" },
  { value: "FIRST", label: "Первое" },
  { value: "SECOND", label: "Второе" },
  { value: "DRINK", label: "Напиток" },
  { value: "SALAD", label: "Салат" },
  { value: "SOUP", label: "Суп" },
  { value: "SNACK", label: "Перекус" }
];

export const flagOptions = [
  { value: "VEGAN", label: "Веган" },
  { value: "GLUTEN_FREE", label: "Без глютена" },
  { value: "SUGAR_FREE", label: "Без сахара" }
];

export const sortOptions = [
  { value: "name", label: "Сортировка: название" },
  { value: "calories", label: "Сортировка: калорийность" },
  { value: "proteins", label: "Сортировка: белки" },
  { value: "fats", label: "Сортировка: жиры" },
  { value: "carbs", label: "Сортировка: углеводы" }
];

export const labelByValue = Object.fromEntries(
  [...productCategoryOptions, ...cookingStateOptions, ...dishCategoryOptions, ...flagOptions].map((item) => [item.value, item.label])
);

export const emptyProduct = {
  name: "",
  photos: [],
  nutrition: { calories: 0, proteins: 0, fats: 0, carbs: 0 },
  composition: "",
  category: "VEGETABLES",
  cookingState: "READY_TO_EAT",
  flags: []
};

export const emptyDish = {
  name: "",
  photos: [],
  nutrition: { calories: 0, proteins: 0, fats: 0, carbs: 0 },
  ingredients: [{ productId: "", amountGrams: 100 }],
  portionSize: 100,
  category: "",
  flags: []
};
