package com.ges.mediterraneapp.mapper;

import com.ges.mediterraneapp.model.dao.Ingredient;
import com.ges.mediterraneapp.model.dao.Recipe;
import com.ges.mediterraneapp.model.dto.IngredientDto;
import com.ges.mediterraneapp.model.dto.RecipeDto;
import com.ges.mediterraneapp.service.IngredientService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.toSet;

public class RecipeMapper {
    @Autowired
    private final IngredientService ingredientService;

    private final IngredientMapper ingredientMapper;

    public RecipeMapper(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
        this.ingredientMapper = new IngredientMapper();
    }

    public RecipeDto toDto(Recipe recipe) {
        RecipeDto recipeDto = new RecipeDto();

        recipeDto.setUuid(recipe.getUuid().toString());
        recipeDto.setName(recipe.getName());
        recipeDto.setRelations(recipe.getRelations());
        recipeDto.setPreparedTime(recipe.getPreparedTime());
        recipeDto.setCookedTime(recipe.getCookedTime());
        recipeDto.setTotalTime(recipe.getTotalTime());

        Set<String> steps = recipe.getSteps();
        if (steps != null) {
            recipeDto.setSteps(steps);
        }

        Set<Ingredient> ingredients = recipe.getIngredients();
        if (ingredients != null) {
            recipeDto.setIngredients(ingredients.stream()
                    .map(ingredientMapper::toDto)
                    .collect(toSet()));
        }

        return recipeDto;
    }

    public Recipe updateModel(RecipeDto recipeDto, Recipe recipe) {
        recipe.setUuid(recipeDto.getUuid() != null ? UUID.fromString(recipeDto.getUuid()) : null);
        recipe.setName(recipeDto.getName());
        recipe.setRelations(recipeDto.getRelations());
        recipe.setPreparedTime(recipeDto.getPreparedTime());
        recipe.setCookedTime(recipeDto.getCookedTime());
        recipe.setTotalTime(recipeDto.getTotalTime());

        Set<String> steps = recipeDto.getSteps();
        if (steps != null) {
            recipe.setSteps(steps);
        }

        Set<IngredientDto> ingredients = recipeDto.getIngredients();
        if (ingredients != null) {
            recipe.setIngredients(ingredients.stream()
                    .map(ingredient -> ingredientService.findIngredientByNameAndAmountAndMeasurement(ingredient.getNameIngredient(),
                            ingredient.getAmount(),
                            ingredient.getMeasurement()))
                    .collect(toSet()));
        }
        return recipe;
    }
}
