package com.revolut.db;

import com.revolut.client.TransferResponse;
import com.revolut.model.AccountModel;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public interface AppDb {
    AccountModel selectAccountById(UUID id);
    AccountModel deleteAccountById(UUID id);
    AccountModel insertAccount(AccountModel newAccount) throws Exception;
    AccountModel updateAccountById(AccountModel account);
    TransferResponse selectTransferResultById(UUID transactionId);
    TransferResponse makeTransfer(UUID idFrom, UUID idTo, BigDecimal amount, ReentrantLock lock);

    void insertTransferResult(UUID uuid, TransferResponse transferResponse);
}
