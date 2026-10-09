package app.yap.feature.scenario.presentation.common.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.result.LocalResultEventBus
import app.yap.core.common.navigation.Navigator
import app.yap.core.design.theme.YapTheme
import app.yap.feature.scenario.presentation.common.RepeatConfirmationNavKey
import app.yap.feature.scenario.presentation.common.ScenarioResultKeys
import org.koin.compose.koinInject

private val SheetPadding = 20.dp

@Composable
internal fun RepeatConfirmationSheet(key: RepeatConfirmationNavKey) {
    val colors = YapTheme.colors
    val navigator = koinInject<Navigator>()
    val resultEventBus = LocalResultEventBus.current

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = SheetPadding)) {
        Text(
            text = "Пройти заново?".uppercase(),
            color = colors.onSurface,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 27.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "Сценарий «${key.title}» начнётся с первого шага. Прошлый результат и разбор " +
                "ошибок удалятся, серия и минуты практики останутся.",
            color = colors.muted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(top = 12.dp),
        )
        CtaButton(
            label = "Начать заново",
            onClick = {
                resultEventBus.sendResult(
                    resultKey = ScenarioResultKeys.REPEAT_CONFIRMATION,
                    result = key.scenarioId,
                )
                navigator.back()
            },
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
        )
        Text(
            text = "Отмена",
            color = colors.muted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .heightIn(min = 48.dp)
                .clickableNoIndication { navigator.back() }
                .padding(top = 16.dp),
        )
    }
}
