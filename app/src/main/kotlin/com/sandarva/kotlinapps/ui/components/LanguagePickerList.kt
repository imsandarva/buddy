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
 * Searchable country list. On first-run, [suggested] sits above the alphabet already checked —
 * the Apple / Airbnb “this is the one” pattern — and drops away while they type.
 */
@Composable
fun LanguagePickerList(
    selectedCode: String?,
    enabled: Boolean = true,
    suggested: Country? = null,
    modifier: Modifier = Modifier,
    onSelected: (Country) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val pinned = suggested.takeIf { query.isBlank() }
    val filtered = remember(query, pinned?.code) { countriesMatching(query, pinned?.code) }

    Column(modifier.fillMaxSize()) {
        UnderlineField(query, { query = it }, placeholder = "Find a country", enabled = enabled, imeAction = ImeAction.Search)
        if (filtered.isEmpty() && pinned == null) {
            Box(Modifier.fillMaxWidth().padding(top = 36.dp), contentAlignment = Alignment.Center) {
                Text("Nothing matches that.", style = MaterialTheme.typography.bodyLarge, color = BuddyColors.Mist)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)) {
                if (pinned != null) {
                    item(key = "label-suggested") { PickerLabel("Default") }
                    item(key = "suggested-${pinned.code}") {
                        CountryLanguageRow(pinned, selected = pinned.code == selectedCode, enabled = enabled, onTap = { onSelected(pinned) }, modifier = Modifier.animateItem())
                    }
                    if (filtered.isNotEmpty()) item(key = "label-all") { PickerLabel("All countries") }
                }
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

@Composable
private fun PickerLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = BuddyColors.Mist,
        modifier = Modifier.padding(top = 18.dp, bottom = 2.dp)
    )
}

private fun countriesMatching(raw: String, except: String?): List<Country> {
    val q = raw.trim()
    val base = if (q.isBlank()) CountryData.sortedByName
    else CountryData.sortedByName.filter { it.name.contains(q, ignoreCase = true) || it.language.contains(q, ignoreCase = true) }
    return if (except == null) base else base.filter { it.code != except }
}
