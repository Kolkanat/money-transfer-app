package com.revolut.controller;

import com.revolut.client.AppException;
import com.revolut.client.TransferResponse;
import com.revolut.model.AccountModel;
import com.revolut.repository.AccountRepository;
import com.revolut.repository.TransferRepository;
import com.revolut.transfer.TransferService;
import io.javalin.http.Context;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Objects;
import java.util.UUID;

import static java.util.UUID.fromString;

@AllArgsConstructor
public class AppControllerImpl implements AppController {

    private TransferService transferService;
    private AccountRepository accountRepository;
    private TransferRepository transferRepository;

    private static final String ID_TO_PARAM = "idTo";
    private static final String AMOUNT_PARAM = "amount";
    private static final String ID_FROM_PARAM = "idFrom";
    private static final String ID_PARAM = "id";
    private static final String FIRST_NAME_PARAM = "firstName";
    private static final String LAST_NAME_PARAM = "lastName";
    private static final String BALANCE_PARAM = "balance";
    private static final Long WAIT_TIME_OUT = 20000l;

    @Override
    public AccountModel getAccountById(Context ctx) throws AppException {
        return accountRepository.getById(fromString(ctx.pathParam(ID_PARAM)));
    }

    @Override
    public AccountModel updateAccountById(Context ctx) throws Exception {
        AccountModel accountModel = accountRepository.getById(fromString(ctx.pathParam(ID_PARAM)));
        accountModel.setFirstName(ctx.formParam(FIRST_NAME_PARAM));
        accountModel.setLastName(ctx.formParam(LAST_NAME_PARAM));
        //we don't update balance directly
        return accountRepository.updateById(accountModel);
    }

    @Override
    public AccountModel createAccount(Context ctx) throws Exception {
        if (!NumberUtils.isParsable(ctx.formParam(BALANCE_PARAM))) {
            throw new AppException("Parameter balance is not correct number.", 400);
        }
        AccountModel accountModel = AccountModel.builder()
                .firstName(ctx.formParam(FIRST_NAME_PARAM))
                .lastName(ctx.formParam(LAST_NAME_PARAM))
                .balance(NumberUtils.toScaledBigDecimal(ctx.formParam(BALANCE_PARAM)))
                .build();

        return accountRepository.create(accountModel);
    }

    @Override
    public AccountModel deleteAccountById(Context ctx) throws AppException {
        return accountRepository.deleteById(fromString(ctx.pathParam(ID_PARAM)));
    }

    @Override
    public TransferResponse transferMoney(Context ctx) throws AppException {
        String amount = ctx.formParam(AMOUNT_PARAM);
        String accountIdTo = ctx.formParam(ID_TO_PARAM);
        String accountIdFrom = ctx.formParam(ID_FROM_PARAM);
        if (Objects.isNull(amount) || Objects.isNull(accountIdFrom) || Objects.isNull(accountIdTo)) {
            throw new AppException("Not proper request", 400);
        }
        UUID transactionId = transferService.queueTransferRequest(accountIdFrom, accountIdTo, amount, false);
        return transferService.waitTransferResponse(transactionId, WAIT_TIME_OUT);
    }

    @Override
    public TransferResponse getTransferResultById(Context ctx) {
        return transferRepository.getTransferResultById(fromString(ctx.pathParam(ID_PARAM)));
    }

    @Override
    public String transferMoneyAsync(Context ctx) {
        String amount = ctx.formParam(AMOUNT_PARAM);
        String accountIdTo = ctx.formParam(ID_TO_PARAM);
        String accountIdFrom = ctx.formParam(ID_FROM_PARAM);
        return transferService.queueTransferRequest(accountIdFrom, accountIdTo, amount, true).toString();
    }

}
