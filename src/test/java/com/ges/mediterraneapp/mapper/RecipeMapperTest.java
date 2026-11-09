package com.ges.mediterraneapp.mapper;

import com.ges.mediterraneapp.model.dao.Ingredient;
import com.ges.mediterraneapp.model.dao.Recipe;
import com.ges.mediterraneapp.model.dto.IngredientDto;
import com.ges.mediterraneapp.model.dto.RecipeDto;
import com.ges.mediterraneapp.service.IngredientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RecipeMapperTest {

    private IngredientService ingredientService;
    private RecipeMapper mapper;

    @BeforeEach
    void setUp() {
        ingredientService = mock(IngredientService.class);
        mapper = new RecipeMapper(ingredientService);
    }

    @Test
    void toDtoMapsScalarFieldsStepsAndIngredients() {
        UUID recipeUuid = UUID.randomUUID();
        UUID ingredientUuid = UUID.randomUUID();

        Ingredient ingredient = new Ingredient("Egg", "3", "unit");
        ingredient.setUuid(ingredientUuid);

        Recipe recipe = new Recipe();
        recipe.setUuid(recipeUuid);
        recipe.setName("Tortilla");
        recipe.setRelations("Spanish");
        recipe.setPreparedTime("10m");
        recipe.setCookedTime("20m");
        recipe.setTotalTime("30m");
        recipe.setSteps(new LinkedHashSet<>(List.of("beat", "fry")));
        recipe.setIngredients(new LinkedHashSet<>(List.of(ingredient)));

        RecipeDto dto = mapper.toDto(recipe);

        assertThat(dto.getUuid()).isEqualTo(recipeUuid.toString());
        assertThat(dto.getName()).isEqualTo("Tortilla");
        assertThat(dto.getRelations()).isEqualTo("Spanish");
        assertThat(dto.getPreparedTime()).isEqualTo("10m");
        assertThat(dto.getCookedTime()).isEqualTo("20m");
        assertThat(dto.getTotalTime()).isEqualTo("30m");
        assertThat(dto.getSteps()).containsExactly("beat", "fry");
        assertThat(dto.getIngredients()).hasSize(1);

        IngredientDto ingredientDto = dto.getIngredients().iterator().next();
        assertThat(ingredientDto.getUuid()).isEqualTo(ingredientUuid.toString());
        assertThat(ingredientDto.getNameIngredient()).isEqualTo("Egg");
        assertThat(ingredientDto.getAmount()).isEqualTo("3");
        assertThat(ingredientDto.getMeasurement()).isEqualTo("unit");
    }

    @Test
    void toDtoLeavesNullCollectionsNull() {
        Recipe recipe = new Recipe();
        recipe.setUuid(UUID.randomUUID());
        recipe.setSteps(null);
        recipe.setIngredients(null);

        RecipeDto dto = mapper.toDto(recipe);

        assertThat(dto.getSteps()).isNull();
        assertThat(dto.getIngredients()).isNull();
    }

    @Test
    void updateModelMapsFieldsAndResolvesIngredients() {
        UUID uuid = UUID.randomUUID();
        Ingredient resolved = new Ingredient("Egg", "3", "unit");
        resolved.setUuid(UUID.randomUUID());

        IngredientDto ingredientDto = new IngredientDto();
        ingredientDto.setNameIngredient("Egg");
        ingredientDto.setAmount("3");
        ingredientDto.setMeasurement("unit");

        RecipeDto dto = new RecipeDto();
        dto.setUuid(uuid.toString());
        dto.setName("Tortilla");
        dto.setRelations("Spanish");
        dto.setPreparedTime("10m");
        dto.setCookedTime("20m");
        dto.setTotalTime("30m");
        dto.setSteps(new LinkedHashSet<>(List.of("beat", "fry")));
        dto.setIngredients(Set.of(ingredientDto));

        when(ingredientService.findIngredientByNameAndAmountAndMeasurement("Egg", "3", "unit"))
                .thenReturn(resolved);

        Recipe recipe = new Recipe();
        Recipe result = mapper.updateModel(dto, recipe);

        assertThat(result.getUuid()).isEqualTo(uuid);
        assertThat(result.getName()).isEqualTo("Tortilla");
        assertThat(result.getRelations()).isEqualTo("Spanish");
        assertThat(result.getPreparedTime()).isEqualTo("10m");
        assertThat(result.getCookedTime()).isEqualTo("20m");
        assertThat(result.getTotalTime()).isEqualTo("30m");
        assertThat(result.getSteps()).containsExactly("beat", "fry");
        assertThat(result.getIngredients()).containsExactly(resolved);

        verify(ingredientService).findIngredientByNameAndAmountAndMeasurement("Egg", "3", "unit");
    }

    @Test
    void updateModelLeavesCollectionsUntouchedWhenDtoCollectionsAreNull() {
        Ingredient existing = new Ingredient("Salt", "1", "tsp");
        existing.setUuid(UUID.randomUUID());

        Recipe recipe = new Recipe();
        recipe.setSteps(new LinkedHashSet<>(List.of("keep")));
        recipe.setIngredients(new LinkedHashSet<>(List.of(existing)));

        RecipeDto dto = new RecipeDto();
        dto.setName("New name");

        Recipe result = mapper.updateModel(dto, recipe);

        assertThat(result.getSteps()).containsExactly("keep");
        assertThat(result.getIngredients()).containsExactly(existing);
        verifyNoInteractions(ingredientService);
    }
}
