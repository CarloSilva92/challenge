package com.db.awmd.challenge.domain;

import java.math.BigDecimal;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class TransferMoneyDTO {

    @NotNull
    @NotEmpty
    private final String accountFromId;

    @NotNull
    @NotEmpty
    private final String accountToId;

    @NotNull
    private final BigDecimal amountToTransfer;

    @JsonCreator
    public TransferMoneyDTO(@JsonProperty("accountFromId") String accountFromId,
                   @JsonProperty("accountToId") String accountToId,
                   @JsonProperty("amountToTransfer") BigDecimal amountToTransfer) {
        this.accountFromId = accountFromId;
        this.accountToId = accountToId;
        this.amountToTransfer = amountToTransfer;
    }
}