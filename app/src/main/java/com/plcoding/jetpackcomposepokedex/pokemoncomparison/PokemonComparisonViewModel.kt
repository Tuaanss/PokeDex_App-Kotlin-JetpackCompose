package com.plcoding.pokedex.pokemoncomparison

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plcoding.pokedex.data.models.PokedexListEntry
import com.plcoding.pokedex.data.remote.responses.Pokemon
import com.plcoding.pokedex.repository.PokemonRepository
import com.plcoding.pokedex.util.Constants
import com.plcoding.pokedex.util.Constants.PAGE_SIZE
import com.plcoding.pokedex.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class PokemonComparisonViewModel @Inject constructor(
    private val repository: PokemonRepository
) : ViewModel() {

    var pokemon1Name = mutableStateOf<String?>(null)
    var pokemon2Name = mutableStateOf<String?>(null)
    var pokemon1 = mutableStateOf<Resource<Pokemon>>(Resource.Loading())
    var pokemon2 = mutableStateOf<Resource<Pokemon>>(Resource.Loading())
    
    // For Pokemon selection dialog
    var allPokemonList = mutableStateOf<List<PokedexListEntry>>(listOf())
    var isLoadingPokemonList = mutableStateOf(false)
    private var hasLoadedAllPokemon = false

    fun setPokemon1(name: String) {
        pokemon1Name.value = name
        loadPokemon1(name)
    }

    fun setPokemon2(name: String) {
        pokemon2Name.value = name
        loadPokemon2(name)
    }

    fun clearPokemon1() {
        pokemon1Name.value = null
        pokemon1.value = Resource.Loading()
    }

    fun clearPokemon2() {
        pokemon2Name.value = null
        pokemon2.value = Resource.Loading()
    }

    fun clearAll() {
        clearPokemon1()
        clearPokemon2()
    }

    private fun loadPokemon1(name: String) {
        viewModelScope.launch {
            pokemon1.value = Resource.Loading()
            pokemon1.value = repository.getPokemonInfo(name.lowercase())
        }
    }

    private fun loadPokemon2(name: String) {
        viewModelScope.launch {
            pokemon2.value = Resource.Loading()
            pokemon2.value = repository.getPokemonInfo(name.lowercase())
        }
    }

    fun loadPokemons(name1: String?, name2: String?) {
        name1?.let { setPokemon1(it) }
        name2?.let { setPokemon2(it) }
    }
    
    fun loadAllPokemonList() {
        if (hasLoadedAllPokemon && allPokemonList.value.isNotEmpty()) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            isLoadingPokemonList.value = true
            try {
                // First, get the total count
                val countResult = repository.getPokemonList(1, 0)
                val totalCount = when(countResult) {
                    is Resource.Success -> countResult.data!!.count
                    else -> 10000 // Fallback to large number if can't get count
                }
                
                // Load all Pokemon in batches (API may limit per request)
                val batchSize = Constants.BATCH_SIZE_FOR_SEARCH
                val allEntries = mutableListOf<PokedexListEntry>()
                var offset = 0
                
                while (offset < totalCount) {
                    val limit = minOf(batchSize, totalCount - offset)
                    val result = repository.getPokemonList(limit, offset)
                    
                    when(result) {
                        is Resource.Success -> {
                            val pokedexEntries = result.data!!.results.mapIndexed { index, entry ->
                                val number = if(entry.url.endsWith("/")) {
                                    entry.url.dropLast(1).takeLastWhile { it.isDigit() }
                                } else {
                                    entry.url.takeLastWhile { it.isDigit() }
                                }
                                val url = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${number}.png"
                                PokedexListEntry(entry.name.capitalize(Locale.ROOT), url, number.toInt())
                            }
                            allEntries.addAll(pokedexEntries)
                            offset += limit
                            
                            // Update list progressively
                            allPokemonList.value = allEntries.toList()
                        }
                        else -> {
                            // If batch fails, break and use what we have
                            break
                        }
                    }
                }
                
                hasLoadedAllPokemon = true
            } catch (e: Exception) {
                // Error handling
            } finally {
                isLoadingPokemonList.value = false
            }
        }
    }
}

