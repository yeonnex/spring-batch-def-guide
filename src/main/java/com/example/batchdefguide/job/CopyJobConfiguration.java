package com.example.batchdefguide.job;

import com.example.batchdefguide.domain.Customer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.json.JacksonJsonObjectReader;
import org.springframework.batch.item.json.JsonItemReader;
import org.springframework.batch.item.json.builder.JsonItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.text.SimpleDateFormat;

/**
 * 각 Customer 객체를 대상으로 해당 고객에 얼마나 많은 거래 내역을 보유하고 있는지 출력하는 잡
 */
@Configuration
@RequiredArgsConstructor
public class CopyJobConfiguration {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    Job job() {
        return jobBuilderFactory.get("customerMultiFormatJob")
                .incrementer(new RunIdIncrementer())
                .start(copyFileStep())
                .build();
    }

    @Bean
    Step copyFileStep() {
        return stepBuilderFactory.get("copyFileStep")
                .<Customer, Customer>chunk(10)
                .reader(customerFileReader(null))
                .writer(itemWriter())
                .build();
    }

    @Bean
    @StepScope
    JsonItemReader<Customer> customerFileReader(@Value("#{jobParameters['customerFile']}") Resource inputFile) {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss"));

        JacksonJsonObjectReader<Customer> jsonObjectReader = new JacksonJsonObjectReader<>(Customer.class);
        jsonObjectReader.setMapper(objectMapper);

        return new JsonItemReaderBuilder<Customer>()
                .name("customerFileReader")
                .jsonObjectReader(jsonObjectReader)
                .resource(inputFile)
                .build();
    }

    @Bean
    ItemWriter<Customer> itemWriter() {
        return items -> items.forEach(System.out::println);
    }
}
