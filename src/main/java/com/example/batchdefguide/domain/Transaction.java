package com.example.batchdefguide.domain;

import lombok.*;

import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private String accountNumber;
    private Date trasactionDate;
    private Double amount;
}
