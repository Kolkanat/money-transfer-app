package com.revolut.repository;

import com.revolut.client.TransferResponse;

import java.util.UUID;

public interface TransferRepository {
    TransferResponse getTransferResultById(UUID transactionId);
}
