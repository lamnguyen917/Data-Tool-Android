package com.elp.datatool.presentation.collect

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elp.datatool.R
import com.elp.datatool.data.sample.SampleData
import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.SearchHit
import com.elp.datatool.presentation.components.BackButton
import com.elp.datatool.presentation.components.BottomActionBar
import com.elp.datatool.presentation.components.DtTopBar
import com.elp.datatool.presentation.components.EmptyState
import com.elp.datatool.presentation.components.FieldLabel
import com.elp.datatool.presentation.components.nameRes
import com.elp.datatool.presentation.components.descriptionRes
import com.elp.datatool.presentation.components.labelRes
import com.elp.datatool.presentation.components.productIdLabel
import com.elp.datatool.ui.icons.DtIcons
import com.elp.datatool.ui.theme.DataToolTheme

@Composable
fun SearchProductsRoute(
    viewModel: CollectViewModel,
    onBack: () -> Unit,
    onOpenVerification: () -> Unit,
) {
    val collectState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    SearchProductsScreen(
        source = collectState.source,
        state = searchState,
        onBack = onBack,
        onOpenVerification = onOpenVerification,
        onQueryChange = viewModel::onQueryChange,
        onSubmit = viewModel::onSearchSubmit,
        onToggleHit = viewModel::onToggleHit,
        onAddSelected = {
            viewModel.onAddSelected()
            onBack()
        },
    )
}

@Composable
fun SearchProductsScreen(
    source: DataSource,
    state: SearchUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onToggleHit: (String) -> Unit,
    onAddSelected: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenVerification: () -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val sourceName = stringResource(source.nameRes)

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            DtTopBar(
                title = stringResource(R.string.search_title),
                subtitle = sourceName,
                navigationIcon = { BackButton(onBack) },
            )
        },
        bottomBar = {
            BottomActionBar {
                val count = state.selectedIds.size
                Button(
                    onClick = onAddSelected,
                    enabled = count > 0,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp),
                ) {
                    Text(
                        if (count > 0) {
                            pluralStringResource(R.plurals.search_add, count, count)
                        } else {
                            stringResource(R.string.search_add_none)
                        }
                    )
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                FieldLabel(stringResource(R.string.search_keyword))
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    placeholder = { Text(stringResource(R.string.search_placeholder, sourceName)) },
                    leadingIcon = {
                        Icon(
                            DtIcons.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(
                                    DtIcons.Close,
                                    contentDescription = stringResource(R.string.search_clear),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, autoCorrectEnabled = false),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboard?.hide()
                        onSubmit()
                    }),
                )
                Spacer(Modifier.height(10.dp))
                if (state.status == SearchStatus.LOADING) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                } else {
                    Spacer(Modifier.height(4.dp))
                }
            }

            when {
                state.status == SearchStatus.IDLE -> EmptyState(
                    icon = DtIcons.Search,
                    title = stringResource(R.string.search_idle_title),
                    message = stringResource(R.string.search_idle_body, sourceName),
                )

                state.status == SearchStatus.FAILED && state.challengeUrl != null -> EmptyState(
                    icon = DtIcons.Alert,
                    title = stringResource(R.string.search_verify_title),
                    message = stringResource(R.string.search_verify_body, sourceName),
                    actionLabel = stringResource(R.string.search_verify_action),
                    onAction = onOpenVerification,
                )

                state.status == SearchStatus.FAILED -> EmptyState(
                    icon = DtIcons.Alert,
                    title = state.error?.let { stringResource(it.labelRes) }
                        ?: stringResource(R.string.search_error_title),
                    message = state.error?.let { stringResource(it.descriptionRes) }
                        ?: stringResource(R.string.search_error_body),
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = onSubmit,
                )

                state.status == SearchStatus.UNSUPPORTED -> EmptyState(
                    icon = DtIcons.SearchX,
                    title = stringResource(R.string.search_unsupported_title),
                    message = stringResource(R.string.search_unsupported_body, sourceName),
                )

                state.status == SearchStatus.LOADING && state.hits.isEmpty() -> Unit

                state.status == SearchStatus.LOADED && state.hits.isEmpty() -> EmptyState(
                    icon = DtIcons.SearchX,
                    title = stringResource(R.string.search_empty_title),
                    message = stringResource(R.string.search_empty_body),
                )

                else -> SearchResultList(
                    hits = state.hits,
                    selectedIds = state.selectedIds,
                    onToggleHit = onToggleHit,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SearchResultList(
    hits: List<SearchHit>,
    selectedIds: Set<String>,
    onToggleHit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item {
            Text(
                text = pluralStringResource(R.plurals.search_result_count, hits.size, hits.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        items(hits, key = { it.productId }) { hit ->
            SearchHitRow(
                hit = hit,
                selected = hit.productId in selectedIds,
                onToggle = { onToggleHit(hit.productId) },
            )
        }
    }
}

@Composable
private fun SearchHitRow(hit: SearchHit, selected: Boolean, onToggle: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() })
                .heightIn(min = 58.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = selected, onCheckedChange = null, modifier = Modifier.padding(12.dp))
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text(hit.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    text = listOfNotNull(productIdLabel(hit.source, hit.productId), hit.publisher)
                        .joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Preview(showBackground = true, heightDp = 760)
@Composable
private fun SearchProductsScreenPreview() {
    DataToolTheme {
        SearchProductsScreen(
            source = DataSource.UNITY_ASSET_STORE,
            state = SearchUiState(
                query = "terrain",
                status = SearchStatus.LOADED,
                hits = SampleData.catalog.take(3),
                selectedIds = setOf("96478"),
            ),
            onBack = {},
            onQueryChange = {},
            onSubmit = {},
            onToggleHit = {},
            onAddSelected = {},
        )
    }
}
