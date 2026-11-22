package by.smartplanogram.spmob.ui.search

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import by.smartplanogram.spmob.R
import by.smartplanogram.spmob.SpmobApp
import by.smartplanogram.spmob.data.local.entity.ProductSearchRow
import by.smartplanogram.spmob.ui.departments.departmentTitle
import by.smartplanogram.spmob.ui.product.ProductTitleText
import by.smartplanogram.spmob.ui.product.productIcon
import by.smartplanogram.spmob.ui.rack.RackCarouselActivity
import by.smartplanogram.spmob.ui.scanner.ScannerActivity
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SearchScreen(
    wide: Boolean = false,
    viewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory()),
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val layoutMaxWidth = maxWidth
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (wide || layoutMaxWidth >= 600.dp) 40.dp else 16.dp)
                .padding(top = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.search_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { context.startActivity(ScannerActivity.intent(context)) },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QrCodeScanner,
                        contentDescription = stringResource(R.string.scanner_title),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            Spacer(Modifier.size(12.dp))
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearQuery) {
                            Icon(Icons.Outlined.Clear, contentDescription = null)
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
            )

            AnimatedVisibility(
                visible = state.suggestions.isNotEmpty(),
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut(),
            ) {
                Column {
                    Spacer(Modifier.size(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            state.suggestions.forEach { name ->
                                ProductTitleText(
                                    text = name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.onQueryChange(name) }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.size(10.dp))
            Text(
                text = stringResource(R.string.search_place_filter),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FadingChipRow {
                item {
                    PlaceChip(
                        selected = state.placeType.isEmpty(),
                        label = stringResource(R.string.search_chip_all),
                        onClick = { viewModel.onPlaceTypeSelected("") },
                    )
                }
                item {
                    PlaceChip(
                        selected = state.placeType == "rack",
                        label = stringResource(R.string.search_place_rack),
                        onClick = { viewModel.onPlaceTypeSelected("rack") },
                    )
                }
                item {
                    PlaceChip(
                        selected = state.placeType == "cooler",
                        label = stringResource(R.string.search_place_cooler),
                        onClick = { viewModel.onPlaceTypeSelected("cooler") },
                    )
                }
                item {
                    PlaceChip(
                        selected = state.placeType == "stand",
                        label = stringResource(R.string.search_place_stand),
                        onClick = { viewModel.onPlaceTypeSelected("stand") },
                    )
                }
            }

            Text(
                text = stringResource(R.string.search_dept_filter),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FadingChipRow {
                item {
                    PlaceChip(
                        selected = state.zoneCode.isEmpty(),
                        label = stringResource(R.string.search_chip_all),
                        onClick = { viewModel.onZoneSelected("") },
                    )
                }
                items(state.departments, key = { it.id }) { dept ->
                    PlaceChip(
                        selected = state.zoneCode == dept.id,
                        label = departmentTitle(dept.id),
                        onClick = { viewModel.onZoneSelected(dept.id) },
                    )
                }
            }

            Text(
                text = if (state.catalogTotal > 0) {
                    stringResource(
                        R.string.search_found_of_catalog,
                        state.matchTotal,
                        state.catalogTotal,
                    )
                } else {
                    stringResource(R.string.search_found, state.matchTotal)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
            )

            if (state.results.isEmpty() && !state.loading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.search_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(
                        items = state.results,
                        key = { it.product.barcode },
                    ) { row ->
                        ProductResultCard(
                            row = row,
                            onOpen = {
                                viewModel.remember(row)
                                scope.launch {
                                    openProductPlanogram(
                                        context = context,
                                        row = row,
                                        onNoLocation = {
                                            Toast.makeText(
                                                context,
                                                R.string.search_no_location,
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        },
                                    )
                                }
                            },
                            modifier = Modifier.animateItem(
                                placementSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                ),
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FadingChipRow(
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    val bg = MaterialTheme.colorScheme.background
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        LazyRow(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 28.dp, top = 4.dp, bottom = 4.dp),
            content = content,
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(36.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, bg),
                    ),
                ),
        )
    }
}

@Composable
private fun PlaceChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondary,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
        ),
    )
}

@Composable
private fun ProductResultCard(
    row: ProductSearchRow,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onOpen,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = productIcon(
                    productName = row.product.name,
                    category = row.product.category,
                    iconHint = row.product.iconHint,
                ),
                contentDescription = row.product.name,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                ProductTitleText(
                    text = row.product.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                )
                Text(
                    text = row.product.barcode,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (row.product.category.isNotBlank()) {
                    Text(
                        text = row.product.category,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private suspend fun openProductPlanogram(
    context: android.content.Context,
    row: ProductSearchRow,
    onNoLocation: () -> Unit,
) {
    if (!row.hasLocation || row.rackId.isNullOrBlank()) {
        onNoLocation()
        return
    }
    val departmentId = withContext(Dispatchers.IO) {
        row.zoneCode?.takeIf { it.isNotBlank() }
            ?: SpmobApp.instance.db.racks().byId(row.rackId)?.departmentId
    } ?: return
    val storeId = withContext(Dispatchers.IO) {
        SpmobApp.instance.catalog.currentStore()?.id
    }
    context.startActivity(
        RackCarouselActivity.intent(
            context = context,
            departmentId = departmentId,
            targetRackId = row.rackId,
            storeId = storeId,
            targetShelf = row.shelf,
            productName = row.product.name,
        ),
    )
}
