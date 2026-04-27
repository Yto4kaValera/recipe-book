import { useEffect, useMemo, useState } from "react";
import DishPanel from "./DishPanel";
import ProductPanel from "./ProductPanel";
import { emptyDish, emptyProduct, flagOptions } from "./recipeBookConfig";
import { appendListParams, request } from "./recipeBookUtils";
import { validateDishForm, validateProductForm } from "./recipeBookValidation";

export default function RecipeBookApp() {
  const [activePage, setActivePage] = useState("products");
  const [products, setProducts] = useState([]);
  const [dishes, setDishes] = useState([]);
  const [productForm, setProductForm] = useState(emptyProduct);
  const [dishForm, setDishForm] = useState(emptyDish);
  const [editingProductId, setEditingProductId] = useState(null);
  const [editingDishId, setEditingDishId] = useState(null);
  const [isProductFormOpen, setIsProductFormOpen] = useState(false);
  const [isDishFormOpen, setIsDishFormOpen] = useState(false);
  const [selectedProductId, setSelectedProductId] = useState(null);
  const [selectedDishId, setSelectedDishId] = useState(null);
  const [message, setMessage] = useState("");
  const [productFilters, setProductFilters] = useState({
    search: "",
    category: "",
    cookingState: "",
    sortBy: "name",
    flags: []
  });
  const [dishFilters, setDishFilters] = useState({
    search: "",
    category: "",
    flags: []
  });

  const selectedProduct = products.find((product) => product.id === selectedProductId) || null;
  const selectedDish = dishes.find((dish) => dish.id === selectedDishId) || null;

  const availableDishFlags = useMemo(() => {
    const chosenProducts = dishForm.ingredients
      .map((ingredient) => products.find((product) => product.id === ingredient.productId))
      .filter(Boolean);

    if (!chosenProducts.length) {
      return [];
    }

    return flagOptions
      .map((flag) => flag.value)
      .filter((flag) => chosenProducts.every((product) => product.flags.includes(flag)));
  }, [dishForm.ingredients, products]);

  const blockedDishFlags = flagOptions
    .map((flag) => flag.value)
    .filter((flag) => !availableDishFlags.includes(flag));

  async function loadProducts() {
    const params = new URLSearchParams();
    if (productFilters.search) {
      params.set("search", productFilters.search);
    }
    if (productFilters.category) {
      params.set("category", productFilters.category);
    }
    if (productFilters.cookingState) {
      params.set("cookingState", productFilters.cookingState);
    }
    params.set("sortBy", productFilters.sortBy);
    appendListParams(params, "flags", productFilters.flags);
    setProducts(await request(`/products?${params.toString()}`));
  }

  async function loadDishes() {
    const params = new URLSearchParams();
    if (dishFilters.search) {
      params.set("search", dishFilters.search);
    }
    if (dishFilters.category) {
      params.set("category", dishFilters.category);
    }
    appendListParams(params, "flags", dishFilters.flags);
    setDishes(await request(`/dishes?${params.toString()}`));
  }

  useEffect(() => {
    loadProducts().catch((error) => setMessage(error.message));
  }, [productFilters]);

  useEffect(() => {
    loadDishes().catch((error) => setMessage(error.message));
  }, [dishFilters]);

  useEffect(() => {
    setDishForm((current) => ({
      ...current,
      flags: current.flags.filter((flag) => availableDishFlags.includes(flag))
    }));
  }, [availableDishFlags]);

  useEffect(() => {
    const validIngredients = dishForm.ingredients.filter((ingredient) => ingredient.productId && ingredient.amountGrams > 0);
    if (!validIngredients.length) {
      return;
    }

    const timer = setTimeout(async () => {
      try {
        const nutrition = await request("/dishes/calculate", {
          method: "POST",
          body: JSON.stringify({ ingredients: validIngredients })
        });
        setDishForm((current) => ({ ...current, nutrition }));
      } catch {
      }
    }, 250);

    return () => clearTimeout(timer);
  }, [dishForm.ingredients]);

  async function saveProduct(event) {
    event.preventDefault();
    try {
      const validationError = validateProductForm(productForm);
      if (validationError) {
        setMessage(validationError);
        return;
      }

      const payload = { ...productForm, composition: productForm.composition || null };
      if (editingProductId) {
        await request(`/products/${editingProductId}`, { method: "PUT", body: JSON.stringify(payload) });
      } else {
        const created = await request("/products", { method: "POST", body: JSON.stringify(payload) });
        setSelectedProductId(created.id);
      }
      setProductForm(emptyProduct);
      setEditingProductId(null);
      setIsProductFormOpen(false);
      setMessage("Продукт сохранён.");
      await loadProducts();
    } catch (error) {
      setMessage(error.message);
    }
  }

  async function saveDish(event) {
    event.preventDefault();
    try {
      const validationError = validateDishForm(dishForm, products, availableDishFlags);
      if (validationError) {
        setMessage(validationError);
        return;
      }

      const payload = { ...dishForm, category: dishForm.category || null };
      if (editingDishId) {
        await request(`/dishes/${editingDishId}`, { method: "PUT", body: JSON.stringify(payload) });
      } else {
        const created = await request("/dishes", { method: "POST", body: JSON.stringify(payload) });
        setSelectedDishId(created.id);
      }
      setDishForm(emptyDish);
      setEditingDishId(null);
      setIsDishFormOpen(false);
      setMessage("Блюдо сохранено.");
      await loadDishes();
    } catch (error) {
      setMessage(error.message);
    }
  }

  async function deleteProduct(product) {
    try {
      await request(`/products/${product.id}`, { method: "DELETE" });
      if (selectedProductId === product.id) {
        setSelectedProductId(null);
      }
      setMessage("Продукт удалён.");
      await loadProducts();
      await loadDishes();
    } catch (error) {
      setMessage(error.message);
    }
  }

  async function deleteDish(dish) {
    try {
      await request(`/dishes/${dish.id}`, { method: "DELETE" });
      if (selectedDishId === dish.id) {
        setSelectedDishId(null);
      }
      setMessage("Блюдо удалено.");
      await loadDishes();
    } catch (error) {
      setMessage(error.message);
    }
  }

  function productNameById(productId) {
    return products.find((product) => product.id === productId)?.name || "Неизвестный продукт";
  }

  function openNewProductForm() {
    setProductForm(emptyProduct);
    setEditingProductId(null);
    setIsProductFormOpen(true);
    setMessage("");
  }

  function openEditProductForm(product) {
    setProductForm({ ...product, composition: product.composition || "" });
    setEditingProductId(product.id);
    setIsProductFormOpen(true);
    setMessage("");
  }

  function openNewDishForm() {
    setDishForm(emptyDish);
    setEditingDishId(null);
    setIsDishFormOpen(true);
    setMessage("");
  }

  function openEditDishForm(dish) {
    setDishForm({ ...dish, category: dish.category || "" });
    setEditingDishId(dish.id);
    setIsDishFormOpen(true);
    setMessage("");
  }

  return (
    <div className="page">
      <header className="hero">
        <div className="hero-copy">
          <p className="eyebrow">Книга рецептов</p>
          <h1>Продукты и блюда</h1>
          <div className="page-switch">
            <button
              type="button"
              className={activePage === "products" ? "" : "secondary"}
              onClick={() => setActivePage("products")}
            >
              Продукты
            </button>
            <button
              type="button"
              className={activePage === "dishes" ? "" : "secondary"}
              onClick={() => setActivePage("dishes")}
            >
              Блюда
            </button>
          </div>
        </div>
        {message ? <div className="banner" data-testid="app-banner">{message}</div> : null}
      </header>

      <main className="single-layout">
        {activePage === "products" ? (
          <ProductPanel
            products={products}
            selectedProduct={selectedProduct}
            selectedProductId={selectedProductId}
            setSelectedProductId={setSelectedProductId}
            productForm={productForm}
            setProductForm={setProductForm}
            editingProductId={editingProductId}
            productFilters={productFilters}
            setProductFilters={setProductFilters}
            setMessage={setMessage}
            saveProduct={saveProduct}
            deleteProduct={deleteProduct}
            isFormOpen={isProductFormOpen}
            setIsFormOpen={setIsProductFormOpen}
            openNewForm={openNewProductForm}
            openEditForm={openEditProductForm}
          />
        ) : (
          <DishPanel
            products={products}
            dishes={dishes}
            selectedDish={selectedDish}
            selectedDishId={selectedDishId}
            setSelectedDishId={setSelectedDishId}
            dishForm={dishForm}
            setDishForm={setDishForm}
            editingDishId={editingDishId}
            dishFilters={dishFilters}
            setDishFilters={setDishFilters}
            setMessage={setMessage}
            blockedDishFlags={blockedDishFlags}
            saveDish={saveDish}
            deleteDish={deleteDish}
            productNameById={productNameById}
            isFormOpen={isDishFormOpen}
            setIsFormOpen={setIsDishFormOpen}
            openNewForm={openNewDishForm}
            openEditForm={openEditDishForm}
          />
        )}
      </main>
    </div>
  );
}
