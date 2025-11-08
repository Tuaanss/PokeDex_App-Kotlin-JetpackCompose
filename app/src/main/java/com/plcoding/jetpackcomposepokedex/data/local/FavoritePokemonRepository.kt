package com.plcoding.pokedex.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.favoritePokemonDataStore: DataStore<Preferences> by preferencesDataStore(name = "favorite_pokemon")

@Singleton
class FavoritePokemonRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore: DataStore<Preferences> = context.favoritePokemonDataStore
    private val favoritePokemonKey = stringSetPreferencesKey("favorite_pokemon_names")

    val favoritePokemonNames: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[favoritePokemonKey] ?: emptySet()
    }

    suspend fun addFavorite(pokemonName: String) {
        dataStore.edit { preferences ->
            val currentFavorites = preferences[favoritePokemonKey] ?: emptySet()
            preferences[favoritePokemonKey] = currentFavorites + pokemonName.lowercase()
        }
    }

    suspend fun removeFavorite(pokemonName: String) {
        dataStore.edit { preferences ->
            val currentFavorites = preferences[favoritePokemonKey] ?: emptySet()
            preferences[favoritePokemonKey] = currentFavorites - pokemonName.lowercase()
        }
    }
}

