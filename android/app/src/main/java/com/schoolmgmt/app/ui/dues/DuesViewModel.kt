package com.schoolmgmt.app.ui.dues

import androidx.lifecycle.ViewModel
import com.schoolmgmt.app.data.repository.FeeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DuesViewModel @Inject constructor(
    feeRepository: FeeRepository,
) : ViewModel() {
    /** Every UNPAID/PARTIAL fee across the whole school — see FeeRepository.observeDues. */
    val dues = feeRepository.observeDues()
}
