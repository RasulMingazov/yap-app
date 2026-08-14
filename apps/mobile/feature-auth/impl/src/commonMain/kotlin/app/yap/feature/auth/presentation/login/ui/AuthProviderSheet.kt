package app.yap.feature.auth.presentation.login.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import app.yap.feature.auth.generated.resources.Res
import app.yap.feature.auth.generated.resources.login_provider_semantics
import app.yap.feature.auth.generated.resources.login_provider_sheet_title
import app.yap.feature.auth.presentation.login.LoginViewModel
import org.jetbrains.compose.resources.stringResource

private const val SHEET_TITLE_TRACKING = 0.04
private val MARK_SIZE = 24.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AuthProviderSheet(
    providers: List<LoginViewModel.UiState.Provider>,
    onDismiss: () -> Unit,
    onProviderChosen: (LoginViewModel.UiState.Provider) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = loginColors()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = colors.surface,
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
            Text(
                text = stringResource(Res.string.login_provider_sheet_title),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = SHEET_TITLE_TRACKING.em,
                color = colors.sheetLabel,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 10.dp),
            )

            providers.forEach { provider ->
                AuthProviderRow(provider = provider, onClick = { onProviderChosen(provider) })
            }
        }
    }
}

@Composable
private fun AuthProviderRow(
    provider: LoginViewModel.UiState.Provider,
    onClick: () -> Unit,
) {
    val colors = loginColors()
    val name = stringResource(provider.labelRes)
    val spokenName = stringResource(Res.string.login_provider_semantics, name)

    Surface(
        onClick = onClick,
        color = Color.Transparent,
        contentColor = colors.onSurface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(providerTestTag(provider))
            .semantics { contentDescription = spokenName },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .heightIn(min = 52.dp)
                .padding(horizontal = 4.dp, vertical = 8.dp),
        ) {
            AuthProviderMark(name = name, tint = colors.accent)
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun AuthProviderMark(
    name: String,
    tint: Color,
) {
    Surface(
        color = tint,
        shape = CircleShape,
        modifier = Modifier.size(MARK_SIZE),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.size(MARK_SIZE),
        ) {
            Text(
                text = name.take(1),
                style = MaterialTheme.typography.labelSmall,
                color = loginColors().onAction,
            )
        }
    }
}

internal fun providerTestTag(provider: LoginViewModel.UiState.Provider): String =
    "login_provider_${provider.provider.name.lowercase()}"
