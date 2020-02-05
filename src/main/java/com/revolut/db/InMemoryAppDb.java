package com.revolut.db;

import com.revolut.client.TransferResponse;
import com.revolut.model.AccountModel;
import lombok.Getter;
import org.apache.commons.lang3.math.NumberUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import static com.revolut.utils.Utils.appBalanceFormat;
import static java.util.Optional.ofNullable;

public class InMemoryAppDb implements AppDb {
    private final Map<UUID, InMemoryAppDb.Account> accounts = new HashMap<>();
    private final Map<UUID, TransferResponse> responses = new HashMap<>();

    @Override
    public AccountModel selectAccountById(UUID id) {
        return parse(accounts.get(id));
    }

    @Override
    public AccountModel deleteAccountById(UUID id) {
        synchronized (accounts.get(id)) {
            return parse(accounts.remove(id));
        }
    }

    @Override
    public AccountModel insertAccount(AccountModel newAccount) throws Exception {
        if (Objects.nonNull(newAccount)) {
            UUID id = ofNullable(newAccount.getId()).orElse(UUID.randomUUID());
            BigDecimal balance = ofNullable(newAccount.getBalance()).orElse(NumberUtils.toScaledBigDecimal("0.00"));
            if (accounts.containsKey(id)) {
                throw new Exception(String.format("Account with id:%s already exists", id));
            }
            synchronized (accounts) {
                if (accounts.containsKey(id)) {
                    throw new Exception(String.format("Account with id:%s already exists", id));
                } else {
                    accounts.put(id, new Account(id, balance, newAccount.getFirstName(), newAccount.getLastName()));
                    return parse(accounts.get(id));
                }
            }

        }
        throw new Exception("Empty account");
    }

    @Override
    public AccountModel updateAccountById(AccountModel accountModel) {
        ofNullable(accountModel).map(acc -> acc.getId()).map(id -> accounts.get(id)).ifPresent(acc -> {
            synchronized (acc) {
                acc.setFirstName(accountModel.getFirstName());
                acc.setLastName(accountModel.getLastName());
            }
        });
        return parse(accounts.get(accountModel.getId()));
    }

    @Override
    public void insertTransferResult(UUID uuid, TransferResponse transferResponse) {
        responses.put(uuid, transferResponse);
    }

    @Override
    public TransferResponse selectTransferResultById(UUID transactionId) {
        return responses.get(transactionId);
    }

    @Override
    public TransferResponse makeTransfer(UUID idFrom, UUID idTo, BigDecimal amount, ReentrantLock lock) {
        DB_CODE result = null;
        try {
            synchronized (accounts.get(idFrom)) {
                synchronized (accounts.get(idTo)) {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                    result = accounts.get(idFrom).makeTransaction(amount.negate());
                    if (DB_CODE.SUCCESS.equals(result)) {
                        result = accounts.get(idTo).makeTransaction(amount);
                        if (!DB_CODE.SUCCESS.equals(result)) {
                            //rollback
                            accounts.get(idFrom).makeTransaction(amount);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            result = DB_CODE.INTERNAL_ERROR;
            if (Objects.isNull(accounts.get(idFrom))) {
                result = DB_CODE.FROM_ACCOUNT_NOT_FOUND;
            } else if (Objects.isNull(accounts.get(idTo))) {
                result = DB_CODE.TO_ACCOUNT_NOT_FOUND;
            }
        }

        return TransferResponse.builder()
                .idTo(idTo.toString())
                .message(result.message)
                .idFrom(idFrom.toString())
                .amount(appBalanceFormat(amount))
                .transactionDate(new Date())
                .transactionState(result.state)
                .build();
    }

    private AccountModel parse(Account account) {
        return ofNullable(account)
                .map(acc -> AccountModel.builder()
                        .id(acc.getId())
                        .firstName(acc.getFirstName())
                        .lastName(acc.getLastName())
                        .balance(acc.getBalance())
                        .build())
                .orElse(null);
    }

    @Getter
    static class Account {
        private UUID id;
        private BigDecimal balance;
        private String firstName;
        private String lastName;

        public Account(UUID id, BigDecimal amount, String firstName, String lastName) throws Exception {
            if (Objects.isNull(id) || Objects.isNull(amount)) {
                throw new Exception("id and amount cannot be null.");
            } else if (amount.compareTo(BigDecimal.valueOf(0d)) < 0) {
                throw new Exception("amount can not be lower than zero");
            }
            this.id = id;
            this.balance = amount;
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public synchronized DB_CODE makeTransaction(BigDecimal amount) {
            BigDecimal newValue = this.balance.add(amount);
            if (newValue.compareTo(BigDecimal.valueOf(0d)) < 0) {
                return DB_CODE.BALANCE_NOT_ENOUGH;
            }
            this.balance = newValue;
            return DB_CODE.SUCCESS;
        }

        private void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        private void setLastName(String lastName) {
            this.lastName = lastName;
        }

    }

    enum DB_CODE {
        BALANCE_NOT_ENOUGH("FAILED", "Balance is not enough."),
        SUCCESS("SUCCESS", "Success transaction."),
        FROM_ACCOUNT_NOT_FOUND("FAILED", "The account from which the transfer is made does not exist."),
        TO_ACCOUNT_NOT_FOUND("FAILED", "The account to which the transfer is made does not exist."),
        INTERNAL_ERROR("FAILED", "Internal Error.")
        ;

        private String state;
        private String message;

        DB_CODE(String state, String message) {
            this.state = state;
            this.message = message;
        }
    }
}
