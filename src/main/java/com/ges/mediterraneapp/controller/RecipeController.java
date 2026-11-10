package com.ges.mediterraneapp.controller;

import com.ges.mediterraneapp.mapper.RecipeMapper;
import com.ges.mediterraneapp.model.dto.RecipeDto;
import com.ges.mediterraneapp.service.RecipeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/recipe")
public class RecipeController {
    private final RecipeService recipeService;
    private final RecipeMapper recipeMapper;

    public RecipeController(RecipeService recipeService, RecipeMapper recipeMapper) {
        this.recipeService = recipeService;
        this.recipeMapper = recipeMapper;
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
        RecipeService.UpsertResult result = recipeService.upsertRecipe(recipeDto);
        return new ResponseEntity<>(recipeMapper.toDto(result.recipe()),
                result.created() ? HttpStatus.CREATED : HttpStatus.OK);
    }

    @PostMapping("/bulk")
    public HttpStatus createRecipes (@RequestBody List<@Valid RecipeDto> recipes) {
        recipeService.upsertRecipes(recipes);
        return HttpStatus.CREATED;
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<RecipeDto> updateRecipe (@PathVariable String uuid, @Valid @RequestBody RecipeDto recipeDto) {
        return new ResponseEntity<>(recipeMapper.toDto(recipeService.updateRecipe(uuid, recipeDto)), HttpStatus.OK);
    }

    @DeleteMapping("/{uuid}")
    public HttpStatus deleteRecipe (@PathVariable String uuid) {
        recipeService.deleteRecipe(uuid);
        return HttpStatus.OK;
    }
}
