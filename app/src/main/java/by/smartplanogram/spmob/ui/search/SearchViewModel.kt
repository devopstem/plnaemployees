package by.smartplanogram.spmob.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import by.smartplanogram.spmob.SpmobApp
import by.smartplanogram.spmob.data.local.entity.ProductSearchRow
import by.smartplanogram.spmob.data.repo.CatalogRepository
import by.smartplanogram.spmob.data.repo.DepartmentInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val zoneCode: String = "",
    val placeType: String = "",
    val departments: List<DepartmentInfo> = emptyList(),
    val results: List<ProductSearchRow> = emptyList(),
    val matchTotal: Int = 0,
    val catalogTotal: Int = 0,
    val suggestions: List<String> = emptyList(),
    val loading: Boolean = true,
)

class SearchViewModel(
    private val catalog: CatalogRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(SearchUiState())
    val ui: StateFlow<SearchUiState> = _ui.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            SpmobApp.instance.seedReadyFlow.first { it }
            val depts = catalog.departments()
            _ui.update { it.copy(departments = depts) }
            runSearch()
        }
    }

    fun onQueryChange(query: String) {
        _ui.update { it.copy(query = query) }
        scheduleSearch()
    }

    fun clearQuery() {
        onQueryChange("")
    }

    fun onZoneSelected(zoneCode: String) {
        _ui.update { it.copy(zoneCode = zoneCode) }
        scheduleSearch()
    }

    fun onPlaceTypeSelected(placeType: String) {
        _ui.update { it.copy(placeType = placeType) }
        scheduleSearch()
    }

    private fun scheduleSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(180)
            SpmobApp.instance.seedReadyFlow.first { it }
            runSearch()
        }
    }

    private suspend fun runSearch() {
        val state = _ui.value
        val catalogTotal = catalog.productCount()
        val matchTotal = catalog.searchTotal(
            query = state.query,
            zoneCode = state.zoneCode,
            placeType = state.placeType,
        )
        val rows = catalog.search(
            query = state.query,
            zoneCode = state.zoneCode,
            placeType = state.placeType,
        )
        val suggestions = if (state.query.isBlank()) {
            emptyList()
        } else {
            rows.map { it.product.name }.distinct().take(5)
        }
        _ui.update {
            it.copy(
                results = rows,
                matchTotal = matchTotal,
                catalogTotal = catalogTotal,
                suggestions = suggestions,
                loading = false,
            )
        }
    }

    fun remember(row: ProductSearchRow) {
        viewModelScope.launch { catalog.rememberSearch(row) }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SearchViewModel(SpmobApp.instance.catalog) as T
        }
    }
}
