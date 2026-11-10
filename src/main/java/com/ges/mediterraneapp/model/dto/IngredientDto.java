package com.ges.mediterraneapp.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IngredientDto {
    String uuid;

    @Size(max = 255)
    String amount;
    @Size(max = 255)
    String measurement;
    @NotBlank
    @Size(max = 255)
    String nameIngredient;
}
