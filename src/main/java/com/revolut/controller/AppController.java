package com.revolut.controller;

import com.revolut.client.AppException;
import com.revolut.client.TransferResponse;
import com.revolut.model.AccountModel;
import io.javalin.http.Context;

public interface AppController {
    AccountModel getAccountById(Context ctx) throws Exception;
    AccountModel updateAccountById(Context ctx) throws Exception;
    AccountModel createAccount(Context ctx) throws Exception;
    AccountModel deleteAccountById(Context ctx) throws AppException;

    TransferResponse transferMoney(Context ctx) throws AppException;
    TransferResponse getTransferResultById(Context ctx);
    String transferMoneyAsync(Context ctx);
}
