package com.plcoding.pokedex.pokemonlist

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.FloatingActionButton
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
// removed obsolete import for compose navigate
import coil.request.ImageRequest
import coil.compose.AsyncImage
import com.plcoding.pokedex.R
import com.plcoding.pokedex.data.models.PokedexListEntry
import com.plcoding.pokedex.ui.theme.RobotoCondensed
import com.plcoding.pokedex.util.parseTypeNameToColor
import com.plcoding.pokedex.util.parseTypeNameToIconRes
import java.util.*

@Composable
fun PokemonListScreen(
    navController: NavController,
    selectForSlot: String? = null,
    currentPokemon1: String? = null,
    currentPokemon2: String? = null,
    viewModel: PokemonListViewModel = hiltViewModel()
) {
    Surface(
        color = MaterialTheme.colors.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column {
                Spacer(modifier = Modifier.height(20.dp))
                Image(
                    painter = painterResource(id = R.drawable.ic_international_pok_mon_logo),
                    contentDescription = "Pokemon",
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(CenterHorizontally)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SearchBar(
                        hint = "Search...",
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.searchPokemonList(it)
                    }
                    TypeFilterIcon(viewModel = viewModel)
                }
            Spacer(modifier = Modifier.height(16.dp))
            PokemonList(
                navController = navController,
                selectForSlot = selectForSlot,
                currentPokemon1 = currentPokemon1,
                currentPokemon2 = currentPokemon2
            )
            }
            FloatingActionButton(
                onClick = {
                    navController.navigate("pokemon_comparison_screen/null/null")
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                backgroundColor = MaterialTheme.colors.primary
            ) {
                Icon(
                    imageVector = Icons.Filled.CompareArrows,
                    contentDescription = "So sánh Pokemon",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    hint: String = "",
    onSearch: (String) -> Unit = {}
) {
    var text by remember {
        mutableStateOf("")
    }
    var isHintDisplayed by remember {
        mutableStateOf(hint != "")
    }

    Box(modifier = modifier) {
        BasicTextField(
            value = text,
            onValueChange = {
                text = it
                onSearch(it)
            },
            maxLines = 1,
            singleLine = true,
            textStyle = TextStyle(color = Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(5.dp, CircleShape)
                .background(Color.White, CircleShape)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .onFocusChanged {
                    isHintDisplayed = !it.isFocused && text.isEmpty()
                }
        )
        if(isHintDisplayed) {
            Text(
                text = hint,
                color = Color.LightGray,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
fun TypeFilterIcon(
    viewModel: PokemonListViewModel
) {
    val selectedType by remember { viewModel.selectedType }
    var expanded by remember { mutableStateOf(false) }
    
    val pokemonTypes = listOf(
        "normal", "fire", "water", "electric", "grass", "ice",
        "fighting", "poison", "ground", "flying", "psychic", "bug",
        "rock", "ghost", "dragon", "dark", "steel", "fairy"
    )
    
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .size(48.dp)
                .shadow(5.dp, CircleShape)
                .background(Color.White, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.FilterList,
                contentDescription = "Filter by Type",
                tint = if(selectedType != null) parseTypeNameToColor(selectedType!!) else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            DropdownMenuItem(onClick = {
                viewModel.filterByType(null)
                expanded = false
            }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("All Types")
                }
            }
            Divider()
            pokemonTypes.forEach { type ->
                DropdownMenuItem(onClick = {
                    viewModel.filterByType(type)
                    expanded = false
                }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(parseTypeNameToColor(type)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = parseTypeNameToIconRes(type)),
                                contentDescription = type,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(type.capitalize(Locale.ROOT))
                    }
                }
            }
        }
    }
}

@Composable
fun PokemonList(
    navController: NavController,
    selectForSlot: String? = null,
    currentPokemon1: String? = null,
    currentPokemon2: String? = null,
    viewModel: PokemonListViewModel = hiltViewModel()
) {
    val pokemonList by remember { viewModel.pokemonList }
    val endReached by remember { viewModel.endReached }
    val loadError by remember { viewModel.loadError }
    val isLoading by remember { viewModel.isLoading }
    val isSearching by remember { viewModel.isSearching }

    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        val itemCount = if(pokemonList.size % 2 == 0) {
            pokemonList.size / 2
        } else {
            pokemonList.size / 2 + 1
        }
        items(itemCount) {
            if(it >= itemCount - 1 && !endReached && !isLoading && !isSearching) {
                LaunchedEffect(key1 = true) {
                    viewModel.loadPokemonPaginated()
                }
            }
            PokedexRow(
                rowIndex = it,
                entries = pokemonList,
                navController = navController,
                selectForSlot = selectForSlot,
                currentPokemon1 = currentPokemon1,
                currentPokemon2 = currentPokemon2
            )
        }
    }

    Box(
        contentAlignment = Center,
        modifier = Modifier.fillMaxSize()
    ) {
        if(isLoading) {
            CircularProgressIndicator(color = MaterialTheme.colors.primary)
        }
        if(loadError.isNotEmpty()) {
            RetrySection(error = loadError) {
                viewModel.loadPokemonPaginated()
            }
        }
    }

}

@Composable
fun PokedexEntry(
    entry: PokedexListEntry,
    navController: NavController,
    modifier: Modifier = Modifier,
    selectForSlot: String? = null,
    currentPokemon1: String? = null,
    currentPokemon2: String? = null,
    viewModel: PokemonListViewModel = hiltViewModel()
) {
    val defaultDominantColor = MaterialTheme.colors.surface
    var dominantColor by remember {
        mutableStateOf(defaultDominantColor)
    }

    Box(
        contentAlignment = Center,
        modifier = modifier
            .shadow(5.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .aspectRatio(1f)
            .background(
                Brush.verticalGradient(
                    listOf(
                        dominantColor,
                        defaultDominantColor
                    )
                )
            )
            .clickable {
                when (selectForSlot) {
                    "select_for_slot1" -> {
                        // Navigate directly to comparison with this Pokemon as Pokemon 1
                        navController.navigate(
                            "pokemon_comparison_screen/${entry.pokemonName}/${currentPokemon2 ?: "null"}"
                        )
                    }
                    "select_for_slot2" -> {
                        // Navigate directly to comparison with this Pokemon as Pokemon 2
                        navController.navigate(
                            "pokemon_comparison_screen/${currentPokemon1 ?: "null"}/${entry.pokemonName}"
                        )
                    }
                    else -> {
                        // Normal navigation to detail screen
                        navController.navigate(
                            "pokemon_detail_screen/${dominantColor.toArgb()}/${entry.pokemonName}"
                        )
                    }
                }
            }
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(entry.imageUrl)
                    .build(),
                contentDescription = entry.pokemonName,
                onSuccess = { success ->
                    viewModel.calcDominantColor(success.result.drawable) { color ->
                        dominantColor = color
                    }
                },
                modifier = Modifier
                    .size(120.dp)
                    .align(CenterHorizontally)
            )
            Text(
                text = entry.pokemonName,
                fontFamily = RobotoCondensed,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun PokedexRow(
    rowIndex: Int,
    entries: List<PokedexListEntry>,
    navController: NavController,
    selectForSlot: String? = null,
    currentPokemon1: String? = null,
    currentPokemon2: String? = null
) {
    Column {
        Row {
            PokedexEntry(
                entry = entries[rowIndex * 2],
                navController = navController,
                modifier = Modifier.weight(1f),
                selectForSlot = selectForSlot,
                currentPokemon1 = currentPokemon1,
                currentPokemon2 = currentPokemon2
            )
            Spacer(modifier = Modifier.width(16.dp))
            if(entries.size >= rowIndex * 2 + 2) {
                PokedexEntry(
                    entry = entries[rowIndex * 2 + 1],
                    navController = navController,
                    modifier = Modifier.weight(1f),
                    selectForSlot = selectForSlot,
                    currentPokemon1 = currentPokemon1,
                    currentPokemon2 = currentPokemon2
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun RetrySection(
    error: String,
    onRetry: () -> Unit
) {
    Column {
        Text(error, color = Color.Red, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { onRetry() },
            modifier = Modifier.align(CenterHorizontally)
        ) {
            Text(text = "Retry")
        }
    }
}