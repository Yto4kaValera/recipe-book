﻿import { cookingStateOptions, labelByValue, productCategoryOptions, sortOptions } from "./recipeBookConfig";
import { DetailSection, FieldNutrition, FlagSelector, PhotoPicker } from "./recipeBookFields";
import { formatDate } from "./recipeBookUtils";

export default function ProductPanel({
    products,
    selectedProduct,
    selectedProductId,
    setSelectedProductId,
    productForm,
    setProductForm,
    editingProductId,
    productFilters,
    setProductFilters,
    setMessage,
    saveProduct,
    deleteProduct,
    isFormOpen,
    setIsFormOpen,
    openNewForm,
    openEditForm
}) {
    return (
        <section className="panel">
            {selectedProductId && selectedProduct ? (
                <>
                    <div className="section-head">
                        <h2>Просмотр продукта</h2>
                        <button type="button" className="secondary section-action" onClick={() => setSelectedProductId(null)}>
                            Назад к списку
                        </button>
                    </div>
                    <DetailSection title={selectedProduct.name}>
                        <p><strong>Категория:</strong> {labelByValue[selectedProduct.category]}</p>
                        <p><strong>Необходимость готовки:</strong> {labelByValue[selectedProduct.cookingState]}</p>
                        <p><strong>Состав:</strong> {selectedProduct.composition || "Не указан"}</p>
                        <p><strong>Флаги:</strong> {selectedProduct.flags.length ? selectedProduct.flags.map((flag) => labelByValue[flag]).join(", ") : "Нет"}</p>
                        <p><strong>Создан:</strong> {formatDate(selectedProduct.createdAt)}</p>
                        {selectedProduct.updatedAt ? <p><strong>Изменён:</strong> {formatDate(selectedProduct.updatedAt)}</p> : null}
                        <div className="stats">
                            <span>{selectedProduct.nutrition.calories} ккал / 100 г</span>
                            <span>Белки {selectedProduct.nutrition.proteins}</span>
                            <span>Жиры {selectedProduct.nutrition.fats}</span>
                            <span>Углеводы {selectedProduct.nutrition.carbs}</span>
                        </div>
                        <div className="actions">
                            <button type="button" onClick={() => openEditForm(selectedProduct)}>
                                Редактировать
                            </button>
                            <button type="button" className="secondary" onClick={() => deleteProduct(selectedProduct)}>
                                Удалить
                            </button>
                        </div>
                        {selectedProduct.photos.length ? (
                            <div className="photo-grid">
                                {selectedProduct.photos.map((photo, index) => <img key={index} src={photo} alt="" className="detail-photo" />)}
                            </div>
                        ) : null}
                    </DetailSection>
                </>
            ) : (
                <>
                    <div className="section-head">
                        <h2>Продукты</h2>
                        <button type="button" className="secondary section-action" onClick={openNewForm}>
                            {isFormOpen ? "Форма открыта" : "Создать продукт"}
                        </button>
                    </div>

                    <div className="filters">
                        <button type="button" onClick={openNewForm}>Новый продукт</button>
                        <input
                            placeholder="Поиск по названию"
                            value={productFilters.search}
                            onChange={(event) => setProductFilters({ ...productFilters, search: event.target.value })}
                        />
                        <select value={productFilters.category} onChange={(event) => setProductFilters({ ...productFilters, category: event.target.value })}>
                            <option value="">Все категории</option>
                            {productCategoryOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
                        </select>
                        <select value={productFilters.cookingState} onChange={(event) => setProductFilters({ ...productFilters, cookingState: event.target.value })}>
                            <option value="">Любая готовность</option>
                            {cookingStateOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
                        </select>
                        <select value={productFilters.sortBy} onChange={(event) => setProductFilters({ ...productFilters, sortBy: event.target.value })}>
                            {sortOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
                        </select>
                        <FlagSelector selected={productFilters.flags} onChange={(nextFlags) => setProductFilters({ ...productFilters, flags: nextFlags })} />
                    </div>

                    {isFormOpen ? (
                        <form className="stack form-card" onSubmit={saveProduct}>
                            <div className="section-head">
                                <h3>{editingProductId ? "Редактирование продукта" : "Создание продукта"}</h3>
                                <button type="button" className="secondary section-action" onClick={() => setIsFormOpen(false)}>Закрыть</button>
                            </div>
                            <label>
                                Название
                                <input minLength={2} required value={productForm.name} onChange={(event) => setProductForm({ ...productForm, name: event.target.value })} />
                            </label>
                            <PhotoPicker
                                photos={productForm.photos}
                                onChange={(photos) => setProductForm({ ...productForm, photos })}
                                onError={setMessage}
                            />
                            <FieldNutrition value={productForm.nutrition} onChange={(nutrition) => setProductForm({ ...productForm, nutrition })} />
                            <label>
                                Состав
                                <textarea value={productForm.composition} onChange={(event) => setProductForm({ ...productForm, composition: event.target.value })} />
                            </label>
                            <div className="two-cols">
                                <label>
                                    Категория
                                    <select value={productForm.category} onChange={(event) => setProductForm({ ...productForm, category: event.target.value })}>
                                        {productCategoryOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
                                    </select>
                                </label>
                                <label>
                                    Необходимость готовки
                                    <select value={productForm.cookingState} onChange={(event) => setProductForm({ ...productForm, cookingState: event.target.value })}>
                                        {cookingStateOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
                                    </select>
                                </label>
                            </div>
                            <FlagSelector selected={productForm.flags} onChange={(nextFlags) => setProductForm({ ...productForm, flags: nextFlags })} />
                            <button type="submit">{editingProductId ? "Сохранить изменения" : "Создать продукт"}</button>
                        </form>
                    ) : null}

                    <div className="cards">
                        {products.map((product) => (
                            <article key={product.id} className="card">
                                <div className="card-head">
                                    <div>
                                        <h3>{product.name}</h3>
                                        <p>{labelByValue[product.category]} · {labelByValue[product.cookingState]}</p>
                                    </div>
                                    <button type="button" className="secondary" onClick={() => setSelectedProductId(product.id)}>Открыть</button>
                                </div>
                                <div className="stats">
                                    <span>{product.nutrition.calories} ккал</span>
                                    <span>Б {product.nutrition.proteins}</span>
                                    <span>Ж {product.nutrition.fats}</span>
                                    <span>У {product.nutrition.carbs}</span>
                                </div>
                                <div className="chips">
                                    {product.flags.map((flag) => <span key={flag} className="chip">{labelByValue[flag]}</span>)}
                                </div>
                                <div className="actions">
                                    <button type="button" onClick={() => openEditForm(product)}>
                                        Редактировать
                                    </button>
                                    <button type="button" className="secondary" onClick={() => deleteProduct(product)}>Удалить</button>
                                </div>
                            </article>
                        ))}
                    </div>
                </>
            )}
        </section>
    );
}