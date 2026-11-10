package com.ges.mediterraneapp.service;

import com.ges.mediterraneapp.mapper.RecipeMapper;
import com.ges.mediterraneapp.model.dao.Ingredient;
import com.ges.mediterraneapp.model.dao.Recipe;
import com.ges.mediterraneapp.model.dao.repository.IngredientRepository;
import com.ges.mediterraneapp.model.dao.repository.RecipeRepository;
import com.ges.mediterraneapp.model.dto.RecipeDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class RecipeService {
    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final RecipeMapper recipeMapper;

    public RecipeService(RecipeRepository recipeRepository,
                         IngredientRepository ingredientRepository,
                         RecipeMapper recipeMapper) {
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.recipeMapper = recipeMapper;
    }

    public List<Recipe> getAllRecipe () {
        return recipeRepository.findAll();
    }

    public Recipe findRecipeByUuid (String uuid) {
        return recipeRepository.findByUuid(UUID.fromString(uuid))
                .orElseThrow(() -> new NoSuchElementException("Recipe not found: " + uuid));
    };

    @Transactional
    public UpsertResult upsertRecipe (RecipeDto recipeDto) {
        return doUpsert(recipeDto);
    }

    /**
     * Upserts every recipe in a single transaction. Validation runs before this
     * method, so an invalid payload never reaches the loop; a persistence failure
     * rolls back the whole batch.
     */
    @Transactional
    public List<Recipe> upsertRecipes (List<RecipeDto> recipeDtos) {
        return recipeDtos.stream()
                .map(recipeDto -> doUpsert(recipeDto).recipe())
                .toList();
    }

    @Transactional
    public Recipe updateRecipe (String uuid, RecipeDto recipeDto) {
        Recipe recipe = findRecipeByUuid(uuid);

        if (recipeDto.getUuid() != null && !UUID.fromString(recipeDto.getUuid()).equals(recipe.getUuid())) {
            throw new IllegalArgumentException("Body uuid must match the path uuid");
        }

        return recipeRepository.save(recipeMapper.updateModel(recipeDto, recipe));
    }

    @Transactional
    public void deleteRecipe (String uuid) {
        Recipe recipe = findRecipeByUuid(uuid);
        Set<Ingredient> ingredients = recipe.getIngredients() == null
                ? Set.of()
                : new HashSet<>(recipe.getIngredients());

        recipeRepository.delete(recipe);
        recipeRepository.flush();

        ingredients.stream()
                .filter(ingredient -> ingredient.getUuid() != null)
                .filter(ingredient -> !recipeRepository.existsByIngredients_Uuid(ingredient.getUuid()))
                .forEach(ingredientRepository::delete);
    }

    private UpsertResult doUpsert (RecipeDto recipeDto) {
        Recipe recipe = recipeRepository.findByName(recipeDto.getName())
                .orElseGet(Recipe::new);
        boolean created = recipe.getUuid() == null;

        if (created && recipeDto.getUuid() != null) {
            recipe.setUuid(UUID.fromString(recipeDto.getUuid()));
        }

        Recipe saved = recipeRepository.save(recipeMapper.updateModel(recipeDto, recipe));
        return new UpsertResult(saved, created);
    }

    public record UpsertResult(Recipe recipe, boolean created) {
    }
}
