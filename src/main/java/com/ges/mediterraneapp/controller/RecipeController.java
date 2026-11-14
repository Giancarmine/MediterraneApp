package com.ges.mediterraneapp.controller;

import com.ges.mediterraneapp.mapper.RecipeMapper;
import com.ges.mediterraneapp.model.dto.PageResponse;
import com.ges.mediterraneapp.model.dto.RecipeDto;
import com.ges.mediterraneapp.service.RecipeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        return HttpStatus.valueOf(418);
    }

    @GetMapping
    public ResponseEntity<PageResponse<RecipeDto>> getAllRecipes (
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return new ResponseEntity<>(recipeService.getRecipes(page, size), HttpStatus.OK);
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
