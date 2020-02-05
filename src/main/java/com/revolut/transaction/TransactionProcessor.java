package com.revolut.transaction;

import com.revolut.client.TransferResponse;
import com.revolut.db.AppDb;
import com.revolut.transfer.TransferService;
import lombok.AllArgsConstructor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;

@AllArgsConstructor
public class TransactionProcessor implements Runnable {

    private AppDb db;
    private TransferService transferService;

    @Override
    public void run() {
        ReentrantLock lock = new ReentrantLock();
        ExecutorService executor = Executors.newFixedThreadPool(5);

        while (true) {
            ofNullable(transferService.getNextRequest()).ifPresentOrElse(t -> {
                Runnable r = () -> {
                    lock.lock();
                    TransferResponse response = db.makeTransfer(fromString(t.getAccountIdFrom()),
                            fromString(t.getAccountIdTo()), t.getAmount(), lock);
                    response.setTransactionId(t.getUuid().toString());
                    db.insertTransferResult(t.getUuid(), response);
                    if (!t.getAsync()) {
                        synchronized (t.getUuid()) {
                            t.getUuid().notify();
                        }
                    }
                };
                executor.execute(r);
            },
            () -> {
                try {
                    Thread.sleep(10l);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
