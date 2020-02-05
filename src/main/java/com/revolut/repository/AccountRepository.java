package com.revolut.repository;

import com.revolut.client.AppException;
import com.revolut.model.AccountModel;

import java.util.UUID;

public interface AccountRepository {
    AccountModel getById(UUID fromString) throws AppException;
    AccountModel updateById(AccountModel accountModel);
    AccountModel create(AccountModel accountModel) throws AppException;
    AccountModel deleteById(UUID fromString) throws AppException;
}
