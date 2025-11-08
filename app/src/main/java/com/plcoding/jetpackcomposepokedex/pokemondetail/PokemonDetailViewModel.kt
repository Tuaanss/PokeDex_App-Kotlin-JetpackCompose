package com.plcoding.pokedex.pokemondetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plcoding.pokedex.data.local.FavoritePokemonRepository
import com.plcoding.pokedex.data.remote.responses.Pokemon
import com.plcoding.pokedex.repository.PokemonRepository
import com.plcoding.pokedex.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PokemonDetailViewModel @Inject constructor(
    private val repository: PokemonRepository,
    private val favoriteRepository: FavoritePokemonRepository
) : ViewModel() {

    private val _currentPokemonName = MutableStateFlow<String?>(null)
    
    val isFavorite: StateFlow<Boolean> = combine(
        _currentPokemonName,
        favoriteRepository.favoritePokemonNames
    ) { pokemonName, favorites ->
        pokemonName?.let { favorites.contains(it.lowercase()) } ?: false
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    suspend fun getPokemonInfo(pokemonName: String): Resource<Pokemon> {
        _currentPokemonName.value = pokemonName
        return repository.getPokemonInfo(pokemonName)
    }

    fun toggleFavorite(pokemonName: String) {
        viewModelScope.launch {
            val currentFavorites = favoriteRepository.favoritePokemonNames.first()
            if (currentFavorites.contains(pokemonName.lowercase())) {
                favoriteRepository.removeFavorite(pokemonName)
            } else {
                favoriteRepository.addFavorite(pokemonName)
            }
        }
    }
}