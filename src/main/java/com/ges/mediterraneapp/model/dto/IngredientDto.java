package com.ges.mediterraneapp.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IngredientDto {
    String uuid;

    String amount;
    String measurement;
    @NotBlank
    String nameIngredient;
}
