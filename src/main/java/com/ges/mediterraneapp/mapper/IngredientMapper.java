package com.ges.mediterraneapp.mapper;

import com.ges.mediterraneapp.model.dao.Ingredient;
import com.ges.mediterraneapp.model.dto.IngredientDto;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

@Mapper(imports = UUID.class,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface IngredientMapper {
    IngredientMapper INSTANCE = Mappers.getMapper(IngredientMapper.class);

    @Mapping(target = "uuid", expression = "java(ingredient.getUuid().toString())")
    IngredientDto toDto(Ingredient ingredient);

    @Mappings({
        @Mapping(target = "uuid", expression = "java(ingredientDto.getUuid() != null ? UUID.fromString(ingredientDto.getUuid()) : null)"),
        @Mapping(target = "createdAt", ignore = true),
        @Mapping(target = "updatedAt", ignore = true)
    })
    Ingredient toModel(IngredientDto ingredientDto);

    @Mappings({
            @Mapping(target = "uuid", expression = "java(ingredientDto.getUuid() != null ? UUID.fromString(ingredientDto.getUuid()) : null)"),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true)
    })
    void updateModel(IngredientDto ingredientDto, @MappingTarget Ingredient ingredient);
}
