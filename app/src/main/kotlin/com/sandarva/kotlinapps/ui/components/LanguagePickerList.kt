package com.sandarva.kotlinapps.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.data.Country
import com.sandarva.kotlinapps.data.CountryData
import com.sandarva.kotlinapps.ui.theme.BuddyColors

/**
 * Searchable flag + language list, shared between the onboarding language step and the settings
 * "change language" screen (same idea as a past Flutter nationality picker — full list, live
 * filter, one tap to choose).
 */
@Composable
fun LanguagePickerList(
    selectedCode: String?,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onSelected: (Country) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        val q = query.trim()
        if (q.isBlank()) CountryData.sortedByName
        else CountryData.sortedByName.filter { it.name.contains(q, ignoreCase = true) || it.language.contains(q, ignoreCase = true) }
    }

    Column(modifier.fillMaxSize()) {
        LanguageSearchField(query, { query = it }, enabled)
        Spacer12()
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No languages found", style = MaterialTheme.typography.bodyLarge, color = BuddyColors.Mist)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(filtered, key = { it.code }) { country ->
                    CountryLanguageRow(
                        country = country,
                        selected = country.code == selectedCode,
                        enabled = enabled,
                        onTap = { onSelected(country) }
                    )
                }
                item { Spacer12() }
            }
        }
    }
}

@Composable
private fun Spacer12() {
    androidx.compose.foundation.layout.Spacer(Modifier.height(10.dp))
}

@Composable
private fun LanguageSearchField(value: String, onValueChange: (String) -> Unit, enabled: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        placeholder = { Text("Search country or language", color = BuddyColors.Mist) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = BuddyColors.Mist) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = RoundedCornerShape(22.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BuddyColors.Violet,
            unfocusedBorderColor = BuddyColors.Line,
            focusedTextColor = BuddyColors.Ink,
            unfocusedTextColor = BuddyColors.Ink,
            focusedContainerColor = BuddyColors.Snow,
            unfocusedContainerColor = BuddyColors.Snow,
            cursorColor = BuddyColors.Violet
        )
    )
}
