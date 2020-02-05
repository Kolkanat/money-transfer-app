package com.revolut;

import com.revolut.client.AppException;
import com.revolut.controller.AppControllerImpl;
import com.revolut.model.AccountModel;
import com.revolut.repository.AccountRepositoryImpl;
import com.revolut.repository.TransferRepositoryImpl;
import com.revolut.transfer.TransferServiceImpl;
import org.junit.Test;

import io.javalin.http.Context;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class AppControllerImplTest {

    @Test
    public void getAccountByIdTest() throws Exception {
        Context ctx = mock(Context.class);
        AccountRepositoryImpl repository = mock(AccountRepositoryImpl.class);
        AppControllerImpl appController = new AppControllerImpl(null, repository, null);

        UUID uuid = UUID.randomUUID();
        when(ctx.pathParam("id")).thenReturn(uuid.toString());

        appController.getAccountById(ctx);

        verify(ctx, times(1)).pathParam("id");
        verify(repository, times(1)).getById(uuid);

    }

    @Test
    public void updateAccountByIdTest() throws Exception {
        Context ctx = mock(Context.class);
        AccountRepositoryImpl repository = mock(AccountRepositoryImpl.class);
        AppControllerImpl appController = new AppControllerImpl(null, repository, null);

        UUID uuid = UUID.randomUUID();
        AccountModel account = buildAccount(uuid);
        when(ctx.pathParam("id")).thenReturn(uuid.toString());
        when(ctx.formParam("firstName")).thenReturn("updatedFirstName");
        when(ctx.formParam("lastName")).thenReturn("updatedLastName");
        when(repository.getById(uuid)).thenReturn(account);
        appController.updateAccountById(ctx);

        assertEquals(uuid, account.getId());
        assertEquals("updatedFirstName", account.getFirstName());
        assertEquals("updatedLastName", account.getLastName());
        assertEquals(BigDecimal.valueOf(1000d), account.getBalance());

        verify(ctx, times(1)).pathParam("id");
        verify(ctx, times(1)).formParam("firstName");
        verify(ctx, times(1)).formParam("lastName");
        verify(repository, times(1)).getById(uuid);
        verify(repository, times(1)).updateById(account);

    }

    @Test
    public void createAccountTest() throws Exception {
        Context ctx = mock(Context.class);
        AccountRepositoryImpl repository = mock(AccountRepositoryImpl.class);
        AppControllerImpl appController = new AppControllerImpl(null, repository, null);

        when(ctx.formParam("firstName")).thenReturn("updatedFirstName");
        when(ctx.formParam("lastName")).thenReturn("updatedLastName");
        when(ctx.formParam("balance")).thenReturn("1653");

        AccountModel account = AccountModel.builder()
                .firstName("updatedFirstName")
                .lastName("updatedLastName")
                .balance(new BigDecimal("1653.00"))
                .build();

        appController.createAccount(ctx);

        verify(ctx, times(1)).formParam("firstName");
        verify(ctx, times(1)).formParam("lastName");
        verify(ctx, times(2)).formParam("balance");
        verify(repository, times(1)).create(account);

    }

    @Test
    public void deleteAccountByIdTest() throws Exception {
        Context ctx = mock(Context.class);
        AccountRepositoryImpl repository = mock(AccountRepositoryImpl.class);
        AppControllerImpl appController = new AppControllerImpl(null, repository, null);

        UUID uuid = UUID.randomUUID();
        when(ctx.pathParam("id")).thenReturn(uuid.toString());

        appController.deleteAccountById(ctx);

        verify(ctx, times(1)).pathParam("id");
        verify(repository, times(1)).deleteById(uuid);

    }

    @Test
    public void transferMoneyTest() throws Exception {
        Context ctx = mock(Context.class);
        TransferServiceImpl transferService = mock(TransferServiceImpl.class);
        AppControllerImpl appController = new AppControllerImpl(transferService, null, null);

        UUID idTo = UUID.randomUUID();
        UUID idFrom = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        when(ctx.formParam("amount")).thenReturn("1000");
        when(ctx.formParam("idTo")).thenReturn(idTo.toString());
        when(ctx.formParam("idFrom")).thenReturn(idFrom.toString());
        when(transferService.queueTransferRequest(idFrom.toString(), idTo.toString(), "1000", false))
                .thenReturn(transactionId);

        appController.transferMoney(ctx);

        verify(ctx, times(1)).formParam("amount");
        verify(ctx, times(1)).formParam("idTo");
        verify(ctx, times(1)).formParam("idFrom");
        verify(transferService, times(1))
                .queueTransferRequest(idFrom.toString(), idTo.toString(), "1000", false);
        verify(transferService, times(1)).waitTransferResponse(transactionId, 20000l);
    }

    @Test(expected = AppException.class)
    public void TransferMoneyThrowsExceptionAmountTest() throws Exception {
        Context ctx = mock(Context.class);
        AppControllerImpl appController = new AppControllerImpl(null, null, null);

        UUID idTo = UUID.randomUUID();
        UUID idFrom = UUID.randomUUID();

        when(ctx.formParam("amount")).thenReturn(null);
        when(ctx.formParam("idTo")).thenReturn(idTo.toString());
        when(ctx.formParam("idFrom")).thenReturn(idFrom.toString());

        appController.transferMoney(ctx);
    }

    @Test(expected = AppException.class)
    public void TransferMoneyThrowsExceptionIdFromTest() throws Exception {
        Context ctx = mock(Context.class);
        AppControllerImpl appController = new AppControllerImpl(null, null, null);

        UUID idTo = UUID.randomUUID();

        when(ctx.formParam("amount")).thenReturn("1000");
        when(ctx.formParam("idTo")).thenReturn(idTo.toString());
        when(ctx.formParam("idFrom")).thenReturn(null);

        appController.transferMoney(ctx);
    }

    @Test(expected = AppException.class)
    public void TransferMoneyThrowsExceptionIdToTest() throws Exception {
        Context ctx = mock(Context.class);
        AppControllerImpl appController = new AppControllerImpl(null, null, null);

        UUID idFrom = UUID.randomUUID();

        when(ctx.formParam("amount")).thenReturn("1000");
        when(ctx.formParam("idTo")).thenReturn(null);
        when(ctx.formParam("idFrom")).thenReturn(idFrom.toString());

        appController.transferMoney(ctx);
    }

    @Test
    public void TransferMoneyAsyncTest() throws Exception {
        Context ctx = mock(Context.class);
        TransferServiceImpl transferService = mock(TransferServiceImpl.class);
        AppControllerImpl appController = new AppControllerImpl(transferService, null, null);

        UUID idTo = UUID.randomUUID();
        UUID idFrom = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        when(ctx.formParam("amount")).thenReturn("1000");
        when(ctx.formParam("idTo")).thenReturn(idTo.toString());
        when(ctx.formParam("idFrom")).thenReturn(idFrom.toString());
        when(transferService.queueTransferRequest(idFrom.toString(), idTo.toString(), "1000", true))
                .thenReturn(transactionId);

        appController.transferMoneyAsync(ctx);

        verify(ctx, times(1)).formParam("amount");
        verify(ctx, times(1)).formParam("idTo");
        verify(ctx, times(1)).formParam("idFrom");
        verify(transferService, times(1))
                .queueTransferRequest(idFrom.toString(), idTo.toString(), "1000", true);

    }

    @Test
    public void getTransferResultByIdTest() {
        Context ctx = mock(Context.class);
        TransferRepositoryImpl repository = mock(TransferRepositoryImpl.class);
        AppControllerImpl appController = new AppControllerImpl(null, null, repository);

        UUID id = UUID.randomUUID();
        when(ctx.pathParam("id")).thenReturn(id.toString());

        appController.getTransferResultById(ctx);

        verify(ctx, times(1)).pathParam("id");
        verify(repository, times(1)).getTransferResultById(id);

    }

    public static AccountModel buildAccount(UUID id) {
        return  AccountModel.builder()
                .id(id)
                .firstName("fn")
                .lastName("ln")
                .balance(BigDecimal.valueOf(1000d))
                .build();
    }
}
