package com.revolut;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revolut.client.TransferResponse;
import com.revolut.controller.AppController;
import com.revolut.controller.AppControllerImpl;
import com.revolut.db.AppDb;
import com.revolut.db.InMemoryAppDb;
import com.revolut.model.AccountModel;
import com.revolut.repository.AccountRepository;
import com.revolut.repository.AccountRepositoryImpl;
import com.revolut.repository.TransferRepository;
import com.revolut.repository.TransferRepositoryImpl;
import com.revolut.transaction.TransactionProcessor;
import com.revolut.transfer.TransferService;
import com.revolut.transfer.TransferServiceImpl;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class AppIntegrationTest {
    private static ObjectMapper mapper = new ObjectMapper();

    @Test
    public void appIntegrationCrudTest() throws JsonProcessingException {
        final AppDb db = new InMemoryAppDb();

        //initializing services
        final TransferService transferService = new TransferServiceImpl(db);
        final AccountRepository accountRepository = new AccountRepositoryImpl(db);
        final TransferRepository transferRepository = new TransferRepositoryImpl(db);

        //initializing controller
        final AppController controller = new AppControllerImpl(transferService, accountRepository, transferRepository);

        Javalin server = App.startServer(controller, 8888);

        //create account
        HttpResponse<String> response = Unirest.post("http://localhost:8888/account/")
                .field("firstName", "Myrkymbai")
                .field("lastName", "Myrkymbayev")
                .field("balance", "1233.00")
                .asString();

        AccountModel account = mapper.readValue(response.getBody(), AccountModel.class);

        assertEquals(200, response.getStatus());
        assertNotNull(account.getId());
        assertEquals("Myrkymbai", account.getFirstName());
        assertEquals("Myrkymbayev", account.getLastName());
        assertEquals("1233.00", account.getBalance().toString());

        //fetch account
        UUID currentId = account.getId();
        response = Unirest.get("http://localhost:8888/account/" + currentId).asString();

        account = mapper.readValue(response.getBody(), AccountModel.class);

        assertEquals(200, response.getStatus());
        assertEquals(currentId, account.getId());
        assertEquals("Myrkymbai", account.getFirstName());
        assertEquals("Myrkymbayev", account.getLastName());
        assertEquals("1233.00", account.getBalance().toString());

        //update account
        response = Unirest.put("http://localhost:8888/account/" + currentId)
                .field("firstName", "Nick")
                .field("lastName", "Nickov")
                .asString();
        account = mapper.readValue(response.getBody(), AccountModel.class);

        assertEquals(currentId, account.getId());
        assertEquals("Nick", account.getFirstName());
        assertEquals("Nickov", account.getLastName());
        assertEquals("1233.00", account.getBalance().toString());

        //delete account
        response = Unirest.delete("http://localhost:8888/account/" + currentId).asString();
        account = mapper.readValue(response.getBody(), AccountModel.class);

        assertEquals(200, response.getStatus());
        assertEquals(currentId, account.getId());
        assertEquals("Nick", account.getFirstName());
        assertEquals("Nickov", account.getLastName());
        assertEquals("1233.00", account.getBalance().toString());

        response = Unirest.get("http://localhost:8888/account/" + currentId).asString();
        assertEquals(404, response.getStatus());
        assertEquals("Account not found.", response.getBody());

        server.stop();
    }

    @Test
    public void appIntegrationTransferStressTest() throws JsonProcessingException, InterruptedException {
        List<String> transferIds = new ArrayList<>();
        final AppDb db = new InMemoryAppDb();

        //initializing services
        final TransferService transferService = new TransferServiceImpl(db);
        final AccountRepository accountRepository = new AccountRepositoryImpl(db);
        final TransferRepository transferRepository = new TransferRepositoryImpl(db);

        //initializing controller
        final AppController controller = new AppControllerImpl(transferService, accountRepository, transferRepository);

        //starting up transaction system
        final TransactionProcessor transactionProcessor = new TransactionProcessor(db, transferService);
        Executors.newSingleThreadExecutor().execute(transactionProcessor);

        Javalin server = App.startServer(controller, 7777);

        BigDecimal balance1 = new BigDecimal("1233.00");
        AccountModel account1 = AccountModel.builder()
                .firstName("Myrkymbai")
                .lastName("Myrkymbayev")
                .balance(balance1)
                .build();

        //create account-1
        HttpResponse<String> response = Unirest.post("http://localhost:7777/account/")
                .field("firstName", account1.getFirstName())
                .field("lastName", account1.getLastName())
                .field("balance", account1.getBalance().toString())
                .asString();

        account1 = mapper.readValue(response.getBody(), AccountModel.class);

        assertEquals(200, response.getStatus());
        assertNotNull(account1.getId());

        BigDecimal balance2 = new BigDecimal("255.00");
        AccountModel account2 = AccountModel.builder()
                .firstName("Will")
                .lastName("Smith")
                .balance(balance2)
                .build();

        //create account-2
        response = Unirest.post("http://localhost:7777/account/")
                .field("firstName", account2.getFirstName())
                .field("lastName", account2.getLastName())
                .field("balance", account2.getBalance().toString())
                .asString();

        account2 = mapper.readValue(response.getBody(), AccountModel.class);

        assertEquals(200, response.getStatus());
        assertNotNull(account2.getId());

        BigDecimal balance3 = new BigDecimal("123.40");
        AccountModel account3 = AccountModel.builder()
                .firstName("Petya")
                .lastName("Ptushkin")
                .balance(balance3)
                .build();

        //create account-3
        response = Unirest.post("http://localhost:7777/account/")
                .field("firstName", account3.getFirstName())
                .field("lastName", account3.getLastName())
                .field("balance", account3.getBalance().toString())
                .asString();

        account3 = mapper.readValue(response.getBody(), AccountModel.class);

        assertEquals(200, response.getStatus());
        assertNotNull(account3.getId());

        String id1 = account1.getId().toString();
        String id2 = account2.getId().toString();
        String id3 = account3.getId().toString();

        BigDecimal amount1 = new BigDecimal("31.00");
        BigDecimal amount2 = new BigDecimal("14.00");

        //lambda for transfer from account-1 to account-2
        Runnable acc1ToAcc2Transfer = () -> {
            Unirest.post("http://localhost:7777/transfer")
                    .field("amount", amount1)
                    .field("idFrom", id1)
                    .field("idTo", id2).asString();
        };

        ReentrantLock lock = new ReentrantLock();
        //lambda for transfer from account-2 to account-3 asynchronously
        Runnable acc2ToAcc3Transfer = () -> {
            HttpResponse<String> httpResponse = Unirest.post("http://localhost:7777/transfer/async")
                    .field("amount", amount2)
                    .field("idFrom", id2)
                    .field("idTo", id3).asString();
            lock.lock();
            try {
                transferIds.add(mapper.readValue(httpResponse.getBody(), String.class));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            lock.unlock();
        };

        int count = 4;
        ExecutorService executor = Executors.newFixedThreadPool(count);
        IntStream.rangeClosed(1, count).parallel()
                .forEach(i -> executor.execute(i%2 == 0 ? acc1ToAcc2Transfer:acc2ToAcc3Transfer));

        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.MINUTES);
        //waiting until transactionProcessor finishes its job
        Thread.sleep(1000l);

        //asserting get http://localhost:7777/transfer/:id
        TransferResponse transferResponse;
        for (String id:transferIds) {
            HttpResponse<String> httpResponse = Unirest.get("http://localhost:7777/transfer/" + id).asString();
            transferResponse = mapper.readValue(httpResponse.getBody(), TransferResponse.class);
            assertEquals(id, transferResponse.getTransactionId());
            assertEquals(amount2.toString(), transferResponse.getAmount());
            assertEquals(id3, transferResponse.getIdTo());
            assertEquals(id2, transferResponse.getIdFrom());
            assertEquals("Success transaction.", transferResponse.getMessage());
            assertEquals("SUCCESS", transferResponse.getTransactionState());
        }

        response = Unirest.get("http://localhost:7777/account/" + id1).asString();
        account1 = mapper.readValue(response.getBody(), AccountModel.class);

        response = Unirest.get("http://localhost:7777/account/" + id2).asString();
        account2 = mapper.readValue(response.getBody(), AccountModel.class);

        response = Unirest.get("http://localhost:7777/account/" + id3).asString();
        account3 = mapper.readValue(response.getBody(), AccountModel.class);

        server.stop();

        BigDecimal finalBalance1 = balance1.subtract(amount1.multiply(new BigDecimal(count/2)));
        BigDecimal finalBalance2 = balance2.add(amount1.multiply(new BigDecimal(count/2)))
                .subtract(amount2.multiply(new BigDecimal(count/2)));
        BigDecimal finalBalance3 = balance3.add(amount2.multiply(new BigDecimal(count/2)));

        //asserting final result
        assertEquals(finalBalance1, account1.getBalance());
        assertEquals(finalBalance2, account2.getBalance());
        assertEquals(finalBalance3, account3.getBalance());
    }
}
