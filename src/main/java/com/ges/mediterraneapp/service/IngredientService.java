package com.ges.mediterraneapp.service;

import com.ges.mediterraneapp.model.dao.Ingredient;
import com.ges.mediterraneapp.model.dao.repository.IngredientRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class IngredientService {
    private final IngredientRepository ingredientRepository;

    public IngredientService(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
    }

    public List<Ingredient> getAllIngredient () {
        return ingredientRepository.findAll();
    }

    public Ingredient findIngredientByUuid (String uuid) {
        return ingredientRepository.findByUuid(UUID.fromString(uuid))
                .orElseThrow();
    };

    public Ingredient findIngredientByNameAndAmountAndMeasurement (String name, String amount, String measurement) {
        return ingredientRepository.findByNameIngredientAndAmountAndMeasurement(name, amount, measurement)
                .orElse(new Ingredient(name, amount, measurement));
    };

    public Ingredient createIngredient (Ingredient ingredient) {
        return ingredientRepository.save(ingredient);
    }

    public Ingredient updateIngredient (Ingredient ingredient) {
        return ingredientRepository.save(ingredient);
    }

    public void deleteIngredient (String uuid) {
        ingredientRepository.delete(findIngredientByUuid(uuid));
    }
}
