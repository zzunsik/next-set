package io.github.zzunsik.nextset.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.zzunsik.nextset.domain.model.OpenSourceNotice
import io.github.zzunsik.nextset.domain.repository.LicenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 오픈소스 라이선스 목록(SET-03). 읽기 전에는 null이다. */
class LicensesViewModel(licenseRepository: LicenseRepository) : ViewModel() {
    private val _notices = MutableStateFlow<List<OpenSourceNotice>?>(null)
    val notices: StateFlow<List<OpenSourceNotice>?> = _notices.asStateFlow()

    init {
        viewModelScope.launch { _notices.value = licenseRepository.notices() }
    }
}

/** 라이선스 한 건의 저작권 표시와 전문. */
data class LicenseDetailUiState(
    val notice: OpenSourceNotice,
    val text: String,
)

/** 라이선스 한 건(SET-03). 읽기 전에는 null이다. */
class LicenseDetailViewModel(licenseRepository: LicenseRepository, id: String) : ViewModel() {
    private val _uiState = MutableStateFlow<LicenseDetailUiState?>(null)
    val uiState: StateFlow<LicenseDetailUiState?> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val notice = licenseRepository.notices().first { it.id == id }
            _uiState.value = LicenseDetailUiState(notice = notice, text = licenseRepository.licenseText(id))
        }
    }
}
