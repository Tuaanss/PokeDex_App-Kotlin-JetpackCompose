package com.plcoding.pokedex.mypokemon

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.plcoding.pokedex.data.local.FavoritePokemonRepository
import com.plcoding.pokedex.data.models.PokedexListEntry
import com.plcoding.pokedex.repository.PokemonRepository
import com.plcoding.pokedex.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyPokemonViewModel @Inject constructor(
    private val favoriteRepository: FavoritePokemonRepository,
    private val pokemonRepository: PokemonRepository
) : ViewModel() {

    private val _favoritePokemonList = MutableStateFlow<List<PokedexListEntry>>(emptyList())
    val favoritePokemonList: StateFlow<List<PokedexListEntry>> = _favoritePokemonList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadFavoritePokemon()
    }

    private fun loadFavoritePokemon() {
        viewModelScope.launch {
            favoriteRepository.favoritePokemonNames.collect { favoriteNames ->
                _isLoading.value = true
                try {
                    if (favoriteNames.isNotEmpty()) {
                        val pokemonEntries = mutableListOf<PokedexListEntry>()
                        favoriteNames.forEach { pokemonName ->
                            try {
                                val pokemonInfo = pokemonRepository.getPokemonInfo(pokemonName)
                                if (pokemonInfo is Resource.Success) {
                                    val pokemon = pokemonInfo.data!!
                                    val types = pokemon.types.map { it.type.name }
                                    val entry = PokedexListEntry(
                                        pokemonName = pokemon.name,
                                        imageUrl = pokemon.sprites.frontDefault ?: "",
                                        number = pokemon.id,
                                        types = types
                                    )
                                    pokemonEntries.add(entry)
                                }
                            } catch (e: Exception) {
                                // Skip if Pokemon not found
                            }
                        }
                        _favoritePokemonList.value = pokemonEntries.sortedBy { it.number }
                    } else {
                        _favoritePokemonList.value = emptyList()
                    }
                } catch (e: Exception) {
                    _favoritePokemonList.value = emptyList()
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    fun calcDominantColor(drawable: Drawable, onFinish: (Color) -> Unit) {
        val bitmap = (drawable as? BitmapDrawable)?.bitmap
            ?: return

        Palette.from(bitmap).generate { palette ->
            palette?.dominantSwatch?.rgb?.let { colorValue ->
                onFinish(Color(colorValue))
            }
        }
    }
}

