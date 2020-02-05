package com.revolut.utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class Utils {
    private static String balanceFormat = "######.00";

    public static String appBalanceFormat(BigDecimal balance) {
        return new DecimalFormat(balanceFormat).format(balance);
    }
}
