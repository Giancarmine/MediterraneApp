package com.ges.mediterraneapp.service;

import com.ges.mediterraneapp.model.dao.Recipe;
import com.ges.mediterraneapp.model.dao.repository.RecipeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class RecipeService {
    private final RecipeRepository recipeRepository;

    public RecipeService(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    public List<Recipe> getAllRecipe () {
        return recipeRepository.findAll();
    }

    public Recipe findRecipeByUuid (String uuid) {
        return recipeRepository.findByUuid(UUID.fromString(uuid));
    };

    public Recipe createRecipe (Recipe recipe) {
        return recipeRepository.save(recipe);
    }
}
