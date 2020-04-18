package com.ges.mediterraneapp.service;

import com.ges.mediterraneapp.mapper.RecipeMapper;
import com.ges.mediterraneapp.model.dao.Recipe;
import com.ges.mediterraneapp.model.dao.repository.RecipeRepository;
import com.ges.mediterraneapp.model.dto.RecipeDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class RecipeService {
    private final RecipeRepository recipeRepository;
    private final RecipeMapper mapper;

    public RecipeService(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;

        this.mapper = new RecipeMapper();
    }


    public List<RecipeDto> getAllRecipe () {
        return mapper.toDto(recipeRepository.findAll());
    }

    public RecipeDto findRecipeByUuid (String uuid) {
        Recipe recipe = recipeRepository.findByUuid(UUID.fromString(uuid));
        return mapper.toDto(recipe);
    };

    public RecipeDto createRecipe (RecipeDto recipeDto) {
        Recipe recipe = recipeRepository.findByTitle(recipeDto.getTitle())
                .orElse(new Recipe());
        return mapper.toDto(recipeRepository.save(mapper.toDao(recipeDto, recipe)));
    }
}
