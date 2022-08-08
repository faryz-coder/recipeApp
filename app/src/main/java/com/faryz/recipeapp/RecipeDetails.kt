package com.faryz.recipeapp

import kotlinx.serialization.Serializable

@Serializable
data class RecipeDetails(
    val name: String,
    val type: String,
    val ingredients: String,
    val steps: String,
    val imgUri: String
)

data class RecipeList(
    val name: String
)