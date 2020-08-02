package com.ges.mediterraneapp.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecipeDto {
    String uuid;

    String name;
    String relations;
    String preparedTime;
    String cookedTime;
    String totalTime;

    List<String> steps;

    List<IngredientDto> ingredients;
}
