package com.revolut;

import com.revolut.client.TransferResponse;
import com.revolut.db.AppDb;
import com.revolut.transaction.TransactionProcessor;
import com.revolut.transfer.TransferRequest;
import com.revolut.transfer.TransferService;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.*;

import static java.util.UUID.fromString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class TransactionProcessorTest {
    @Test
    public void runTest() throws ExecutionException, InterruptedException {
        Long timeToWait = 1000l;
        UUID transactionId = UUID.randomUUID();

        Callable<Long> callable = () -> {
            long timeInMills = System.currentTimeMillis();
            synchronized (transactionId) {
                try {
                    transactionId.wait(timeToWait);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            timeInMills = System.currentTimeMillis() - timeInMills;
            return timeInMills;
        };

        TransferRequest request = TransferRequest.builder()
                .accountIdFrom(UUID.randomUUID().toString())
                .accountIdTo(UUID.randomUUID().toString())
                .amount(BigDecimal.valueOf(1456l))
                .uuid(transactionId)
                .async(false)
                .build();

        TransferResponse response = TransferResponse.builder()
                .amount(request.getAmount().toString())
                .message("Test Message")
                .transactionState("SUCCESS")
                .transactionDate(new Date())
                .idTo(request.getAccountIdTo())
                .idFrom(request.getAccountIdFrom())
                .build();
        AppDb db = mock(AppDb.class);
        TransferService transferService = mock(TransferService.class);

        when(transferService.getNextRequest()).thenReturn(null).thenReturn(request).thenReturn(null);
        when(db.makeTransfer(eq(fromString(request.getAccountIdFrom())), eq(fromString(request.getAccountIdTo())),
                eq(request.getAmount()), any())).thenReturn(response);

        TransactionProcessor tp = new TransactionProcessor(db, transferService);
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Future<Long> future = executorService.submit(callable);
        executorService.execute(tp);
        executorService.shutdown();
        executorService.awaitTermination(2, TimeUnit.SECONDS);

        assertTrue(future.get() < timeToWait);
        assertEquals(transactionId.toString(), response.getTransactionId());
        verify(db, times(1)).insertTransferResult(transactionId, response);
    }
}
