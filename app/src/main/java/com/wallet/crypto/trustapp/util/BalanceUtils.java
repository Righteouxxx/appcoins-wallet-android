package com.wallet.crypto.trustapp.util;

import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

public class BalanceUtils {
    private static String WEI_IN_ETH = "1000000000000000000";

    public static BigDecimal weiToEth(BigDecimal wei) {
        return Convert.fromWei(wei, Convert.Unit.ETHER);
    }

    public static String ethToUsd(String priceUsd, String ethBalance) {
        BigDecimal usd = new BigDecimal(ethBalance).multiply(new BigDecimal(priceUsd));
        usd = usd.setScale(2, RoundingMode.HALF_UP);
        return usd.toString();
    }

    public static BigDecimal EthToWei(String eth) throws Exception {
        return new BigDecimal(eth).multiply(new BigDecimal(WEI_IN_ETH));
    }

    public static BigDecimal weiToGweiBI(BigInteger wei) {
        return Convert.fromWei(new BigDecimal(wei), Convert.Unit.GWEI);
    }

    public static String weiToGwei(BigDecimal wei) {
        return Convert.fromWei(wei, Convert.Unit.GWEI).toPlainString();
    }

    public static BigInteger gweiToWei(BigDecimal gwei) {
        return Convert.toWei(gwei, Convert.Unit.GWEI).toBigInteger();
    }

    /**
     * Base - taken to mean default unit for a currency e.g. ETH, DOLLARS
     * Subunit - taken to mean subdivision of base e.g. WEI, CENTS
     *
     * @param baseAmount - decimal amount in base unit of a given currency
     * @param decimals - decimal places used to convert to subunits
     * @return amount in subunits
     */
    public static BigDecimal baseToSubunit(BigDecimal baseAmount, int decimals) {
        assert(decimals >= 0);
        return baseAmount.multiply(BigDecimal.valueOf(10).pow(decimals));
    }

    /**
     * @param subunitAmount - amouunt in subunits
     * @param decimals - decimal places used to convert subunits to base
     * @return amount in base units
     */
    public static BigDecimal subunitToBase(BigDecimal subunitAmount, int decimals) {
        return subunitAmount.divide(BigDecimal.valueOf(10).pow(decimals));
    }
}
