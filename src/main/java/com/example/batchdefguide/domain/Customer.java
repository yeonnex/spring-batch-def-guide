package com.example.batchdefguide.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    private String firstName;
    private String middleInitial;
    private String lastName;
    private String address;
    private String city;
    private String state;
    private String zipCode;

    @JsonIgnoreProperties(ignoreUnknown = true)
    private List<Transaction> transactions;

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        output.append(firstName);
        output.append(" ");
        output.append(middleInitial);
        output.append(". ");
        output.append(lastName);

        if (transactions != null && !transactions.isEmpty()) {
            output.append(" has ");
            output.append(transactions.size());
            output.append(" transactions.");
            output.append("amount detail info: ");
            output.append(transactions.get(0).getAmount());
        } else {
            output.append(" has no transactions.");
        }

        return output.toString();
    }
}
