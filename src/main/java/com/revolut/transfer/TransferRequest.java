package com.revolut.transfer;

import com.revolut.client.AppException;
import io.javalin.http.Context;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import static java.util.Optional.ofNullable;

@Getter
@AllArgsConstructor
@Builder
public class TransferRequest {
    private UUID uuid;
    private String accountIdFrom;
    private String accountIdTo;
    private BigDecimal amount;
    private Boolean async;
}
