package com.ges.mediterraneapp.controller;

import com.ges.mediterraneapp.model.dao.Recipe;
import com.ges.mediterraneapp.model.dto.RecipeDto;
import com.ges.mediterraneapp.service.RecipeService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/recipe")
public class RecipeController {
    @Autowired
    private final RecipeService recipeService;
    private static final ModelMapper modelMapper = new ModelMapper();

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping("/status")
    public String status() {
        return "I`m alive";
    }

    @GetMapping
    public List<RecipeDto> getAllRecipes () {
        return recipeService.getAllRecipe().stream()
                .map(element -> modelMapper.map(element, RecipeDto.class))
                .collect(Collectors.toList());
    }

    @GetMapping("/{uuid}")
    public RecipeDto read(@PathVariable String uuid) {
        return modelMapper.map(recipeService.findRecipeByUuid(uuid), RecipeDto.class);
    }

    @PostMapping
    public RecipeDto createRecipe (@Valid @RequestBody RecipeDto recipe) {
        return modelMapper.map(recipeService.createRecipe(modelMapper.map(recipe, Recipe.class)), RecipeDto.class);
    }


    @PostMapping("/bulk")
    public List<RecipeDto> createRecipes (@Valid @RequestBody List<RecipeDto> recipes) {
        return recipes.stream()
                .map(recipe -> modelMapper.map(recipeService.createRecipe(modelMapper.map(recipe, Recipe.class)), RecipeDto.class))
                .collect(Collectors.toList());
    }

    @PutMapping("/{uuid}")
    public RecipeDto updateRecipe (@PathVariable String uuid, @Valid @RequestBody RecipeDto recipeDto) {
        Recipe recipe = recipeService.findRecipeByUuid(uuid);
        modelMapper.map(recipeDto, recipe);

        return modelMapper.map(recipeService.updateRecipe(recipe), RecipeDto.class);
    }

    @DeleteMapping("/{uuid}")
    public HttpStatus deleteRecipe (@PathVariable String uuid) {
        recipeService.deleteRecipe(uuid);
        return HttpStatus.OK;
    }
}