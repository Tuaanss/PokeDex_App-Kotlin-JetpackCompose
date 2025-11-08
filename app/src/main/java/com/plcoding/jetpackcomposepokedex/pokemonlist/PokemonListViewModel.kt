package com.plcoding.pokedex.pokemonlist

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.plcoding.pokedex.data.models.PokedexListEntry
import com.plcoding.pokedex.repository.PokemonRepository
import com.plcoding.pokedex.util.Constants
import com.plcoding.pokedex.util.Constants.PAGE_SIZE
import com.plcoding.pokedex.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class PokemonListViewModel @Inject constructor(
    private val repository: PokemonRepository
) : ViewModel() {

    private var curPage = 0

    var pokemonList = mutableStateOf<List<PokedexListEntry>>(listOf())
    var loadError = mutableStateOf("")
    var isLoading = mutableStateOf(false)
    var endReached = mutableStateOf(false)

    private var cachedPokemonList = listOf<PokedexListEntry>()
    private var isSearchStarting = true
    var isSearching = mutableStateOf(false)
    var selectedType = mutableStateOf<String?>(null)
    private var currentSearchQuery = ""
    
    // Full list for search - loaded in background
    private var allPokemonListForSearch = listOf<PokedexListEntry>()
    private var isLoadingAllForSearch = mutableStateOf(false)
    private var hasLoadedAllForSearch = false

    init {
        loadPokemonPaginated()
        // Load all Pokemon in background for search
        loadAllPokemonForSearch()
    }

    fun searchPokemonList(query: String) {
        currentSearchQuery = query
        viewModelScope.launch(Dispatchers.Default) {
            // Debounce search - wait 300ms after user stops typing
            delay(300)
            
            // Check if query changed during delay
            if (query != currentSearchQuery) {
                return@launch
            }
            
            // Apply filters (which includes search query)
            applyFilters()
        }
    }

    fun filterByType(type: String?) {
        selectedType.value = type
        applyFilters()
    }
    
    private fun applyFilters() {
        viewModelScope.launch(Dispatchers.Default) {
            val type = selectedType.value
            val query = currentSearchQuery
            
            // If no filter and no search, restore cached list
            if (query.isEmpty() && type == null) {
                // If cachedPokemonList is empty but pokemonList has data, sync them
                if (cachedPokemonList.isEmpty() && pokemonList.value.isNotEmpty()) {
                    cachedPokemonList = pokemonList.value
                }
                pokemonList.value = cachedPokemonList
                isSearching.value = false
                isSearchStarting = true
                return@launch
            }
            
            // Sync cachedPokemonList from pokemonList if cached is empty but pokemonList has data
            // This handles the case when app restarts and cachedPokemonList is reset
            if (cachedPokemonList.isEmpty() && pokemonList.value.isNotEmpty() && !isSearching.value) {
                cachedPokemonList = pokemonList.value
            }
            
            // Always use cachedPokemonList as the source for filtering
            // This ensures we filter on the complete list, not a previously filtered list
            val sourceList = cachedPokemonList
            
            // If cachedPokemonList is empty or doesn't have types yet when filtering by type,
            // use current pokemonList as fallback
            val listToFilter = if (sourceList.isEmpty()) {
                pokemonList.value
            } else if (type != null && sourceList.none { it.types.isNotEmpty() }) {
                // If filtering by type but no types loaded yet, use current list
                pokemonList.value
            } else {
                sourceList
            }
            
            val results = listToFilter.filter { entry ->
                val matchesQuery = query.isEmpty() || 
                    entry.pokemonName.contains(query.trim(), ignoreCase = true) ||
                    entry.number.toString() == query.trim()
                val matchesType = type == null || 
                    (entry.types.isNotEmpty() && entry.types.any { it.equals(type, ignoreCase = true) })
                matchesQuery && matchesType
            }
            
            pokemonList.value = results
            isSearching.value = true
        }
    }

    fun loadPokemonPaginated() {
        viewModelScope.launch {
            isLoading.value = true
            val offset = curPage * PAGE_SIZE
            val result = repository.getPokemonList(PAGE_SIZE, offset)
            when(result) {
                is Resource.Success -> {
                    val totalCount = result.data!!.count
                    val currentOffset = offset
                    val nextOffset = currentOffset + result.data.results.size
                    
                    // Check if we've reached the end
                    endReached.value = nextOffset >= totalCount || result.data.results.isEmpty()
                    
                    val pokedexEntries = result.data.results.mapIndexed { index, entry ->
                        val number = if(entry.url.endsWith("/")) {
                            entry.url.dropLast(1).takeLastWhile { it.isDigit() }
                        } else {
                            entry.url.takeLastWhile { it.isDigit() }
                        }
                        val url = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${number}.png"
                        PokedexListEntry(entry.name.capitalize(Locale.ROOT), url, number.toInt())
                    }
                    
                    // Add entries first without types for faster display
                    curPage++
                    loadError.value = ""
                    isLoading.value = false
                    
                    // Always add to cached list first (this is the source of truth)
                    cachedPokemonList = cachedPokemonList + pokedexEntries
                    
                    // Update display list based on current filter state
                    if(!isSearching.value && selectedType.value == null) {
                        // No filter active, show all cached Pokemon
                        pokemonList.value = cachedPokemonList
                    } else {
                        // Filter is active, re-apply it
                        applyFilters()
                    }
                    
                    // Load types in background without blocking UI (lazy loading)
                    viewModelScope.launch(Dispatchers.IO) {
                        val entriesWithTypes = pokedexEntries.map { entry ->
                            async {
                                try {
                                    val pokemonInfo = repository.getPokemonInfo(entry.pokemonName.lowercase(Locale.ROOT))
                                    val types = when(pokemonInfo) {
                                        is Resource.Success -> {
                                            pokemonInfo.data?.types?.map { it.type.name } ?: emptyList()
                                        }
                                        else -> emptyList()
                                    }
                                    entry.copy(types = types)
                                } catch (e: Exception) {
                                    entry.copy(types = emptyList())
                                }
                            }
                        }.awaitAll()
                        
                        // Update cached list with types
                        val cachedListMutable = cachedPokemonList.toMutableList()
                        val startIndex = cachedListMutable.size - entriesWithTypes.size
                        entriesWithTypes.forEachIndexed { index, entryWithType ->
                            if (startIndex + index < cachedListMutable.size) {
                                cachedListMutable[startIndex + index] = entryWithType
                            }
                        }
                        cachedPokemonList = cachedListMutable
                        
                        // Always re-apply filters if active, otherwise update display list
                        if (isSearching.value || selectedType.value != null) {
                            applyFilters()
                        } else {
                            pokemonList.value = cachedPokemonList
                        }
                    }
                }
                is Resource.Error -> {
                    loadError.value = result.message!!
                    isLoading.value = false
                }
                is Resource.Loading -> {
                    // no-op; loading already handled by isLoading
                }
            }
        }
    }

    fun calcDominantColor(drawable: Drawable, onFinish: (Color) -> Unit) {
        viewModelScope.launch(Dispatchers.Default) {
            val bmp = (drawable as BitmapDrawable).bitmap.copy(Bitmap.Config.ARGB_8888, true)

            Palette.from(bmp).generate { palette ->
                palette?.dominantSwatch?.rgb?.let { colorValue ->
                    onFinish(Color(colorValue))
                }
            }
        }
    }
    
    private fun loadAllPokemonForSearch() {
        if (hasLoadedAllForSearch) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            isLoadingAllForSearch.value = true
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
                            
                            // Update list progressively for search (avoid creating new list unnecessarily)
                            allPokemonListForSearch = allEntries
                        }
                        else -> {
                            // If batch fails, break and use what we have
                            break
                        }
                    }
                }
                
                hasLoadedAllForSearch = true
                
                // Update cached list with all Pokemon for search
                viewModelScope.launch(Dispatchers.Default) {
                    if (!isSearching.value) {
                        cachedPokemonList = allPokemonListForSearch
                    }
                }
            } catch (e: Exception) {
                // Error handling
            } finally {
                isLoadingAllForSearch.value = false
            }
        }
    }
}