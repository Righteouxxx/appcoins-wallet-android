package com.asfoundation.wallet.repository

import android.content.SharedPreferences
import com.asfoundation.wallet.repository.entity.TransactionEntity
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single

class TransactionsLocalRepository(private val transactionsDao: TransactionsDao,
                                  private val sharedPreferences: SharedPreferences) :
    TransactionsRepository {

  companion object {
    private const val OLD_TRANSACTIONS_LOAD = "IS_OLD_TRANSACTIONS_LOADED"
    private const val LAST_LOCALE = "locale"
  }

  override fun getAllAsFlowable(relatedWallet: String): Flowable<List<TransactionEntity>> {
    return transactionsDao.getAllAsFlowable(relatedWallet)
  }

  override fun insertAll(roomTransactions: List<TransactionEntity>) {
    return transactionsDao.insertAll(roomTransactions)
  }

  override fun getNewestTransaction(relatedWallet: String): Maybe<TransactionEntity> {
    return transactionsDao.getNewestTransaction(relatedWallet)

  }

  override fun getOlderTransaction(relatedWallet: String): Maybe<TransactionEntity> {
    return transactionsDao.getOlderTransaction(relatedWallet)
  }


  override fun isOldTransactionsLoaded(): Single<Boolean> {
    return Single.fromCallable { sharedPreferences.getBoolean(OLD_TRANSACTIONS_LOAD, false) }
  }

  override fun deleteAllTransactions() = transactionsDao.deleteAllTransactions()

  override fun oldTransactionsLoaded() {
    sharedPreferences.edit()
        .putBoolean(OLD_TRANSACTIONS_LOAD, true)
        .apply()
  }

  override fun setLocale(locale: String) {
    sharedPreferences.edit()
        .putString(LAST_LOCALE, locale)
        .apply()
  }

  override fun getLastLocale() = sharedPreferences.getString(LAST_LOCALE, null)
}