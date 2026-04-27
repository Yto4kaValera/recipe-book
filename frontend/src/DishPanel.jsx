import { dishCategoryOptions, labelByValue } from "./recipeBookConfig";
import { DetailSection, FieldNutrition, FlagSelector, PhotoPicker } from "./recipeBookFields";
import { formatDate } from "./recipeBookUtils";

export default function DishPanel({
  products,
  dishes,
  selectedDish,
  selectedDishId,
  setSelectedDishId,
  dishForm,
  setDishForm,
  editingDishId,
  dishFilters,
  setDishFilters,
  setMessage,
  blockedDishFlags,
  saveDish,
  deleteDish,
  productNameById,
  isFormOpen,
  setIsFormOpen,
  openNewForm,
  openEditForm
}) {
  return (
    <section className="panel">
      {selectedDishId && selectedDish ? (
        <>
          <div className="section-head">
            <h2>Просмотр блюда</h2>
            <button type="button" className="secondary section-action" onClick={() => setSelectedDishId(null)}>
              Назад к списку
            </button>
          </div>
          <DetailSection title={selectedDish.name}>
            <p><strong>Категория:</strong> {labelByValue[selectedDish.category]}</p>
            <p><strong>Размер порции:</strong> {selectedDish.portionSize} г</p>
            <p><strong>Флаги:</strong> {selectedDish.flags.length ? selectedDish.flags.map((flag) => labelByValue[flag]).join(", ") : "Нет"}</p>
            <p><strong>Создано:</strong> {formatDate(selectedDish.createdAt)}</p>
            {selectedDish.updatedAt ? <p><strong>Изменено:</strong> {formatDate(selectedDish.updatedAt)}</p> : null}
            <div className="stats">
              <span>{selectedDish.nutrition.calories} ккал / порция</span>
              <span>Белки {selectedDish.nutrition.proteins}</span>
              <span>Жиры {selectedDish.nutrition.fats}</span>
              <span>Углеводы {selectedDish.nutrition.carbs}</span>
            </div>
            <div className="stack">
              <strong>Состав</strong>
              {selectedDish.ingredients.map((ingredient, index) => (
                <div key={index} className="ingredient-preview">
                  <span>{productNameById(ingredient.productId)}</span>
                  <span>{ingredient.amountGrams} г</span>
                </div>
              ))}
            </div>
            <div className="actions">
              <button type="button" onClick={() => openEditForm(selectedDish)}>
                Редактировать
              </button>
              <button type="button" className="secondary" onClick={() => deleteDish(selectedDish)}>
                Удалить
              </button>
            </div>
            {selectedDish.photos.length ? (
              <div className="photo-grid">
                {selectedDish.photos.map((photo, index) => <img key={index} src={photo} alt="" className="detail-photo" />)}
              </div>
            ) : null}
          </DetailSection>
        </>
      ) : (
        <>
      <div className="section-head">
        <h2>Блюда</h2>
        <button type="button" className="secondary section-action" onClick={openNewForm}>
          {isFormOpen ? "Форма открыта" : "Создать блюдо"}
        </button>
      </div>

      <div className="filters" data-testid="dish-filters">
        <button type="button" onClick={openNewForm}>Новое блюдо</button>
        <input
          data-testid="dish-search"
          placeholder="Поиск по названию"
          value={dishFilters.search}
          onChange={(event) => setDishFilters({ ...dishFilters, search: event.target.value })}
        />
        <select data-testid="dish-category-filter" value={dishFilters.category} onChange={(event) => setDishFilters({ ...dishFilters, category: event.target.value })}>
          <option value="">Все категории</option>
          {dishCategoryOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
        </select>
        <FlagSelector
          testIdPrefix="dish-filter-flag"
          selected={dishFilters.flags}
          onChange={(nextFlags) => setDishFilters({ ...dishFilters, flags: nextFlags })}
        />
      </div>

      {isFormOpen ? (
        <form className="stack form-card" data-testid="dish-form" onSubmit={saveDish}>
          <div className="section-head">
            <h3>{editingDishId ? "Редактирование блюда" : "Создание блюда"}</h3>
            <button type="button" className="secondary section-action" onClick={() => setIsFormOpen(false)}>Закрыть</button>
          </div>
          <label>
            Название
            <input data-testid="dish-name" minLength={2} required value={dishForm.name} onChange={(event) => setDishForm({ ...dishForm, name: event.target.value })} />
          </label>
          <PhotoPicker
            photos={dishForm.photos}
            onChange={(photos) => setDishForm({ ...dishForm, photos })}
            onError={setMessage}
          />
          <label>
            Размер порции, г
            <input
              data-testid="dish-portion-size"
              type="number"
              min="0.01"
              step="0.01"
              required
              value={dishForm.portionSize}
              onChange={(event) => setDishForm({ ...dishForm, portionSize: Number(event.target.value) })}
            />
          </label>
          <label>
            Категория
            <select data-testid="dish-category" value={dishForm.category || ""} onChange={(event) => setDishForm({ ...dishForm, category: event.target.value })}>
              <option value="">Определить по макросу или выбрать вручную</option>
              {dishCategoryOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
            </select>
          </label>
          <div className="stack">
            <strong>Состав блюда</strong>
            {dishForm.ingredients.map((ingredient, index) => (
              <div key={index} className="ingredient-row" data-testid={`dish-ingredient-row-${index}`}>
                <select
                  data-testid={`dish-ingredient-product-${index}`}
                  value={ingredient.productId}
                  onChange={(event) => {
                    const next = [...dishForm.ingredients];
                    next[index] = { ...next[index], productId: event.target.value };
                    setDishForm({ ...dishForm, ingredients: next });
                  }}
                >
                  <option value="">Выберите продукт</option>
                  {products.map((product) => <option key={product.id} value={product.id}>{product.name}</option>)}
                </select>
                <input
                  data-testid={`dish-ingredient-amount-${index}`}
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={ingredient.amountGrams}
                  onChange={(event) => {
                    const next = [...dishForm.ingredients];
                    next[index] = { ...next[index], amountGrams: Number(event.target.value) };
                    setDishForm({ ...dishForm, ingredients: next });
                  }}
                />
                <button
                  type="button"
                  className="secondary"
                  data-testid={`dish-ingredient-remove-${index}`}
                  onClick={() => {
                    const nextIngredients = dishForm.ingredients.filter((_, current) => current !== index);
                    setDishForm({
                      ...dishForm,
                      ingredients: nextIngredients.length ? nextIngredients : [{ productId: "", amountGrams: 100 }]
                    });
                  }}
                >
                  Удалить
                </button>
              </div>
            ))}
            <button
              type="button"
              className="secondary"
              data-testid="dish-add-ingredient"
              onClick={() => setDishForm({
                ...dishForm,
                ingredients: [...dishForm.ingredients, { productId: "", amountGrams: 100 }]
              })}
            >
              Добавить продукт
            </button>
          </div>
          <FieldNutrition
            testIdPrefix="dish-nutrition"
            value={dishForm.nutrition}
            limitMacrosToHundred={false}
            onChange={(nutrition) => setDishForm({ ...dishForm, nutrition })}
          />
          <p className="hint">КБЖУ пересчитывается автоматически по составу. После этого значения можно вручную исправить.</p>
          <FlagSelector
            testIdPrefix="dish-flag"
            selected={dishForm.flags}
            disabled={blockedDishFlags}
            onChange={(nextFlags) => setDishForm({ ...dishForm, flags: nextFlags })}
          />
          <button type="submit" disabled={!products.length}>{editingDishId ? "Сохранить изменения" : "Создать блюдо"}</button>
        </form>
      ) : null}

      {!products.length ? <p className="hint">Сначала создайте хотя бы один продукт, затем можно будет добавить блюдо.</p> : null}

      <div className="cards" data-testid="dish-cards">
        {dishes.map((dish) => (
          <article key={dish.id} className="card" data-testid={`dish-card-${dish.id}`}>
            <div className="card-head">
              <div>
                <h3>{dish.name}</h3>
                <p>{labelByValue[dish.category]} · {dish.portionSize} г</p>
              </div>
              <button type="button" className="secondary" onClick={() => setSelectedDishId(dish.id)}>Открыть</button>
            </div>
            <div className="stats">
              <span>{dish.nutrition.calories} ккал</span>
              <span>Б {dish.nutrition.proteins}</span>
              <span>Ж {dish.nutrition.fats}</span>
              <span>У {dish.nutrition.carbs}</span>
            </div>
            <div className="chips">
              {dish.flags.map((flag) => <span key={flag} className="chip">{labelByValue[flag]}</span>)}
            </div>
            <div className="actions">
              <button type="button" onClick={() => openEditForm(dish)}>
                Редактировать
              </button>
              <button type="button" className="secondary" onClick={() => deleteDish(dish)}>Удалить</button>
            </div>
          </article>
        ))}
      </div>
        </>
      )}
    </section>
  );
}
