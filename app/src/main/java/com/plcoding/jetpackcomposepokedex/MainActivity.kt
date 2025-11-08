package com.plcoding.pokedex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import com.plcoding.pokedex.mypokemon.MyPokemonScreen
import com.plcoding.pokedex.pokemondetail.PokemonDetailScreen
import com.plcoding.pokedex.pokemonlist.PokemonListScreen
import com.plcoding.pokedex.pokemoncomparison.PokemonComparisonScreen
import com.plcoding.pokedex.ui.theme.PokedexTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PokedexTheme {
                val navController = rememberNavController()
                var selectedTabIndex by remember { mutableStateOf(0) }
                
                NavHost(
                    navController = navController,
                    startDestination = "main_tabs"
                ) {
                    composable("main_tabs") {
                        Scaffold(
                            topBar = {
                                TabRow(selectedTabIndex = selectedTabIndex) {
                                    Tab(
                                        selected = selectedTabIndex == 0,
                                        onClick = { selectedTabIndex = 0 },
                                        text = { Text("All Pokemon") }
                                    )
                                    Tab(
                                        selected = selectedTabIndex == 1,
                                        onClick = { selectedTabIndex = 1 },
                                        text = { Text("My Pokemon") }
                                    )
                                }
                            }
                        ) { paddingValues ->
                            Box(modifier = Modifier.padding(paddingValues)) {
                                when (selectedTabIndex) {
                                    0 -> PokemonListScreen(navController = navController)
                                    1 -> MyPokemonScreen(navController = navController)
                                }
                            }
                        }
                    }
                    composable(
                        "pokemon_list_screen/{selectForSlot}/{currentPokemon1}/{currentPokemon2}",
                        arguments = listOf(
                            navArgument("selectForSlot") {
                                type = NavType.StringType
                            },
                            navArgument("currentPokemon1") {
                                type = NavType.StringType
                                nullable = true
                            },
                            navArgument("currentPokemon2") {
                                type = NavType.StringType
                                nullable = true
                            }
                        )
                    ) {
                        val selectForSlot = remember {
                            it.arguments?.getString("selectForSlot") ?: ""
                        }
                        val currentPokemon1 = remember {
                            val name = it.arguments?.getString("currentPokemon1")
                            if (name == "null") null else name
                        }
                        val currentPokemon2 = remember {
                            val name = it.arguments?.getString("currentPokemon2")
                            if (name == "null") null else name
                        }
                        PokemonListScreen(
                            navController = navController,
                            selectForSlot = selectForSlot,
                            currentPokemon1 = currentPokemon1,
                            currentPokemon2 = currentPokemon2
                        )
                    }
                    composable(
                        "pokemon_detail_screen/{dominantColor}/{pokemonName}",
                        arguments = listOf(
                            navArgument("dominantColor") {
                                type = NavType.IntType
                            },
                            navArgument("pokemonName") {
                                type = NavType.StringType
                            }
                        )
                    ) {
                        val dominantColor = remember {
                            val color = it.arguments?.getInt("dominantColor")
                            color?.let { Color(it) } ?: Color.White
                        }
                        val pokemonName = remember {
                            it.arguments?.getString("pokemonName")
                        }
                        PokemonDetailScreen(
                            dominantColor = dominantColor,
                            pokemonName = pokemonName?.toLowerCase(Locale.ROOT) ?: "",
                            navController = navController
                        )
                    }
                    composable(
                        "pokemon_comparison_screen/{pokemon1Name}/{pokemon2Name}",
                        arguments = listOf(
                            navArgument("pokemon1Name") {
                                type = NavType.StringType
                                nullable = true
                            },
                            navArgument("pokemon2Name") {
                                type = NavType.StringType
                                nullable = true
                            }
                        )
                    ) {
                        val pokemon1Name = remember {
                            val name = it.arguments?.getString("pokemon1Name")
                            if (name == "null") null else name
                        }
                        val pokemon2Name = remember {
                            val name = it.arguments?.getString("pokemon2Name")
                            if (name == "null") null else name
                        }
                        PokemonComparisonScreen(
                            navController = navController,
                            pokemon1Name = pokemon1Name,
                            pokemon2Name = pokemon2Name
                        )
                    }
                }
            }
        }
    }
}
