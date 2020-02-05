package com.revolut.transfer;

import com.revolut.client.TransferResponse;

import java.util.UUID;

public interface TransferService {
    TransferRequest getNextRequest();
    TransferResponse waitTransferResponse(UUID transactionId,Long waitTimeOut );
    UUID queueTransferRequest(String idFrom, String idTo, String amount, Boolean async);
}
