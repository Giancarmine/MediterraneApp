-- Baseline schema for a fresh database.
--
-- Existing databases created by Hibernate `ddl-auto: update` are baselined at
-- version 1 by spring.flyway.baseline-on-migrate, so this script only runs on an
-- empty schema. Column and constraint names match Hibernate's naming strategy.

create table ingredient (
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    uuid uuid not null,
    amount varchar(255),
    measurement varchar(255),
    name_ingredient varchar(255),
    primary key (uuid),
    constraint uk_ingredient_identity unique (name_ingredient, amount, measurement)
);

create table recipe (
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    uuid uuid not null,
    cooked_time varchar(255),
    name varchar(255),
    prepared_time varchar(255),
    relations varchar(255),
    total_time varchar(255),
    primary key (uuid),
    constraint uk_recipe_name unique (name)
);

create table recipe_ingredients (
    ingredients_uuid uuid not null,
    recipe_uuid uuid not null,
    primary key (ingredients_uuid, recipe_uuid)
);

create table recipe_steps (
    recipe_uuid uuid not null,
    steps varchar(2500),
    unique (recipe_uuid, steps)
);

alter table recipe_ingredients
    add constraint fk_recipe_ingredients_ingredient
    foreign key (ingredients_uuid) references ingredient;

alter table recipe_ingredients
    add constraint fk_recipe_ingredients_recipe
    foreign key (recipe_uuid) references recipe;

alter table recipe_steps
    add constraint fk_recipe_steps_recipe
    foreign key (recipe_uuid) references recipe;
