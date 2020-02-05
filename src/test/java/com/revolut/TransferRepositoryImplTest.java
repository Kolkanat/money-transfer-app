package com.revolut;

import com.revolut.client.TransferResponse;
import com.revolut.db.AppDb;
import com.revolut.repository.TransferRepository;
import com.revolut.repository.TransferRepositoryImpl;
import org.junit.Test;

import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class TransferRepositoryImplTest {
    @Test
    public void getTransferResultByIdTest() {
        UUID transactionId = UUID.randomUUID();
        TransferResponse response = TransferResponse.builder()
                .amount("1222")
                .message("Test Message")
                .transactionState("SUCCESS")
                .transactionDate(new Date())
                .idTo(UUID.randomUUID().toString())
                .transactionId(transactionId.toString())
                .build();

        AppDb db = mock(AppDb.class);
        TransferRepository repository = new TransferRepositoryImpl(db);
        when(db.selectTransferResultById(transactionId)).thenReturn(response);
        TransferResponse mockResponse = repository.getTransferResultById(transactionId);

        assertNotNull(mockResponse);
        assertEquals(response.getIdTo() ,mockResponse.getIdTo());
        assertEquals(response.getAmount() ,mockResponse.getAmount());
        assertEquals(response.getIdFrom() ,mockResponse.getIdFrom());
        assertEquals(response.getMessage(), mockResponse.getMessage());
        assertEquals(response.getTransactionId() ,mockResponse.getTransactionId());
        assertEquals(response.getTransactionDate() ,mockResponse.getTransactionDate());
        assertEquals(response.getTransactionState() ,mockResponse.getTransactionState());
        verify(db, times(1)).selectTransferResultById(transactionId);
    }
}
