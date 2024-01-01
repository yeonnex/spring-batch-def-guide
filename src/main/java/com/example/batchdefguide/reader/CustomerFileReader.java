package com.example.batchdefguide.reader;

import com.example.batchdefguide.domain.Customer;
import com.example.batchdefguide.domain.Transaction;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;

import java.util.ArrayList;

public class CustomerFileReader implements ItemStreamReader<Customer> {

    private Object cutItem = null;
    private ItemStreamReader<Object> delegate;

    public CustomerFileReader(ItemStreamReader<Object> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Customer read() throws Exception {
        if (cutItem == null) {
            cutItem = delegate.read();
        }

        Customer item = (Customer) cutItem;
        cutItem = null;

        if (item != null) {
            item.setTransactions(new ArrayList<>());

            while (peek() instanceof Transaction) {
                item.getTransactions().add((Transaction) cutItem);
            }
        }
        return item;
    }

    private Object peek() throws Exception {
        cutItem = delegate.read();
        return cutItem;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        delegate.open(executionContext);
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        delegate.update(executionContext);
    }

    @Override
    public void close() throws ItemStreamException {
        delegate.close();
    }
}
