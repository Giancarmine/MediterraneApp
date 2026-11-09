package com.ges.mediterraneapp.model.dao.repository;

import com.ges.mediterraneapp.model.dao.Recipe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RecipeRepositoryTest {

    @Autowired
    private RecipeRepository recipeRepository;

    @Test
    void findsSavedRecipeByUuidAndName() {
        Recipe recipe = new Recipe();
        recipe.setName("Tortilla de patatas");

        Recipe saved = recipeRepository.saveAndFlush(recipe);

        assertThat(recipeRepository.findByUuid(saved.getUuid()))
                .isPresent()
                .get()
                .extracting(Recipe::getName)
                .isEqualTo("Tortilla de patatas");
        assertThat(recipeRepository.findByName("Tortilla de patatas"))
                .isPresent()
                .get()
                .extracting(Recipe::getUuid)
                .isEqualTo(saved.getUuid());
    }

    @Test
    void returnsEmptyOptionalWhenNothingMatches() {
        assertThat(recipeRepository.findByUuid(UUID.randomUUID())).isEmpty();
        assertThat(recipeRepository.findByName("does not exist")).isEmpty();
    }
}
