package com.revolut;

import com.revolut.client.AppException;
import com.revolut.controller.AppController;
import com.revolut.controller.AppControllerImpl;
import com.revolut.db.AppDb;
import com.revolut.db.InMemoryAppDb;
import com.revolut.repository.AccountRepository;
import com.revolut.repository.AccountRepositoryImpl;
import com.revolut.repository.TransferRepository;
import com.revolut.repository.TransferRepositoryImpl;
import com.revolut.transaction.TransactionProcessor;
import com.revolut.transfer.TransferService;
import com.revolut.transfer.TransferServiceImpl;
import io.javalin.Javalin;
import java.util.concurrent.Executors;

public class App {

    public static void main(String[] args) {
        //starting up db
        final AppDb db = new InMemoryAppDb();

        //initializing services
        final TransferService transferService = new TransferServiceImpl(db);
        final AccountRepository accountRepository = new AccountRepositoryImpl(db);
        final TransferRepository transferRepository = new TransferRepositoryImpl(db);

        //initializing controller
        final AppController controller = new AppControllerImpl(transferService, accountRepository, transferRepository);

        //starting up transaction processor
        final TransactionProcessor transactionProcessor = new TransactionProcessor(db, transferService);
        Executors.newSingleThreadExecutor().execute(transactionProcessor);

        startServer(controller, 7777);
    }

    public static Javalin startServer(AppController controller, int port) {
        Javalin server = Javalin.create().start(port);

        server.get("/account/:id", ctx -> ctx.json(controller.getAccountById(ctx)));
        server.put("/account/:id", ctx -> ctx.json(controller.updateAccountById(ctx)));
        server.post("/account", ctx -> ctx.json(controller.createAccount(ctx)));
        server.delete("/account/:id", ctx -> ctx.json(controller.deleteAccountById(ctx)));

        server.get("/transfer/:id", ctx -> ctx.json(controller.getTransferResultById(ctx)));
        server.post("/transfer/async", ctx -> ctx.json(controller.transferMoneyAsync(ctx)));
        server.post("/transfer", ctx -> ctx.json(controller.transferMoney(ctx)));

        server.exception(AppException.class, (e, ctx) -> {
            ctx.result(e.getMessage());
            ctx.status(e.getHttpCode());
        }).exception(IllegalArgumentException.class, (e, ctx) -> {
            ctx.result("Not proper request params.");
            ctx.status(400);
        }).exception(NullPointerException.class, (e, ctx) -> {
            ctx.result("Not proper request.");
            ctx.status(400);
        }).exception(Exception.class, (e, ctx) -> {
            ctx.result("Internal server error.");
            ctx.status(500);
        });
        return server;
    }

}
