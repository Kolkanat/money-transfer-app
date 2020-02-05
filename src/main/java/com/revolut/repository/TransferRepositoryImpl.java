package com.revolut.repository;

import com.revolut.client.TransferResponse;
import com.revolut.db.AppDb;
import lombok.AllArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
public class TransferRepositoryImpl implements TransferRepository {

    private AppDb db;

    @Override
    public TransferResponse getTransferResultById(UUID transactionId) {
        return db.selectTransferResultById(transactionId);
    }
}
