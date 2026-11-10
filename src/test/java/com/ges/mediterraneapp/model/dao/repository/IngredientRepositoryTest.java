package com.ges.mediterraneapp.model.dao.repository;

import com.ges.mediterraneapp.model.dao.Ingredient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class IngredientRepositoryTest {

    @Autowired
    private IngredientRepository ingredientRepository;

    @Test
    void rejectsDuplicateIngredientIdentity() {
        ingredientRepository.saveAndFlush(new Ingredient("Salt", "1", "g"));

        assertThatThrownBy(() ->
                ingredientRepository.saveAndFlush(new Ingredient("Salt", "1", "g")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
