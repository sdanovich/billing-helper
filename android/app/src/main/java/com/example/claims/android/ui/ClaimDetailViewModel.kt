package com.example.claims.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.claims.android.data.ClaimDetail
import com.example.claims.android.data.ClaimsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ClaimDetailViewModel(
    private val repo: ClaimsRepository,
    private val claimId: String
) : ViewModel() {

    data class State(
        val claim: ClaimDetail? = null,
        val loading: Boolean = true,
        val error: String? = null
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = State(loading = true)
        viewModelScope.launch {
            try {
                _state.value = State(claim = repo.detail(claimId), loading = false)
            } catch (e: Exception) {
                _state.value = State(loading = false, error = e.message ?: "Failed to load claim")
            }
        }
    }

    companion object {
        fun factory(repo: ClaimsRepository, claimId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ClaimDetailViewModel(repo, claimId) as T
            }
    }
}
