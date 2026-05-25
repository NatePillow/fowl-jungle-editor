@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package editors.recipes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import model.Recipe

@Composable
fun RecipesEditor(recipes: List<Recipe>, skillNames: Map<String, String>, itemNames: Map<String, String>) {
    if (recipes.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No recipes", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize()) {
        stickyHeader {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeaderCell("Name", 2.5f)
                    HeaderCell("Ticks", 1f)
                    HeaderCell("Skills", 2.5f)
                    HeaderCell("Requires", 4f)
                    HeaderCell("Result", 2f)
                }
            }
        }
        itemsIndexed(recipes) { index, recipe ->
            val bg = if (index % 2 == 0) Color.Transparent
                     else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            Surface(color = bg) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DataCell(recipe.name, 2.5f)
                    DataCell(recipe.ticks.toString(), 1f)
                    DataCell(
                        recipe.skillRequirements.entries
                            .joinToString(", ") { (k, v) -> "${skillNames[k] ?: k} $v" },
                        2.5f
                    )
                    DataCell(
                        recipe.itemRequirements.entries
                            .joinToString(", ") { (k, v) -> "${itemNames[k] ?: k} x$v" },
                        4f
                    )
                    DataCell(
                        recipe.result.entries
                            .joinToString(", ") { (k, v) -> "${itemNames[k] ?: k} x$v" },
                        2f
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun RowScope.HeaderCell(text: String, weight: Float) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun RowScope.DataCell(text: String, weight: Float) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
