package com.revolut;

import com.revolut.client.TransferResponse;
import com.revolut.db.AppDb;
import com.revolut.db.InMemoryAppDb;
import com.revolut.model.AccountModel;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class InMemoryDbTest {

    private static final BigDecimal balance = new BigDecimal("1236.00");

    @Test
    public void insertAccountTest() throws InterruptedException {
        AppDb db = new InMemoryAppDb();
        UUID uuid = UUID.randomUUID();
        ReentrantLock lock = new ReentrantLock();
        List<Integer> sum = new ArrayList<>();
        int attemptNumbers = 30;

        Runnable runnable = () -> {
            try {
                db.insertAccount(buildAccount(uuid));
            } catch (Exception e) {
                lock.lock();
                sum.add(1);
                lock.unlock();
            }
        };

        ExecutorService service = Executors.newFixedThreadPool(30);
        IntStream.rangeClosed(1,attemptNumbers).parallel().forEach(i -> service.execute(runnable));

        service.shutdown();
        service.awaitTermination(1, TimeUnit.MINUTES);
        AccountModel accountModel = buildAccount(uuid);
        AccountModel createdAccount = db.selectAccountById(uuid);

        assertEquals(attemptNumbers - 1, sum.size());
        assertEquals(accountModel.getId(), createdAccount.getId());
        assertEquals(accountModel.getFirstName(), createdAccount.getFirstName());
        assertEquals(accountModel.getLastName(), createdAccount.getLastName());
        assertEquals(accountModel.getBalance(), createdAccount.getBalance());

    }

    @Test
    public void selectAccountByIdTest() throws Exception {
        AppDb db = new InMemoryAppDb();
        UUID uuid = UUID.randomUUID();
        AccountModel accountModel = buildAccount(uuid);

        assertNull(db.selectAccountById(uuid));

        db.insertAccount(accountModel);
        AccountModel createdAccount = db.selectAccountById(uuid);

        assertNotNull(createdAccount);
        assertEquals(accountModel.getId(), createdAccount.getId());
        assertEquals(accountModel.getFirstName(), createdAccount.getFirstName());
        assertEquals(accountModel.getLastName(), createdAccount.getLastName());
        assertEquals(accountModel.getBalance(), createdAccount.getBalance());
    }

    @Test
    public void deleteAccountByIdTest() throws Exception {
        AppDb db = new InMemoryAppDb();
        UUID uuid = UUID.randomUUID();
        AccountModel accountModel = buildAccount(uuid);

        assertNull(db.selectAccountById(uuid));

        db.insertAccount(accountModel);
        assertNotNull(db.selectAccountById(uuid));

        AccountModel deletedAccount = db.deleteAccountById(uuid);
        assertNotNull(deletedAccount);
        assertEquals(accountModel.getId(), deletedAccount.getId());
        assertEquals(accountModel.getFirstName(), deletedAccount.getFirstName());
        assertEquals(accountModel.getLastName(), deletedAccount.getLastName());
        assertEquals(accountModel.getBalance(), deletedAccount.getBalance());
        assertNull(db.selectAccountById(uuid));

    }

    @Test
    public void updateAccountByIdTest() throws Exception {
        AppDb db = new InMemoryAppDb();
        UUID uuid = UUID.randomUUID();
        AccountModel account = buildAccount(uuid);
        db.insertAccount(account);

        account.setFirstName("updated_name");
        account.setLastName("updated_last_name");

        AccountModel updatedAccount = db.updateAccountById(account);;

        assertNotNull(updatedAccount);
        assertEquals(account.getId(), updatedAccount.getId());
        assertEquals(account.getFirstName(), updatedAccount.getFirstName());
        assertEquals(account.getLastName(), updatedAccount.getLastName());
        assertEquals(account.getBalance(), updatedAccount.getBalance());
        assertNull(db.updateAccountById(buildAccount(UUID.randomUUID())));
    }

    @Test
    public void insertAndSelectTransferResultTest() {
        AppDb db = new InMemoryAppDb();
        UUID uuid = UUID.randomUUID();
        TransferResponse transferResponse = buildTransferResponse(uuid);
        assertNull(db.selectTransferResultById(uuid));
        db.insertTransferResult(uuid, transferResponse);
        TransferResponse dbTransferResponse = db.selectTransferResultById(uuid);

        assertNotNull(dbTransferResponse);
        assertEquals(transferResponse.getIdTo(), dbTransferResponse.getIdTo());
        assertEquals(transferResponse.getIdFrom(), dbTransferResponse.getIdFrom());
        assertEquals(transferResponse.getAmount(), dbTransferResponse.getAmount());
        assertEquals(transferResponse.getMessage(), dbTransferResponse.getMessage());
        assertEquals(transferResponse.getTransactionId(), dbTransferResponse.getTransactionId());
        assertEquals(transferResponse.getTransactionDate(), dbTransferResponse.getTransactionDate());
        assertEquals(transferResponse.getTransactionState(), dbTransferResponse.getTransactionState());

    }

    @Test
    public void makeTransferStressTest() throws Exception {
        AppDb db = new InMemoryAppDb();
        UUID idFrom = UUID.randomUUID();
        UUID idTo = UUID.randomUUID();
        AccountModel accountFrom = buildAccount(idFrom);
        AccountModel accountTo = buildAccount(idTo);

        db.insertAccount(accountFrom);
        db.insertAccount(accountTo);

        BigDecimal amount = BigDecimal.valueOf(20d);

        Runnable r = () -> {
            ReentrantLock lock = new ReentrantLock();
            lock.lock();
            db.makeTransfer(idFrom, idTo, amount, lock);
        };

        int pool_size = 2;
        ExecutorService executor = Executors.newFixedThreadPool(pool_size);
        IntStream.rangeClosed(1,pool_size).parallel().forEach(i -> executor.execute(r));

        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.MINUTES);

        BigDecimal operationSum = amount.multiply(BigDecimal.valueOf(pool_size));
        BigDecimal balanceFrom = balance.subtract(operationSum);
        BigDecimal balanceTo = balance.add(operationSum);

        accountFrom = db.selectAccountById(idFrom);
        accountTo = db.selectAccountById(idTo);

        assertEquals(balanceFrom, accountFrom.getBalance());
        assertEquals(balanceTo, accountTo.getBalance());
    }

    public static AccountModel buildAccount(UUID id) {
        return  AccountModel.builder()
                .id(id)
                .firstName("fn")
                .lastName("ln")
                .balance(balance)
                .build();
    }

    public static TransferResponse buildTransferResponse(UUID uuid) {
        return TransferResponse.builder()
                .idTo(UUID.randomUUID().toString())
                .idFrom(UUID.randomUUID().toString())
                .amount("12455")
                .transactionState("SUCCESS")
                .transactionId(uuid.toString())
                .message("TEST")
                .transactionDate(new Date())
                .build();
    }
}
