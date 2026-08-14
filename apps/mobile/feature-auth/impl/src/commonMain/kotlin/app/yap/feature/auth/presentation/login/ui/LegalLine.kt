package app.yap.feature.auth.presentation.login.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import app.yap.core.design.theme.YapTheme
import app.yap.feature.auth.generated.resources.Res
import app.yap.feature.auth.generated.resources.login_legal_prefix
import app.yap.feature.auth.generated.resources.login_legal_privacy
import app.yap.feature.auth.generated.resources.login_legal_privacy_semantics
import app.yap.feature.auth.generated.resources.login_legal_separator
import app.yap.feature.auth.generated.resources.login_legal_suffix
import app.yap.feature.auth.generated.resources.login_legal_terms
import app.yap.feature.auth.generated.resources.login_legal_terms_semantics
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LegalLine(
    privacyUrl: String?,
    termsUrl: String?,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val colors = YapTheme.colors

    FlowRow(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.testTag(LoginTestTags.LEGAL_LINE),
    ) {
        Text(
            text = stringResource(Res.string.login_legal_prefix),
            style = MaterialTheme.typography.bodySmall,
            color = colors.bodyMuted,
        )
        LegalLink(
            destination = termsUrl,
            labelRes = Res.string.login_legal_terms,
            semanticsRes = Res.string.login_legal_terms_semantics,
            testTag = LoginTestTags.LEGAL_TERMS_LINK,
            onOpen = uriHandler::openUri,
        )
        Text(
            text = stringResource(Res.string.login_legal_separator),
            style = MaterialTheme.typography.bodySmall,
            color = colors.bodyMuted,
        )
        LegalLink(
            destination = privacyUrl,
            labelRes = Res.string.login_legal_privacy,
            semanticsRes = Res.string.login_legal_privacy_semantics,
            testTag = LoginTestTags.LEGAL_PRIVACY_LINK,
            onOpen = uriHandler::openUri,
        )
        Text(
            text = stringResource(Res.string.login_legal_suffix),
            style = MaterialTheme.typography.bodySmall,
            color = colors.bodyMuted,
        )
    }
}

@Composable
private fun LegalLink(
    destination: String?,
    labelRes: StringResource,
    semanticsRes: StringResource,
    testTag: String,
    onOpen: (String) -> Unit,
) {
    val spokenName = stringResource(semanticsRes)

    Text(
        text = stringResource(labelRes),
        style = MaterialTheme.typography.bodySmall,
        color = YapTheme.colors.link,
        fontWeight = FontWeight.SemiBold,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .testTag(testTag)
            .semantics { contentDescription = spokenName }
            .clickable { destination?.let(onOpen) },
    )
}
