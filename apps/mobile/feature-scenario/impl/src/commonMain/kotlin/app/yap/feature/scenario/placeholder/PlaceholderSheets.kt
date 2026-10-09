package app.yap.feature.scenario.placeholder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.yap.core.design.theme.YapTheme

private val SheetHorizontalPadding = 20.dp
private val SheetTextGap = 12.dp

@Composable
internal fun SessionComingSoonSheet() {
    PlaceholderSheet(
        title = "Скоро",
        text = "Разговорная практика появится в одном из ближайших обновлений. " +
            "Прогресс уже сохраняется за вашим аккаунтом.",
    )
}

@Composable
internal fun PlaceholderPaywallSheet() {
    PlaceholderSheet(
        title = "Подписка скоро",
        text = "Оформление подписки появится в одном из ближайших обновлений. " +
            "После покупки выбранный сценарий откроется сам.",
    )
}

@Composable
private fun PlaceholderSheet(title: String, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SheetHorizontalPadding),
    ) {
        Text(
            text = title,
            color = YapTheme.colors.onSurface,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = text,
            color = YapTheme.colors.muted,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = SheetTextGap),
        )
    }
}
