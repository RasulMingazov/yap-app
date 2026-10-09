package app.yap.app.root.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.yap.core.design.theme.YapTheme

@Composable
internal fun ProfilePlaceholderScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text(
            text = "Скоро здесь появится профиль: подписка, история диалогов и оформление.",
            color = YapTheme.colors.muted,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
