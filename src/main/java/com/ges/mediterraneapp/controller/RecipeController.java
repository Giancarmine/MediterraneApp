package com.ges.mediterraneapp.controller;

import com.ges.mediterraneapp.mapper.RecipeMapper;
import com.ges.mediterraneapp.model.dao.Recipe;
import com.ges.mediterraneapp.model.dto.RecipeDto;
import com.ges.mediterraneapp.service.IngredientService;
import com.ges.mediterraneapp.service.RecipeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/recipe")
public class RecipeController {
    private final RecipeService recipeService;
    private final IngredientService ingredientService;

    private final RecipeMapper recipeMapper;

    public RecipeController(RecipeService recipeService, IngredientService ingredientService) {
        this.recipeService = recipeService;
        this.ingredientService = ingredientService;

        this.recipeMapper = new RecipeMapper(this.ingredientService);
    }

    @GetMapping("/status")
    public HttpStatus status() {
        return HttpStatus.I_AM_A_TEAPOT;
    }

    @GetMapping
    public ResponseEntity<List<RecipeDto>> getAllRecipes () {
        return new ResponseEntity<>(recipeService.getAllRecipe().stream()
                .map(recipeMapper::toDto)
                .collect(Collectors.toList()), HttpStatus.OK);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<RecipeDto> read(@PathVariable String uuid) {
        return new ResponseEntity<>(recipeMapper.toDto(recipeService.findRecipeByUuid(uuid)), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<RecipeDto> createRecipe (@Valid @RequestBody RecipeDto recipeDto) {
        Recipe recipe = recipeService.findRecipeByName(recipeDto.getName());
        boolean exists = recipe.getUuid() != null;

        if (!exists && recipeDto.getUuid() != null) {
            recipe.setUuid(UUID.fromString(recipeDto.getUuid()));
        }

        Recipe saved = recipeService.createRecipe(recipeMapper.updateModel(recipeDto, recipe));
        return new ResponseEntity<>(recipeMapper.toDto(saved), exists ? HttpStatus.OK : HttpStatus.CREATED);
    }

    @PostMapping("/bulk")
    public HttpStatus createRecipes (@RequestBody List<@Valid RecipeDto> recipes) {
        recipes.forEach(this::createRecipe);
        return HttpStatus.CREATED;
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<RecipeDto> updateRecipe (@PathVariable String uuid, @Valid @RequestBody RecipeDto recipeDto) {
        Recipe recipe = recipeService.findRecipeByUuid(uuid);

        if (recipeDto.getUuid() != null && !UUID.fromString(recipeDto.getUuid()).equals(recipe.getUuid())) {
            throw new IllegalArgumentException("Body uuid must match the path uuid");
        }

        return new ResponseEntity<>(recipeMapper.toDto(recipeService.updateRecipe(recipeMapper.updateModel(recipeDto, recipe))), HttpStatus.OK);
    }

    @DeleteMapping("/{uuid}")
    public HttpStatus deleteRecipe (@PathVariable String uuid) {
        recipeService.deleteRecipe(uuid);
        return HttpStatus.OK;
    }
}
