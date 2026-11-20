package by.smartplanogram.spmob.ui.rack

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.smartplanogram.spmob.R
import by.smartplanogram.spmob.SpmobApp
import by.smartplanogram.spmob.ui.departments.departmentTitle

/**
 * Full-screen planogram: sticky rack header + edge-to-edge vertical shelves.
 * Horizontal pager switches racks; vertical scroll walks the shelf stack.
 */
@Composable
fun RackCarouselScreen(
    viewModel: RackCarouselViewModel,
    onClose: () -> Unit,
    titleOverride: String? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current
    var selectedProduct by remember { mutableStateOf<ProductDetailUi?>(null) }

    BackHandler(enabled = selectedProduct != null) {
        selectedProduct = null
    }
    BackHandler(enabled = selectedProduct == null) {
        onClose()
    }

    val lidProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        lidProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f * lidProgress.value)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val t = lidProgress.value
                    translationY = (1f - t) * size.height * 0.4f
                    alpha = 0.4f + 0.6f * t
                }
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null -> {
                    StickyRackHeader(
                        rackCode = titleOverride
                            ?: stringResource(R.string.departments_planogram_title),
                        shelvesCount = null,
                        productName = null,
                        pageHint = null,
                        onClose = onClose,
                    )
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = state.error.orEmpty(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                }
                state.racks.isEmpty() -> {
                    StickyRackHeader(
                        rackCode = titleOverride
                            ?: stringResource(R.string.departments_planogram_title),
                        shelvesCount = null,
                        productName = null,
                        pageHint = null,
                        onClose = onClose,
                    )
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.departments_racks_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    val pagerState = rememberPagerState(
                        initialPage = state.targetIndex.coerceIn(0, state.racks.lastIndex),
                        pageCount = { state.racks.size },
                    )

                    LaunchedEffect(state.targetIndex, state.racks.size, state.targetRackId) {
                        val page = state.targetIndex.coerceIn(0, state.racks.lastIndex)
                        if (pagerState.currentPage != page) {
                            pagerState.animateScrollToPage(page)
                        }
                    }

                    val current = state.racks.getOrNull(pagerState.currentPage)
                    LaunchedEffect(current?.id) {
                        val rack = current ?: return@LaunchedEffect
                        SpmobApp.instance.settings.rememberRack(
                            id = rack.id,
                            code = rack.code,
                            departmentId = state.departmentId,
                        )
                    }

                    StickyRackHeader(
                        rackCode = current?.code
                            ?: titleOverride
                            ?: stringResource(R.string.departments_planogram_title),
                        shelvesCount = current?.shelves,
                        productName = state.productName.takeIf { it.isNotBlank() },
                        pageHint = if (state.racks.size > 1) {
                            "${pagerState.currentPage + 1} / ${state.racks.size}"
                        } else {
                            null
                        },
                        onClose = onClose,
                    )

                    val deptLabel = if (state.departmentId.isNotBlank()) {
                        departmentTitle(state.departmentId)
                    } else {
                        null
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) { page ->
                        val rack = state.racks[page]
                        RackFaceCard(
                            rack = rack,
                            modifier = Modifier.fillMaxSize(),
                            onProductClick = { product, shelfNo ->
                                view.performHapticFeedback(
                                    HapticFeedbackConstants.KEYBOARD_TAP,
                                )
                                selectedProduct = ProductDetailUi(
                                    name = product.name,
                                    barcode = product.barcode,
                                    category = product.category.takeIf { it.isNotBlank() },
                                    iconHint = product.iconHint,
                                    rackCode = rack.code,
                                    shelf = shelfNo,
                                    departmentTitle = deptLabel,
                                )
                            },
                            onTargetShelfReached = {
                                view.performHapticFeedback(
                                    HapticFeedbackConstants.CONTEXT_CLICK,
                                )
                            },
                        )
                    }
                }
            }
        }

        selectedProduct?.let { detail ->
            ProductDetailSheet(
                detail = detail,
                onDismiss = { selectedProduct = null },
            )
        }
    }
}

@Composable
private fun StickyRackHeader(
    rackCode: String,
    shelvesCount: Int?,
    productName: String?,
    pageHint: String?,
    onClose: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f),
                        shape = CircleShape,
                    ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp, end = 8.dp),
            ) {
                Text(
                    text = rackCode,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = buildString {
                    if (shelvesCount != null) {
                        append(
                            stringResource(R.string.departments_shelves_count, shelvesCount),
                        )
                    }
                    if (!pageHint.isNullOrBlank()) {
                        if (isNotEmpty()) append("  ·  ")
                        append(pageHint)
                    }
                }
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!productName.isNullOrBlank()) {
                    Text(
                        text = productName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
