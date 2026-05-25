@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package editors.equipment

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
import model.EquipmentItem

@Composable
fun EquipmentEditor(items: List<EquipmentItem>, skillNames: Map<String, String>) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No items", style = MaterialTheme.typography.bodyMedium)
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
                    HeaderCell("Name", 3f)
                    HeaderCell("Slot", 1.5f)
                    HeaderCell("Material", 1.5f)
                    HeaderCell("Gold", 1f)
                    HeaderCell("Dmg", 1f)
                    HeaderCell("Prot", 1f)
                    HeaderCell("Lvl", 1f)
                    HeaderCell("Skills", 3f)
                }
            }
        }
        itemsIndexed(items) { index, item ->
            val bg = if (index % 2 == 0) Color.Transparent
                     else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            Surface(color = bg) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DataCell(item.name, 3f)
                    DataCell(item.slotType, 1.5f)
                    DataCell(item.material, 1.5f)
                    DataCell(item.goldValue.toString(), 1f)
                    DataCell(if (item.damage > 0) item.damage.toString() else "—", 1f)
                    DataCell(if (item.protection > 0) item.protection.toString() else "—", 1f)
                    DataCell(if (item.levelRequirement > 0) item.levelRequirement.toString() else "—", 1f)
                    DataCell(
                        item.skillRequirements.keys
                            .joinToString(", ") { skillNames[it] ?: it },
                        3f
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
