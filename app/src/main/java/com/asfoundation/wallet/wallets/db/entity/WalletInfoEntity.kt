package com.asfoundation.wallet.wallets.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.math.BigInteger

@Entity
data class WalletInfoEntity(
  @PrimaryKey val wallet: String,
  val appcCreditsBalanceWei: BigInteger,
  val appcBalanceWei: BigInteger,
  val ethBalanceWei: BigInteger,
  val blocked: Boolean,
  val verified: Boolean,
  val logging: Boolean,
  // Populated from exchanges conversion
  val appcCreditsBalanceFiat: BigDecimal?,
  val appcBalanceFiat: BigDecimal?,
  val ethBalanceFiat: BigDecimal?,
  val fiatCurrency: String?,
  val fiatSymbol: String?,
)