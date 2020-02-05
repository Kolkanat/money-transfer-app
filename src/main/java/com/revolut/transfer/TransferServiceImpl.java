package com.revolut.transfer;

import com.revolut.client.TransferResponse;
import com.revolut.db.AppDb;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@AllArgsConstructor
public class TransferServiceImpl implements TransferService {
    private AppDb appDb;
    private final Queue<TransferRequest> transfers = new ConcurrentLinkedQueue<>();

    public UUID queueTransferRequest(String idFrom, String idTo, String amount, Boolean isAsync) {
        TransferRequest transfer = TransferRequest.builder()
                .accountIdTo(idTo)
                .accountIdFrom(idFrom)
                .uuid(UUID.randomUUID())
                .amount(NumberUtils.toScaledBigDecimal(amount))
                .async(isAsync)
                .build();
        transfers.add(transfer);
        return transfer.getUuid();
    }

    public TransferResponse waitTransferResponse(UUID transactionId, Long waitTimeOut) {
        synchronized (transactionId) {
            try {
                transactionId.wait(waitTimeOut);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                return appDb.selectTransferResultById(transactionId);
            }
        }
    }

    public TransferRequest getNextRequest() {
        return transfers.poll();
    }
}
