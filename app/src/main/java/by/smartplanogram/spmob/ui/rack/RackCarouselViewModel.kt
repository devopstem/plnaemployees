package by.smartplanogram.spmob.ui.rack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import by.smartplanogram.spmob.data.local.AppDatabase
import by.smartplanogram.spmob.data.local.entity.RackEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShelfProductUi(
    val barcode: String,
    val name: String,
    val category: String = "",
    val iconHint: String? = null,
    val highlighted: Boolean = false,
)

data class ShelfUi(
    /** 1-based shelf number as stored in the catalog (1 = bottom). */
    val number: Int,
    val products: List<ShelfProductUi>,
)

data class RackCarouselItem(
    val id: String,
    val code: String,
    val shelves: Int,
    val isTarget: Boolean,
    val targetShelf: Int? = null,
    val shelfRows: List<ShelfUi> = emptyList(),
)

data class RackCarouselUiState(
    val racks: List<RackCarouselItem> = emptyList(),
    val targetIndex: Int = 0,
    val targetRackId: String = "",
    val departmentId: String = "",
    val productName: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
)

class RackCarouselViewModel(
    private val db: AppDatabase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RackCarouselUiState())
    val uiState: StateFlow<RackCarouselUiState> = _uiState.asStateFlow()

    fun loadRacks(
        departmentId: String,
        targetRackId: String,
        storeId: String? = null,
        targetShelf: Int? = null,
        productName: String = "",
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    departmentId = departmentId,
                    targetRackId = targetRackId,
                    productName = productName,
                )
            }
            runCatching {
                val resolvedStoreId = storeId
                    ?: db.stores().first()?.id
                    ?: error("Магазин не выбран")

                val racks: List<RackEntity> = db.racks()
                    .byDepartment(resolvedStoreId, departmentId)
                    .ifEmpty {
                        val target = db.racks().byId(targetRackId)
                        if (target != null) {
                            db.racks().byDepartment(resolvedStoreId, target.departmentId)
                        } else {
                            emptyList()
                        }
                    }

                check(racks.isNotEmpty()) { "В отделе нет стеллажей" }

                val targetIndex = racks.indexOfFirst { it.id == targetRackId }
                    .takeIf { it >= 0 }
                    ?: 0

                val items = racks.map { rack ->
                    val isTarget = rack.id == targetRackId
                    val highlightShelf = if (isTarget) targetShelf else null
                    val shelfRows = buildShelfRows(
                        storeId = resolvedStoreId,
                        rack = rack,
                        highlightShelf = highlightShelf,
                        productName = if (isTarget) productName else "",
                    )
                    RackCarouselItem(
                        id = rack.id,
                        code = rack.code,
                        shelves = rack.shelves,
                        isTarget = isTarget,
                        targetShelf = highlightShelf,
                        shelfRows = shelfRows,
                    )
                }

                _uiState.update {
                    it.copy(
                        racks = items,
                        targetIndex = targetIndex,
                        isLoading = false,
                        error = null,
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Не удалось загрузить стеллажи",
                    )
                }
            }
        }
    }

    private suspend fun buildShelfRows(
        storeId: String,
        rack: RackEntity,
        highlightShelf: Int?,
        productName: String,
    ): List<ShelfUi> {
        val shelfCount = rack.shelves.coerceAtLeast(1)
        val locations = db.locations().forRack(storeId, rack.id)
        val byShelf = locations.groupBy { it.shelf }

        // Visual top → bottom: highest shelf number first (shelf 1 = bottom).
        return (shelfCount downTo 1).map { shelfNo ->
            val products = byShelf[shelfNo].orEmpty().map { loc ->
                val row = db.products().byBarcode(storeId, loc.barcode)
                val name = row?.product?.name ?: loc.barcode
                val category = row?.product?.category.orEmpty()
                val iconHint = row?.product?.iconHint
                val highlighted = highlightShelf == shelfNo &&
                    (productName.isBlank() || name.contains(productName, ignoreCase = true))
                ShelfProductUi(
                    barcode = loc.barcode,
                    name = name,
                    category = category,
                    iconHint = iconHint,
                    highlighted = highlighted,
                )
            }
            ShelfUi(number = shelfNo, products = products)
        }
    }

    class Factory(
        private val db: AppDatabase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(RackCarouselViewModel::class.java))
            return RackCarouselViewModel(db) as T
        }
    }
}
