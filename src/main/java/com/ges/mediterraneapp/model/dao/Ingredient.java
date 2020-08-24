package com.ges.mediterraneapp.model.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ingredient")
@EntityListeners(AuditingEntityListener.class)
public class Ingredient {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "uuid", updatable = false, nullable = false)
    private UUID uuid;

    private String amount;
    private String measurement;
    private String nameIngredient;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Date createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Date updatedAt;

    public Ingredient(String nameIngredient, String amount, String measurement) {
        this.setNameIngredient(nameIngredient);
        this.setAmount(amount);
        this.setMeasurement(measurement);
    }
}
