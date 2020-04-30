package com.ges.mediterraneapp.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IngredientDto {
    String uuid;

    String text;
}
