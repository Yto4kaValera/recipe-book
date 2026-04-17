const dishMacros = [
  "!десерт",
  "!первое",
  "!второе",
  "!напиток",
  "!салат",
  "!суп",
  "!перекус"
];

function validateNutrition(nutrition, unitLabel, limitMacroSumToHundred = true) {
  if (!nutrition) {
    return `КБЖУ ${unitLabel} обязательно.`;
  }

  const fields = [
    ["calories", "Калорийность", false],
    ["proteins", "Белки", true],
    ["fats", "Жиры", true],
    ["carbs", "Углеводы", true]
  ];

  for (const [key, label, maxHundred] of fields) {
    const value = Number(nutrition[key]);
    if (Number.isNaN(value)) {
      return `Поле "${label}" должно быть числом.`;
    }
    if (value < 0) {
      return `Поле "${label}" не может быть меньше 0.`;
    }
    if (maxHundred && limitMacroSumToHundred && value > 100) {
      return `Поле "${label}" не может быть больше 100.`;
    }
  }

  if (limitMacroSumToHundred && Number(nutrition.proteins) + Number(nutrition.fats) + Number(nutrition.carbs) > 100) {
    return "Сумма БЖУ на 100 грамм не может превышать 100.";
  }

  return null;
}

export function validateProductForm(productForm) {
  if (!productForm.name?.trim()) {
    return "Название продукта обязательно.";
  }
  if (productForm.name.trim().length < 2) {
    return "Название продукта должно содержать минимум 2 символа.";
  }
  if ((productForm.photos?.length || 0) > 5) {
    return "У продукта не может быть больше 5 фотографий.";
  }

  const nutritionError = validateNutrition(productForm.nutrition, "продукта");
  if (nutritionError) {
    return nutritionError;
  }

  if (!productForm.category) {
    return "Категория продукта обязательна.";
  }
  if (!productForm.cookingState) {
    return "Необходимость готовки обязательна.";
  }

  return null;
}

export function validateDishForm(dishForm, products, availableDishFlags) {
  if (!dishForm.name?.trim()) {
    return "Название блюда обязательно.";
  }
  if (dishForm.name.trim().length < 2) {
    return "Название блюда должно содержать минимум 2 символа.";
  }
  if ((dishForm.photos?.length || 0) > 5) {
    return "У блюда не может быть больше 5 фотографий.";
  }

  if (!(Number(dishForm.portionSize) > 0)) {
    return "Размер порции должен быть больше 0.";
  }

  if (!Array.isArray(dishForm.ingredients) || !dishForm.ingredients.length) {
    return "Состав блюда должен содержать хотя бы один продукт.";
  }

  for (const ingredient of dishForm.ingredients) {
    if (!ingredient.productId) {
      return "Для каждого ингредиента нужно выбрать продукт.";
    }
    if (!products.some((product) => product.id === ingredient.productId)) {
      return "Один из выбранных продуктов не найден.";
    }
    if (!(Number(ingredient.amountGrams) > 0)) {
      return "Количество каждого продукта должно быть больше 0 грамм.";
    }
  }

  const loweredName = dishForm.name.toLowerCase();
  const hasMacroCategory = dishMacros.some((macro) => loweredName.includes(macro));
  if (!dishForm.category && !hasMacroCategory) {
    return "Категория блюда обязательна: выберите её вручную или укажите макрос в названии.";
  }

  const nutritionError = validateNutrition(dishForm.nutrition, "блюда", false);
  if (nutritionError) {
    return nutritionError;
  }

  for (const flag of dishForm.flags || []) {
    if (!availableDishFlags.includes(flag)) {
      return "Выбранные флаги блюда не соответствуют составу.";
    }
  }

  return null;
}
