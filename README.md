## Revolut Money Transfer

Implementation a RESTful API (including data model and the backing implementation) for money transfers between accounts.

## How to Build
    mvn clean package

## How to Run
    java -jar ./target/money-transfer-app-1.0-SNAPSHOT-jar-with-dependencies.jar

## End Points

### Accounts
    GET     /account/:id
    PUT     /accounts/:id
    DELETE  /accounts/:id
    POST    /accounts

### Transfers
    GET   /transfer/:id
    POST  /transfer/async
    POST  /transfer
