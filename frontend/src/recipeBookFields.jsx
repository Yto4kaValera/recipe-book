import { flagOptions } from "./recipeBookConfig";

export function FieldNutrition({ value, onChange }) {
  const labels = {
    calories: "Калорийность",
    proteins: "Белки",
    fats: "Жиры",
    carbs: "Углеводы"
  };

  return (
    <div className="nutrition-grid">
      {Object.entries(labels).map(([key, label]) => (
        <label key={key}>
          {label}
          <input
            type="number"
            min="0"
            max={key === "calories" ? undefined : 100}
            step="0.01"
            value={value[key]}
            onChange={(event) => onChange({ ...value, [key]: Number(event.target.value) })}
          />
        </label>
      ))}
    </div>
  );
}

export function FlagSelector({ selected, onChange, disabled = [] }) {
  return (
    <div className="flag-row">
      {flagOptions.map((flag) => (
        <label key={flag.value} className={`flag-chip ${disabled.includes(flag.value) ? "disabled" : ""}`}>
          <input
            type="checkbox"
            checked={selected.includes(flag.value)}
            disabled={disabled.includes(flag.value)}
            onChange={(event) => {
              if (event.target.checked) {
                onChange([...selected, flag.value]);
              } else {
                onChange(selected.filter((item) => item !== flag.value));
              }
            }}
          />
          {flag.label}
        </label>
      ))}
    </div>
  );
}

export function PhotoPicker({ photos, onChange, onError }) {
  async function handleFiles(event) {
    const files = Array.from(event.target.files || []);
    if (files.length > 5) {
      onError?.("Можно загрузить не больше 5 фотографий.");
    }
    const limitedFiles = files.slice(0, 5);
    const nextPhotos = [];
    const knownPhotos = new Set(photos);
    let hasDuplicates = false;

    for (const file of limitedFiles) {
      const photo = await new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(String(reader.result));
        reader.onerror = reject;
        reader.readAsDataURL(file);
      });

      if (knownPhotos.has(photo)) {
        hasDuplicates = true;
        continue;
      }

      knownPhotos.add(photo);
      nextPhotos.push(photo);
    }

    if (hasDuplicates) {
      onError?.("Это фото уже добавлено.");
    }

    onChange([...photos, ...nextPhotos].slice(0, 5));
    event.target.value = "";
  }

  return (
    <div className="stack">
      <label>
        Фотографии
        <input type="file" accept="image/*" multiple onChange={handleFiles} />
      </label>
      {photos.length ? (
        <div className="photo-grid">
          {photos.map((photo, index) => (
            <div key={index} className="photo-card">
              <img src={photo} alt="" />
              <button type="button" className="secondary" onClick={() => onChange(photos.filter((_, current) => current !== index))}>
                Удалить фото
              </button>
            </div>
          ))}
        </div>
      ) : null}
    </div>
  );
}

export function DetailSection({ title, children }) {
  return (
    <section className="detail-card">
      <h3>{title}</h3>
      {children}
    </section>
  );
}
