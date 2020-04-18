package com.ges.mediterraneapp.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecipeDto {
    String uuid;

    String title;
}
