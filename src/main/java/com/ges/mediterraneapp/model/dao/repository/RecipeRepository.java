package com.ges.mediterraneapp.model.dao.repository;

import com.ges.mediterraneapp.model.dao.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, UUID> {
    Recipe findByUuid (UUID uuid);
    Optional<Recipe> findByTitle (String title);
}
