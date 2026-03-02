package com.example.moneymanager.ui.components.timeline

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.moneymanager.utils.formatMoneyCompact
import java.util.Date
import java.util.Locale

@Composable
fun TimelineHeader(
    currentMonth: String,
    totalBalance: Double,
    surroundingMonths: List<Date>,
    selectedDate: Date,
    onMonthSelected: (Date) -> Unit,
    income: Double,
    expense: Double
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MonthScroller(
            months = surroundingMonths,
            selectedDate = selectedDate,
            onMonthSelected = onMonthSelected
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = currentMonth,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "\u0E3F" + formatMoneyCompact(totalBalance, Locale.getDefault()),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(24.dp))

        SummaryBlock(
            income = income,
            expense = expense,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
