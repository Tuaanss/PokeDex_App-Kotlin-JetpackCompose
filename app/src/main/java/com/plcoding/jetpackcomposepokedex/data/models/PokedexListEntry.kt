package com.plcoding.pokedex.data.models

data class PokedexListEntry(
    val pokemonName: String,
    val imageUrl: String,
    val number: Int,
    val types: List<String> = emptyList()
)
