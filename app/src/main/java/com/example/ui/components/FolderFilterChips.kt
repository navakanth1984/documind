package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun FolderFilterChips(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    folderCounts: Map<String, Int>
) {
    val categories = listOf(
        "All",
        "Computer Science",
        "Biology",
        "Mathematics",
        "Literature",
        "Physics",
        "Confidential / Offline",
        "Exam Prep"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { cat ->
            val isSelected = selectedCategory.equals(cat, ignoreCase = true)
            val count = if (cat == "All") folderCounts.values.sum() else (folderCounts[cat] ?: 0)

            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(cat) },
                label = { Text("$cat ($count)") },
                leadingIcon = {
                    val icon = when (cat) {
                        "Confidential / Offline" -> Icons.Default.Lock
                        "All" -> Icons.Default.FolderSpecial
                        else -> Icons.Default.Folder
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag("filter_chip_${cat.lowercase().replace(" ", "_")}")
            )
        }
    }
}
