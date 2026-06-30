package com.example.claims.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.claims.android.data.ClaimSummary
import com.example.claims.android.data.ClaimsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ClaimsListViewModel(private val repo: ClaimsRepository) : ViewModel() {

    data class State(
        val claims: List<ClaimSummary> = emptyList(),
        val loading: Boolean = false,
        val error: String? = null,
        val status: String? = null,
        val payer: String? = null,
        val query: String = "",
        val page: Int = 0,
        val hasMore: Boolean = false,
        val payers: List<String> = emptyList()
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()
    private val pageSize = 20

    init { refresh() }

    fun setStatus(s: String?) { _state.update { it.copy(status = s) }; refresh() }
    fun setPayer(p: String?) { _state.update { it.copy(payer = p) }; refresh() }
    fun setQuery(q: String) { _state.update { it.copy(query = q) } }
    fun submitQuery() = refresh()

    fun refresh() = load(reset = true)

    fun loadMore() {
        val s = _state.value
        if (!s.loading && s.hasMore) load(reset = false)
    }

    private fun load(reset: Boolean) {
        val s = _state.value
        val nextPage = if (reset) 0 else s.page + 1
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val resp = repo.list(
                    status = s.status,
                    payer = s.payer,
                    memberId = null,
                    q = s.query.ifBlank { null },
                    page = nextPage,
                    size = pageSize,
                    sort = "createdAt,desc"
                )
                _state.update { cur ->
                    val merged = if (reset) resp.content else cur.claims + resp.content
                    cur.copy(
                        claims = merged,
                        loading = false,
                        page = resp.page.number,
                        hasMore = resp.page.number + 1 < resp.page.totalPages,
                        payers = (cur.payers + merged.map { it.payer }).distinct().sorted()
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Failed to load claims") }
            }
        }
    }

    companion object {
        fun factory(repo: ClaimsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ClaimsListViewModel(repo) as T
            }
    }
}
