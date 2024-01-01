package com.example.batchdefguide.reader;

import com.example.batchdefguide.domain.Customer;
import com.example.batchdefguide.domain.Transaction;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.file.ResourceAwareItemReaderItemStream;
import org.springframework.core.io.Resource;

import java.util.ArrayList;

public class CustomerFileReader implements ResourceAwareItemReaderItemStream<Customer> {

    private final ResourceAwareItemReaderItemStream<Object> delegate;
    private Object cutItem = null;

    public CustomerFileReader(ResourceAwareItemReaderItemStream<Object> delegate) {
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

    @Override
    public void setResource(Resource resource) {
        this.delegate.setResource(resource);
    }
}
