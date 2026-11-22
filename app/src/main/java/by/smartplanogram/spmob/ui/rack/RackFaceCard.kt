package by.smartplanogram.spmob.ui.rack

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import by.smartplanogram.spmob.R
import by.smartplanogram.spmob.ui.product.productIcon
import kotlinx.coroutines.delay

/** Edge-to-edge vertical shelf stack — one continuous rack face. */
@Composable
fun RackFaceCard(
    rack: RackCarouselItem,
    modifier: Modifier = Modifier,
    onProductClick: (ShelfProductUi, shelfNumber: Int) -> Unit = { _, _ -> },
    onTargetShelfReached: () -> Unit = {},
) {
    val listState = rememberLazyListState()
    val bg = MaterialTheme.colorScheme.background
    val shelfBoard = MaterialTheme.colorScheme.surfaceVariant
    val accent = MaterialTheme.colorScheme.secondary
    val productFill = MaterialTheme.colorScheme.primaryContainer
    val productHighlight = MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)

    // Scroll to target shelf + pulse, then haptic callback
    LaunchedEffect(rack.id, rack.targetShelf, rack.shelfRows.size) {
        val target = rack.targetShelf ?: return@LaunchedEffect
        val index = rack.shelfRows.indexOfFirst { it.number == target }
        if (index < 0) return@LaunchedEffect
        delay(280)
        listState.animateScrollToItem(index)
        delay(120)
        onTargetShelfReached()
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 48.dp, top = 8.dp),
        ) {
            itemsIndexed(
                items = rack.shelfRows,
                key = { _, shelf -> "${rack.id}-${shelf.number}" },
            ) { index, shelf ->
                val isTargetShelf = rack.targetShelf == shelf.number
                ShelfRow(
                    shelf = shelf,
                    isTargetShelf = isTargetShelf,
                    shelfBoard = shelfBoard,
                    accent = accent,
                    productFill = productFill,
                    productHighlight = productHighlight,
                    showTopEdge = index == 0,
                    onProductClick = onProductClick,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(colors = listOf(bg, Color.Transparent)),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(colors = listOf(Color.Transparent, bg)),
                ),
        )

        ShelfScrollIndicator(
            listState = listState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
                .width(3.dp)
                .fillMaxHeight(0.35f),
        )
    }
}

@Composable
private fun ShelfRow(
    shelf: ShelfUi,
    isTargetShelf: Boolean,
    shelfBoard: Color,
    accent: Color,
    productFill: Color,
    productHighlight: Color,
    showTopEdge: Boolean,
    onProductClick: (ShelfProductUi, shelfNumber: Int) -> Unit,
) {
    val pulse by animateFloatAsState(
        targetValue = if (isTargetShelf) 1.04f else 1f,
        animationSpec = tween(450),
        label = "shelfPulse",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(pulse),
    ) {
        if (showTopEdge) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(accent.copy(alpha = 0.55f)),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .background(
                    if (isTargetShelf) accent.copy(alpha = 0.14f) else Color.Transparent,
                )
                .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = stringResource(R.string.scanner_shelf, shelf.number),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isTargetShelf) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .width(64.dp)
                    .padding(bottom = 10.dp),
            )
            ProductPlaceholders(
                shelf = shelf,
                productFill = productFill,
                productHighlight = productHighlight,
                onProductClick = onProductClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(shelfBoard),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(accent.copy(alpha = 0.35f)),
        )
    }
}

@Composable
private fun ProductPlaceholders(
    shelf: ShelfUi,
    productFill: Color,
    productHighlight: Color,
    onProductClick: (ShelfProductUi, shelfNumber: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val slots: List<ShelfProductUi?> = remember(shelf) {
        if (shelf.products.isNotEmpty()) {
            shelf.products
        } else {
            List(4) { null }
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        slots.forEachIndexed { index, product ->
            val heightFrac = when (index % 4) {
                0 -> 0.92f
                1 -> 0.72f
                2 -> 0.84f
                else -> 0.64f
            }
            val widthDp = when (index % 3) {
                0 -> 28.dp
                1 -> 36.dp
                else -> 22.dp
            }
            val fill = when {
                product?.highlighted == true -> productHighlight
                product != null -> productFill
                else -> productFill.copy(alpha = 0.45f)
            }
            Box(
                modifier = Modifier
                    .width(widthDp)
                    .fillMaxHeight(heightFrac)
                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                    .background(fill)
                    .then(
                        if (product != null) {
                            Modifier.clickable {
                                onProductClick(product, shelf.number)
                            }
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (product != null) {
                    Icon(
                        imageVector = productIcon(
                            productName = product.name,
                            category = product.category,
                            iconHint = product.iconHint,
                        ),
                        contentDescription = product.name,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.95f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ShelfScrollIndicator(
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    var trackHeightPx by remember { mutableIntStateOf(0) }
    val progress by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            if (total <= 1) return@derivedStateOf 0f
            val visible = info.visibleItemsInfo
            if (visible.isEmpty()) return@derivedStateOf 0f
            visible.first().index.toFloat() / (total - 1).coerceAtLeast(1)
        }
    }
    Box(
        modifier = modifier.onSizeChanged { trackHeightPx = it.height },
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)),
        )
        val thumbFraction = 0.28f
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(thumbFraction)
                .graphicsLayer {
                    val travel = trackHeightPx * (1f - thumbFraction)
                    translationY = progress * travel
                }
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)),
        )
    }
}
