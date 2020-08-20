package com.ges.mediterraneapp.model.dao.repository;

import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StepRepository<String, UUID> extends Repository {
    Optional<String> findByStep (String step);
}
