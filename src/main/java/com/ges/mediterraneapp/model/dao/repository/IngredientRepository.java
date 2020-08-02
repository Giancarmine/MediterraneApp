package com.ges.mediterraneapp.model.dao.repository;

import com.ges.mediterraneapp.model.dao.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, UUID> {
    Ingredient findByUuid (UUID uuid);
    Optional<Ingredient> findByNameIngredient (String nameIngredient);
}
