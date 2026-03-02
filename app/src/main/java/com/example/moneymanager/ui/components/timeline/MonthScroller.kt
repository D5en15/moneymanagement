package com.example.moneymanager.ui.components.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MonthScroller(
    months: List<Date>,
    selectedDate: Date,
    onMonthSelected: (Date) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Scroll to the selected month (middle of the generated list)
    LaunchedEffect(selectedDate) {
        val index = months.indexOfFirst { 
            isSameMonth(it, selectedDate)
        }
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier.padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(items = months, key = { date -> date.time }) { date ->
            MonthItem(
                date = date,
                isSelected = isSameMonth(date, selectedDate),
                onClick = { onMonthSelected(date) }
            )
        }
    }
}

@Composable
fun MonthItem(
    date: Date,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
    val yearFormat = SimpleDateFormat("yy", Locale.getDefault())

    Box(
        modifier = Modifier
            .padding(end = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${monthFormat.format(date)} ${yearFormat.format(date)}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer 
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun isSameMonth(d1: Date, d2: Date): Boolean {
    val fmt = SimpleDateFormat("yyyyMM", Locale.getDefault())
    return fmt.format(d1) == fmt.format(d2)
}
