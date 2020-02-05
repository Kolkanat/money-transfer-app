package com.revolut;

import com.revolut.client.TransferResponse;
import com.revolut.db.AppDb;
import com.revolut.transfer.TransferRequest;
import com.revolut.transfer.TransferService;
import com.revolut.transfer.TransferServiceImpl;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TransferServiceImplTest {

    @Test
    public void queueTransferRequestTest() {
        TransferService transferService = new TransferServiceImpl(null);
        String idFrom = UUID.randomUUID().toString();
        String idTo = UUID.randomUUID().toString();
        String amount = "1000.00";
        UUID transactionId = transferService.queueTransferRequest(idFrom, idTo, amount, false);
        TransferRequest request = transferService.getNextRequest();
        assertNotNull(transactionId);
        assertNotNull(request);
        assertEquals(idFrom, request.getAccountIdFrom());
        assertEquals(idTo, request.getAccountIdTo());
        assertEquals(new BigDecimal(amount), request.getAmount());
        assertEquals(transactionId, request.getUuid());
        assertNull(transferService.getNextRequest());
    }

    @Test
    public void waitTransferResponse() {
        UUID transactionId = UUID.randomUUID();
        TransferResponse response = TransferResponse.builder()
                .amount("1222")
                .transactionState("SUCCESS")
                .transactionId(transactionId.toString())
                .transactionDate(new Date())
                .idTo(UUID.randomUUID().toString())
                .idFrom(UUID.randomUUID().toString())
                .message("Test Message")
                .build();

        AppDb db = mock(AppDb.class);
        when(db.selectTransferResultById(transactionId)).thenReturn(response).thenReturn(null);
        TransferService transferService = new TransferServiceImpl(db);
        long waitTime = 1000;
        long timeMillis = System.currentTimeMillis();
        TransferResponse mockResponse = transferService.waitTransferResponse(transactionId, waitTime);
        timeMillis = System.currentTimeMillis() - timeMillis;

        assertTrue(waitTime < timeMillis);
        assertNotNull(mockResponse);
        assertEquals(response.getIdTo() ,mockResponse.getIdTo());
        assertEquals(response.getAmount() ,mockResponse.getAmount());
        assertEquals(response.getIdFrom() ,mockResponse.getIdFrom());
        assertEquals(response.getMessage(), mockResponse.getMessage());
        assertEquals(response.getTransactionId() ,mockResponse.getTransactionId());
        assertEquals(response.getTransactionDate() ,mockResponse.getTransactionDate());
        assertEquals(response.getTransactionState() ,mockResponse.getTransactionState());

        //testing notify
        when(db.selectTransferResultById(transactionId)).thenReturn(response).thenReturn(null);
        Runnable r = () -> {
            transferService.waitTransferResponse(transactionId, waitTime);
        };
        timeMillis = System.currentTimeMillis();
        new Thread(r).start();
        synchronized (transactionId) {
            transactionId.notify();
        }
        timeMillis = System.currentTimeMillis() - timeMillis;
        assertTrue(waitTime > timeMillis);
    }

}
