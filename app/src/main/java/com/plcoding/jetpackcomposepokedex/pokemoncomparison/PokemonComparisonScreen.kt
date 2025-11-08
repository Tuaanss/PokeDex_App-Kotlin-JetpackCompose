package com.plcoding.pokedex.pokemoncomparison

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.plcoding.pokedex.data.models.PokedexListEntry
import com.plcoding.pokedex.data.remote.responses.Pokemon
import com.plcoding.pokedex.data.remote.responses.Type
import com.plcoding.pokedex.util.Resource
import com.plcoding.pokedex.util.parseStatToAbbr
import com.plcoding.pokedex.util.parseStatToColor
import com.plcoding.pokedex.util.parseTypeToColor
import java.util.*

@Composable
fun PokemonComparisonScreen(
    navController: NavController,
    pokemon1Name: String?,
    pokemon2Name: String?,
    viewModel: PokemonComparisonViewModel = hiltViewModel()
) {
    LaunchedEffect(pokemon1Name, pokemon2Name) {
        viewModel.loadPokemons(pokemon1Name, pokemon2Name)
    }

    val pokemon1Resource by remember { viewModel.pokemon1 }
    val pokemon2Resource by remember { viewModel.pokemon2 }
    val currentPokemon1Name by remember { viewModel.pokemon1Name }
    val currentPokemon2Name by remember { viewModel.pokemon2Name }

    Surface(
        color = MaterialTheme.colors.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            TopAppBar(
                title = { Text("So sánh Pokemon") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = Color.White
            )

            when {
                (pokemon1Name != null && pokemon1Resource is Resource.Loading) || 
                (pokemon2Name != null && pokemon2Resource is Resource.Loading) -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                pokemon1Resource is Resource.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Lỗi khi tải Pokemon 1: ${(pokemon1Resource as Resource.Error).message}",
                                color = Color.Red
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { navController.popBackStack() }) {
                                Text("Quay lại")
                            }
                        }
                    }
                }
                pokemon2Resource is Resource.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Lỗi khi tải Pokemon 2: ${(pokemon2Resource as Resource.Error).message}",
                                color = Color.Red
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { navController.popBackStack() }) {
                                Text("Quay lại")
                            }
                        }
                    }
                }
                (pokemon1Resource is Resource.Success || pokemon1Name == null) && 
                (pokemon2Resource is Resource.Success || pokemon2Name == null) -> {
                    ComparisonContentWithSlots(
                        pokemon1 = (pokemon1Resource as? Resource.Success)?.data,
                        pokemon2 = (pokemon2Resource as? Resource.Success)?.data,
                        navController = navController,
                        currentPokemon1Name = currentPokemon1Name,
                        currentPokemon2Name = currentPokemon2Name
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun ComparisonContentWithSlots(
    pokemon1: Pokemon?,
    pokemon2: Pokemon?,
    navController: NavController,
    currentPokemon1Name: String?,
    currentPokemon2Name: String?,
    viewModel: PokemonComparisonViewModel = hiltViewModel()
) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedSlot by remember { mutableStateOf<Int?>(null) }
    
    val allPokemonList by remember { viewModel.allPokemonList }
    val isLoadingList by remember { viewModel.isLoadingPokemonList }
    
    // Show dialog when list is loaded or already loaded
    LaunchedEffect(allPokemonList.size, selectedSlot) {
        if (selectedSlot != null) {
            if (allPokemonList.isNotEmpty() || !isLoadingList) {
                showDialog = true
            }
        }
    }
    val scrollState = rememberScrollState()
    val maxStat = remember {
        if (pokemon1 != null && pokemon2 != null) {
            maxOf(
                pokemon1.stats.maxOfOrNull { it.baseStat } ?: 0,
                pokemon2.stats.maxOfOrNull { it.baseStat } ?: 0
            ).coerceAtLeast(100)
        } else {
            100
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pokemon images and names
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (pokemon1 != null) {
                PokemonComparisonCard(
                    pokemon = pokemon1,
                    modifier = Modifier.weight(1f),
                    navController = navController
                )
            } else {
                EmptyPokemonSlot(
                    label = "Pokemon 1",
                    onClick = {
                        selectedSlot = 1
                        if (allPokemonList.isEmpty()) {
                            viewModel.loadAllPokemonList()
                        } else {
                            showDialog = true
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            if (pokemon2 != null) {
                PokemonComparisonCard(
                    pokemon = pokemon2,
                    modifier = Modifier.weight(1f),
                    navController = navController
                )
            } else {
                EmptyPokemonSlot(
                    label = "Pokemon 2",
                    onClick = {
                        selectedSlot = 2
                        if (allPokemonList.isEmpty()) {
                            viewModel.loadAllPokemonList()
                        } else {
                            showDialog = true
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (pokemon1 != null && pokemon2 != null) {
            Spacer(modifier = Modifier.height(24.dp))

            // Stats comparison
            Text(
                text = "So sánh chỉ số",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Compare each stat
            val statNames = listOf("hp", "attack", "defense", "special-attack", "special-defense", "speed")
            statNames.forEach { statName ->
                val stat1 = pokemon1.stats.find { it.stat.name == statName }
                val stat2 = pokemon2.stats.find { it.stat.name == statName }
                
                if (stat1 != null && stat2 != null) {
                    ComparisonStatRow(
                        statName = parseStatToAbbr(stat1),
                        stat1Value = stat1.baseStat,
                        stat2Value = stat2.baseStat,
                        maxValue = maxStat,
                        statColor = parseStatToColor(stat1),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Other comparisons
            ComparisonRow(
                label = "Chiều cao",
                value1 = "${pokemon1.height / 10f}m",
                value2 = "${pokemon2.height / 10f}m",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            ComparisonRow(
                label = "Cân nặng",
                value1 = "${pokemon1.weight / 10f}kg",
                value2 = "${pokemon2.weight / 10f}kg",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            ComparisonRow(
                label = "Base Experience",
                value1 = "${pokemon1.baseExperience}",
                value2 = "${pokemon2.baseExperience}",
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Chọn 2 Pokemon để so sánh",
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        }
    }
    
    // Pokemon selection dialog
    if (showDialog) {
        PokemonSelectionDialog(
            pokemonList = allPokemonList,
            isLoading = isLoadingList,
            onDismiss = { 
                showDialog = false
                selectedSlot = null
            },
            onPokemonSelected = { pokemonName ->
                when (selectedSlot) {
                    1 -> {
                        navController.navigate(
                            "pokemon_comparison_screen/$pokemonName/${currentPokemon2Name ?: "null"}"
                        )
                    }
                    2 -> {
                        navController.navigate(
                            "pokemon_comparison_screen/${currentPokemon1Name ?: "null"}/$pokemonName"
                        )
                    }
                }
                showDialog = false
                selectedSlot = null
            }
        )
    }
}

@Composable
fun ComparisonContent(
    pokemon1: Pokemon,
    pokemon2: Pokemon,
    navController: NavController
) {
    val scrollState = rememberScrollState()
    val maxStat = remember {
        maxOf(
            pokemon1.stats.maxOfOrNull { it.baseStat } ?: 0,
            pokemon2.stats.maxOfOrNull { it.baseStat } ?: 0
        ).coerceAtLeast(100)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pokemon images and names
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PokemonComparisonCard(
                pokemon = pokemon1,
                modifier = Modifier.weight(1f),
                navController = navController
            )
            Spacer(modifier = Modifier.width(16.dp))
            PokemonComparisonCard(
                pokemon = pokemon2,
                modifier = Modifier.weight(1f),
                navController = navController
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stats comparison
        Text(
            text = "So sánh chỉ số",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Compare each stat
        val statNames = listOf("hp", "attack", "defense", "special-attack", "special-defense", "speed")
        statNames.forEach { statName ->
            val stat1 = pokemon1.stats.find { it.stat.name == statName }
            val stat2 = pokemon2.stats.find { it.stat.name == statName }
            
            if (stat1 != null && stat2 != null) {
                ComparisonStatRow(
                    statName = parseStatToAbbr(stat1),
                    stat1Value = stat1.baseStat,
                    stat2Value = stat2.baseStat,
                    maxValue = maxStat,
                    statColor = parseStatToColor(stat1),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Other comparisons
        ComparisonRow(
            label = "Chiều cao",
            value1 = "${pokemon1.height / 10f}m",
            value2 = "${pokemon2.height / 10f}m",
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        ComparisonRow(
            label = "Cân nặng",
            value1 = "${pokemon1.weight / 10f}kg",
            value2 = "${pokemon2.weight / 10f}kg",
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        ComparisonRow(
            label = "Base Experience",
            value1 = "${pokemon1.baseExperience}",
            value2 = "${pokemon2.baseExperience}",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun PokemonComparisonCard(
    pokemon: Pokemon,
    modifier: Modifier = Modifier,
    navController: NavController
) {
    val defaultColor = MaterialTheme.colors.surface
    val dominantColor = remember { defaultColor }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                navController.navigate(
                    "pokemon_detail_screen/${dominantColor.toArgb()}/${pokemon.name}"
                )
            },
        shape = RoundedCornerShape(16.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(dominantColor, defaultColor)
                    )
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(pokemon.sprites.frontDefault)
                    .build(),
                contentDescription = pokemon.name,
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "#${pokemon.id} ${pokemon.name.capitalize(Locale.ROOT)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                pokemon.types.forEach { type ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(parseTypeToColor(type))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = type.type.name.capitalize(Locale.ROOT),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ComparisonStatRow(
    statName: String,
    stat1Value: Int,
    stat2Value: Int,
    maxValue: Int,
    statColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = statName,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.weight(0.2f)
            )
            Text(
                text = "$stat1Value",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.3f),
                textAlign = TextAlign.End
            )
            // Visual comparison bar
            Box(
                modifier = Modifier
                    .weight(0.4f)
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSystemInDarkTheme()) Color(0xFF505050) else Color.LightGray
                    )
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Pokemon 1 bar
                    Box(
                        modifier = Modifier
                            .weight(stat1Value.toFloat() / maxValue)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(statColor.copy(alpha = 0.7f))
                    )
                    // Pokemon 2 bar
                    Box(
                        modifier = Modifier
                            .weight(stat2Value.toFloat() / maxValue)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(statColor)
                    )
                }
            }
            Text(
                text = "$stat2Value",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.3f),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun ComparisonRow(
    label: String,
    value1: String,
    value2: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value1,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            text = value2,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun EmptyPokemonSlot(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = 4.dp,
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "+",
                fontSize = 48.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 16.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Chọn Pokemon",
                fontSize = 12.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PokemonSelectionDialog(
    pokemonList: List<PokedexListEntry>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onPokemonSelected: (String) -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    val filteredList = remember(searchText, pokemonList) {
        if (searchText.isEmpty()) {
            pokemonList
        } else {
            pokemonList.filter { 
                it.pokemonName.contains(searchText, ignoreCase = true) ||
                it.number.toString() == searchText.trim()
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Chọn Pokemon")
        },
        text = {
            Column {
                // Search bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    BasicTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                MaterialTheme.colors.surface,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = MaterialTheme.colors.onSurface
                        ),
                        singleLine = true
                    )
                }
                
                Divider()
                
                // Pokemon list
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        items(filteredList) { entry ->
                            PokemonSelectionItem(
                                entry = entry,
                                onClick = {
                                    onPokemonSelected(entry.pokemonName)
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
fun PokemonSelectionItem(
    entry: PokedexListEntry,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(entry.imageUrl)
                .build(),
            contentDescription = entry.pokemonName,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "#${entry.number} ${entry.pokemonName}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
    Divider()
}

