package com.plcoding.jetpackcomposepokedex.pokemonlist

import android.content.IntentSender.OnFinished
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.capitalize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.plcoding.jetpackcomposepokedex.data.models.PokemonListEntry
import com.plcoding.jetpackcomposepokedex.repository.PokemonRepository
import com.plcoding.jetpackcomposepokedex.util.Constants
import com.plcoding.jetpackcomposepokedex.util.Constants.PAGE_SIZE
import com.plcoding.jetpackcomposepokedex.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject


@HiltViewModel
class PokemonListViewModel @Inject constructor(private val repository: PokemonRepository) :
    ViewModel() {

    private var currPage = 0

    var pokemonList = mutableStateOf<List<PokemonListEntry>>(listOf())

    var loadError = mutableStateOf("")
    var isLoading = mutableStateOf(false)
    var endReached = mutableStateOf(false)

init {
    loadPokemonPaginated()
}
    fun loadPokemonPaginated() {
        viewModelScope.launch {
            isLoading.value = true
            val result = repository.getPokemonList(PAGE_SIZE, currPage * PAGE_SIZE)
            when (result) {
                is Resource.Loading -> {

                }

                is Resource.Success -> {
                    endReached.value = currPage * PAGE_SIZE >= result.data?.count!!
                    val pokedexEntries = result.data.results.mapIndexed { index, entry ->
                        val number = if (entry.url.endsWith("/")) {
                            entry.url.dropLast(1).takeLastWhile {
                                it.isDigit()
                            }
                        } else {
                            entry.url.takeLastWhile {
                                it.isDigit()
                            }
                        }

                        val url = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${number}.png"

                        PokemonListEntry(entry.name.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(
                                Locale.ROOT
                            ) else it.toString()
                        }, url, number.toInt())
                    }
                    currPage++

                    loadError.value = ""
                    isLoading.value = false
                    pokemonList.value += pokedexEntries

                }

                is Resource.Error -> {
                    loadError.value = result.message!!
                    isLoading.value = false


                }


            }
        }
    }

    fun calcDominantColor(image: Drawable, onFinished: (Color) -> Unit) {
        val bitmap = (image as BitmapDrawable).bitmap.copy(Bitmap.Config.ARGB_8888, true)

        Palette.from(bitmap).generate { palette ->
            palette?.dominantSwatch?.rgb?.let {
                onFinished(Color(it))

            }

        }
    }
}