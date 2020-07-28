package com.ges.mediterraneapp.mapper;

import com.ges.mediterraneapp.model.dao.Recipe;
import com.ges.mediterraneapp.model.dto.RecipeDto;

import java.util.List;
import java.util.stream.Collectors;

public class RecipeMapper {
    public RecipeDto toDto (Recipe dao) {
        RecipeDto dto = new RecipeDto();
        dto.setUuid(dao.getUuid().toString());
        dto.setTitle(dao.getTitle());

        return dto;
    }

    public List<RecipeDto> toDto (List<Recipe> daos) {
        return daos.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Recipe toDao (RecipeDto dto, Recipe dao) {
        dao.setTitle(dto.getTitle());

        return dao;
    }
}
