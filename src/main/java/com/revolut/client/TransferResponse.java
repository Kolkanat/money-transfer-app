package com.revolut.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {
    public String transactionId;
    public String transactionState;
    public String message;
    public String idFrom;
    public String idTo;
    public String amount;
    public Date transactionDate;
}
