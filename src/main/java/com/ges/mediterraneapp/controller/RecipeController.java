package com.ges.mediterraneapp.controller;

import com.ges.mediterraneapp.model.dto.RecipeDto;
import com.ges.mediterraneapp.service.RecipeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/recipe")
public class RecipeController {
    @Autowired
    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping("/status")
    public String status() {
        return "I`m alive";
    }

    @GetMapping
    public List<RecipeDto> getAllRecipes () {
        return recipeService.getAllRecipe();
    }

    @GetMapping("/{uuid}")
    public RecipeDto read(@PathVariable String uuid) {
        return recipeService.findRecipeByUuid(uuid);
    }

    @PostMapping
    public RecipeDto createRecipe (@Valid @RequestBody RecipeDto recipe) {
        return recipeService.createRecipe(recipe);
    }
}