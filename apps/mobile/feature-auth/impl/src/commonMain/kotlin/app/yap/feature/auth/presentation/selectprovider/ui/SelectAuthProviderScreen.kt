package app.yap.feature.auth.presentation.selectprovider.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.LocalResultEventBus
import app.yap.core.design.theme.YapTheme
import app.yap.feature.auth.api.entity.AuthProvider
import app.yap.feature.auth.generated.resources.Res
import app.yap.feature.auth.generated.resources.login_provider_semantics
import app.yap.feature.auth.generated.resources.login_provider_sheet_title
import app.yap.feature.auth.presentation.common.AuthResultKeys
import app.yap.feature.auth.presentation.selectprovider.SelectAuthProviderViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val SECTION_LABEL_SIZE = 13
private const val SECTION_LABEL_TRACKING = 0.04
private const val ROW_LABEL_SIZE = 16
private val MarkGap = 12.dp
private val RowGap = 2.dp
private val RowMinHeight = 52.dp
private val RowSidePadding = 4.dp
private val SectionLabelBottomPadding = 10.dp
private val SectionLabelTopPadding = 4.dp

@Composable
internal fun SelectAuthProviderScreen() {
    val viewModel: SelectAuthProviderViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val resultEventBus = LocalResultEventBus.current

    SelectAuthProviderContent(
        uiState = uiState,
        onProviderChosen = { provider ->
            resultEventBus.sendResult(
                resultKey = AuthResultKeys.PROVIDER_SELECTION,
                result = provider,
            )
            viewModel.onEvent(SelectAuthProviderViewModel.Event.ProviderChosen)
        },
    )
}

@Composable
internal fun SelectAuthProviderContent(
    uiState: SelectAuthProviderViewModel.UiState,
    onProviderChosen: (AuthProvider) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(RowGap),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(Res.string.login_provider_sheet_title).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontSize = SECTION_LABEL_SIZE.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = SECTION_LABEL_TRACKING.em,
            color = YapTheme.colors.sectionLabel,
            modifier = Modifier
                .padding(
                    bottom = SectionLabelBottomPadding,
                    start = RowSidePadding,
                    top = SectionLabelTopPadding,
                )
                .testTag(SelectAuthProviderTestTags.SECTION_LABEL),
        )

        uiState.providers.forEach { provider ->
            AuthProviderRow(provider = provider, onClick = { onProviderChosen(provider.provider) })
        }
    }
}

@Composable
private fun AuthProviderRow(
    provider: SelectAuthProviderViewModel.UiState.Provider,
    onClick: () -> Unit,
) {
    val colors = YapTheme.colors
    val name = stringResource(provider.ui.labelRes)
    val spokenName = stringResource(Res.string.login_provider_semantics, name)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Surface(
        onClick = onClick,
        color = Color.Transparent,
        contentColor = if (isPressed) colors.accent else colors.onSurface,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(provider.ui.testTag)
            .semantics { contentDescription = spokenName },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MarkGap),
            modifier = Modifier
                .heightIn(min = RowMinHeight)
                .padding(horizontal = RowSidePadding),
        ) {
            Icon(
                painter = painterResource(provider.ui.iconRes),
                contentDescription = null,
                tint = if (provider.ui.isMonochrome) LocalContentColor.current else Color.Unspecified,
            )
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontSize = ROW_LABEL_SIZE.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
