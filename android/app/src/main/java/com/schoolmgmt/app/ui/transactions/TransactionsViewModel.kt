package com.schoolmgmt.app.ui.transactions

import androidx.lifecycle.ViewModel
import com.schoolmgmt.app.data.repository.FeeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    feeRepository: FeeRepository,
) : ViewModel() {
    /** The full transaction ledger (#7) — see FeeRepository.observeTransactions. */
    val transactions = feeRepository.observeTransactions()
}
