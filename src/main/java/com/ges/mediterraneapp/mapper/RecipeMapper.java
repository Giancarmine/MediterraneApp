package com.ges.mediterraneapp.mapper;

import com.ges.mediterraneapp.model.dao.Ingredient;
import com.ges.mediterraneapp.model.dao.Recipe;
import com.ges.mediterraneapp.model.dto.RecipeDto;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.Set;
import java.util.UUID;

@Mapper(imports = {UUID.class, Set.class, Ingredient.class},
        uses = IngredientMapper.class,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RecipeMapper {
    RecipeMapper INSTANCE = Mappers.getMapper(RecipeMapper.class);

    @Mappings({
            @Mapping(target = "uuid", expression = "java(recipe.getUuid().toString())"),
            @Mapping(source = "ingredients", target = "ingredients")
    })
    RecipeDto toDto(Recipe recipe);

    @Mappings({
            @Mapping(target = "uuid", expression = "java(UUID.fromString(recipeDto.getUuid()))"),
            @Mapping(source = "ingredients", target = "ingredients")
    })
    Recipe toModel(RecipeDto recipeDto);

    @Mappings({
            @Mapping(target = "uuid", expression = "java((recipeDto.getUuid() != null) ? UUID.fromString(recipeDto.getUuid()) : null)"),
            @Mapping(source = "ingredients", target = "ingredients"),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true)
    })
    void updateModel(RecipeDto recipeDto, @MappingTarget Recipe recipe);
}
