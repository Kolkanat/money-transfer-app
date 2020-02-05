package com.revolut.repository;

import com.revolut.client.AppException;
import com.revolut.db.AppDb;
import com.revolut.model.AccountModel;
import lombok.AllArgsConstructor;

import java.util.UUID;

import static java.util.Optional.ofNullable;

@AllArgsConstructor
public class AccountRepositoryImpl implements AccountRepository {

    private AppDb accountDb;

    @Override
    public AccountModel getById(UUID id) throws AppException {
        return ofNullable(accountDb.selectAccountById(id))
                .orElseThrow(() -> new AppException("Account not found.", 404));
    }

    @Override
    public AccountModel updateById(AccountModel accountModel) {
        return accountDb.updateAccountById(accountModel);
    }

    @Override
    public AccountModel create(AccountModel accountModel) throws AppException {
        try {
            return accountDb.insertAccount(accountModel);
        } catch (Exception ex) {
            throw new AppException(ex.getMessage(), 400);
        }
    }

    @Override
    public AccountModel deleteById(UUID id) throws AppException {
        try {
            return accountDb.deleteAccountById(id);
        } catch (Exception ex) {
            throw new AppException("Account does not exist.", 404);
        }
    }
}
