package com.recipebook.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DishDraftRequest(@Valid @NotEmpty List<DishIngredient> ingredients) {
}
