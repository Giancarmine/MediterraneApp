package com.ges.mediterraneapp.mapper;

import com.ges.mediterraneapp.model.dao.Ingredient;
import com.ges.mediterraneapp.model.dto.IngredientDto;

import java.util.UUID;

public class IngredientMapper {
    IngredientDto toDto (Ingredient ingredient) {
        IngredientDto ingredientDto = new IngredientDto();

        ingredientDto.setUuid(ingredient.getUuid().toString());
        ingredientDto.setNameIngredient(ingredient.getNameIngredient());
        ingredientDto.setAmount(ingredient.getAmount());
        ingredientDto.setMeasurement(ingredient.getMeasurement());
        return ingredientDto;
    }

    public Ingredient updateModel(IngredientDto ingredientDto, Ingredient ingredient) {
        ingredient.setUuid(ingredientDto.getUuid() != null ? UUID.fromString(ingredientDto.getUuid()) : null);
        ingredient.setNameIngredient(ingredientDto.getNameIngredient());
        ingredient.setAmount(ingredientDto.getAmount());
        ingredient.setMeasurement(ingredientDto.getMeasurement());

        return ingredient;
    }
}
