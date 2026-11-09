package com.ges.mediterraneapp.mapper;

import com.ges.mediterraneapp.model.dao.Ingredient;
import com.ges.mediterraneapp.model.dto.IngredientDto;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IngredientMapperTest {

    private final IngredientMapper mapper = new IngredientMapper();

    @Test
    void toDtoMapsEveryField() {
        UUID uuid = UUID.randomUUID();
        Ingredient ingredient = new Ingredient("Flour", "250", "g");
        ingredient.setUuid(uuid);

        IngredientDto dto = mapper.toDto(ingredient);

        assertThat(dto.getUuid()).isEqualTo(uuid.toString());
        assertThat(dto.getNameIngredient()).isEqualTo("Flour");
        assertThat(dto.getAmount()).isEqualTo("250");
        assertThat(dto.getMeasurement()).isEqualTo("g");
    }

    @Test
    void updateModelParsesUuidAndCopiesFields() {
        UUID uuid = UUID.randomUUID();
        IngredientDto dto = new IngredientDto();
        dto.setUuid(uuid.toString());
        dto.setNameIngredient("Flour");
        dto.setAmount("250");
        dto.setMeasurement("g");

        Ingredient ingredient = mapper.updateModel(dto, new Ingredient());

        assertThat(ingredient.getUuid()).isEqualTo(uuid);
        assertThat(ingredient.getNameIngredient()).isEqualTo("Flour");
        assertThat(ingredient.getAmount()).isEqualTo("250");
        assertThat(ingredient.getMeasurement()).isEqualTo("g");
    }

    @Test
    void updateModelLeavesUuidNullWhenDtoHasNoUuid() {
        IngredientDto dto = new IngredientDto();
        dto.setNameIngredient("Flour");

        Ingredient ingredient = mapper.updateModel(dto, new Ingredient());

        assertThat(ingredient.getUuid()).isNull();
    }
}
