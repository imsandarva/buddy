package com.sandarva.kotlinapps.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
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
 * Searchable country list shared by first-run language and Settings. The field is only a line;
 * the list is the page — one tap chooses, and filtering never rebuilds rows that did not change.
 */
@Composable
fun LanguagePickerList(
    selectedCode: String?,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onSelected: (Country) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) { countriesMatching(query) }

    Column(modifier.fillMaxSize()) {
        UnderlineField(query, { query = it }, placeholder = "Find a country", enabled = enabled, imeAction = ImeAction.Search)
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(top = 36.dp), contentAlignment = Alignment.Center) {
                Text("Nothing matches that.", style = MaterialTheme.typography.bodyLarge, color = BuddyColors.Mist)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp)) {
                items(filtered, key = { it.code }) { country ->
                    CountryLanguageRow(
                        country = country,
                        selected = country.code == selectedCode,
                        enabled = enabled,
                        onTap = { onSelected(country) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

private fun countriesMatching(raw: String): List<Country> {
    val q = raw.trim()
    if (q.isBlank()) return CountryData.sortedByName
    return CountryData.sortedByName.filter { it.name.contains(q, ignoreCase = true) || it.language.contains(q, ignoreCase = true) }
}
