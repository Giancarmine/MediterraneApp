package com.ges.mediterraneapp.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecipeDto {
    String uuid;

    @NotBlank
    @Size(max = 255)
    String name;
    String relations;
    String preparedTime;
    String cookedTime;
    String totalTime;

    Set<@Size(max = 2500) String> steps;

    @Valid
    Set<IngredientDto> ingredients;
}
