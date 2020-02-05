package com.revolut;

import com.revolut.client.AppException;
import com.revolut.db.AppDb;
import com.revolut.model.AccountModel;
import com.revolut.repository.AccountRepository;
import com.revolut.repository.AccountRepositoryImpl;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class AccountRepositoryImplTest {
    @Test
    public void getByIdTest() throws Exception {
        UUID id = UUID.randomUUID();
        AccountModel account = buildAccount(id);
        AppDb db = mock(AppDb.class);
        when(db.selectAccountById(id)).thenReturn(account);
        AccountRepository repository = new AccountRepositoryImpl(db);
        AccountModel mockAccount = repository.getById(id);

        assertNotNull(mockAccount);
        assertEquals(account.getId(), mockAccount.getId());
        assertEquals(account.getFirstName(), mockAccount.getFirstName());
        assertEquals(account.getLastName(), mockAccount.getLastName());
        assertEquals(account.getBalance(), mockAccount.getBalance());
        verify(db, times(1)).selectAccountById(id);
    }

    @Test(expected = AppException.class)
    public void getByIdThrowsExceptionTest() throws Exception {
        AppDb db = mock(AppDb.class);
        when(db.selectAccountById(any())).thenReturn(null);
        AccountRepository repository = new AccountRepositoryImpl(db);
        repository.getById(UUID.randomUUID());
    }

    @Test
    public void updateByIdTest() {
        UUID id = UUID.randomUUID();
        AccountModel account = buildAccount(id);
        AccountModel updatedAccount = AccountModel.builder()
                .id(id)
                .firstName("fnu")
                .lastName("lnu")
                .balance(new BigDecimal(1234))
                .build();

        AppDb db = mock(AppDb.class);
        when(db.updateAccountById(account)).thenReturn(updatedAccount);
        AccountRepository repository = new AccountRepositoryImpl(db);
        AccountModel mockAccount = repository.updateById(account);

        assertNotNull(mockAccount);
        assertEquals(updatedAccount.getId(), mockAccount.getId());
        assertEquals(updatedAccount.getFirstName(), mockAccount.getFirstName());
        assertEquals(updatedAccount.getLastName(), mockAccount.getLastName());
        assertEquals(updatedAccount.getBalance(), mockAccount.getBalance());
        verify(db, times(1)).updateAccountById(account);

    }

    @Test
    public void createTest() throws Exception {
        UUID id = UUID.randomUUID();
        AccountModel account = buildAccount(null);
        AccountModel createdAccount = AccountModel.builder()
                .id(id)
                .firstName("fn")
                .lastName("ln")
                .balance(new BigDecimal(1234))
                .build();

        AppDb db = mock(AppDb.class);
        when(db.insertAccount(account)).thenReturn(createdAccount);
        AccountRepository repository = new AccountRepositoryImpl(db);
        AccountModel mockAccount = repository.create(account);

        assertNotNull(mockAccount);
        assertEquals(createdAccount.getId(), mockAccount.getId());
        assertEquals(createdAccount.getFirstName(), mockAccount.getFirstName());
        assertEquals(createdAccount.getLastName(), mockAccount.getLastName());
        assertEquals(createdAccount.getBalance(), mockAccount.getBalance());
        verify(db, times(1)).insertAccount(account);
    }

    @Test(expected = AppException.class)
    public void createThrowsExceptionTest() throws Exception {
        AppDb db = mock(AppDb.class);
        when(db.insertAccount(any())).thenThrow(new Exception("test"));
        AccountRepository repository = new AccountRepositoryImpl(db);
        repository.create(new AccountModel());
    }

    @Test
    public void deleteByIdTest() throws Exception {
        UUID id = UUID.randomUUID();
        AccountModel account = buildAccount(id);
        AppDb db = mock(AppDb.class);
        when(db.deleteAccountById(id)).thenReturn(account);
        AccountRepository repository = new AccountRepositoryImpl(db);
        AccountModel mockAccount = repository.deleteById(id);

        assertNotNull(mockAccount);
        assertEquals(account.getId(), mockAccount.getId());
        assertEquals(account.getFirstName(), mockAccount.getFirstName());
        assertEquals(account.getLastName(), mockAccount.getLastName());
        assertEquals(account.getBalance(), mockAccount.getBalance());
        verify(db, times(1)).deleteAccountById(id);
    }

    @Test(expected = AppException.class)
    public void deleteByIdThrowsExceptionTest() throws AppException {
        AppDb db = mock(AppDb.class);
        when(db.deleteAccountById(any())).thenThrow(new NullPointerException("test"));
        AccountRepository repository = new AccountRepositoryImpl(db);
        repository.deleteById(UUID.randomUUID());
    }

    public static AccountModel buildAccount(UUID id) {
        return  AccountModel.builder()
                .id(id)
                .firstName("fn")
                .lastName("ln")
                .balance(new BigDecimal(1234))
                .build();
    }

}
