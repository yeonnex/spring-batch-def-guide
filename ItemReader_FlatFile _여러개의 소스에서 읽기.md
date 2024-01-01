# 개요

스프링 배치는 멀티 포맷으로 구성된 레코드 예제에서 사용했던 것과 비슷한 패턴을 사용하는
`MultiResourceItemReader` 라는 `ItemReader` 를 제공한다.

`MultiResoureItemReader` 는 `CustomerFileItemReader` 처럼 다른 `ItemReader`를 래핑한다.
하지만 `MultiResourceItemReader` 는 읽어들일 리소스를 정의하는 일을 자식 `ItemReader`에게 맡기지 않는다.
대신 읽어야할 파일명의 패턴을 `MultiResourceItemReader` 의 의존성으로 정의한다.

이번 예제에서는 여러 줄로 구성된 레코드 예제에서 사용했던 것처럼 동일한 파일 포맷을 사용한다.
`customerFile1.csv`, `customerFile2.csv`, `customerFile3.csv`, `customerFile4.csv`, `customerFile5.csv` 인 파일 다섯개를 처리해보자.
이렇게 하려면 두 부분을 수정해야 하는데 먼저 수정해야할 것은 `배치 잡 구성` dlek.
`MultiResourceItemReader` 를 사용하도록 구성을 손봐야 한다.

# 여러 고객의 파일을 처리하는 잡 구성

```java
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
                .chunk(10)
                .reader(multiCustomerReader(null))
                .writer(itemWriter())
                .build();
    }

    @Bean
    @StepScope
    MultiResourceItemReader multiCustomerReader(@Value("#{'${customerFile}'.split(',')}") Resource[] inputFiles) {
        return new MultiResourceItemReaderBuilder<>()
                .name("multiCustomerReader")
                .resources(inputFiles)
                .delegate(customerFileReader())
                .build();
    }

    @Bean
    CustomerFileReader customerFileReader() {
        return new CustomerFileReader(customerItemReader());
    }

    @Bean
    @StepScope
    FlatFileItemReader customerItemReader() {
        return new FlatFileItemReaderBuilder()
                .name("customerItemReader")
                .lineMapper(lineTokenizer())
                .build();
    }

    @Bean
    PatternMatchingCompositeLineMapper lineTokenizer() {
        Map<String, LineTokenizer> lineTokenizers = new HashMap<>(2);

        lineTokenizers.put("CUST*", customerLineTokenizer());
        lineTokenizers.put("TRANS*", transactionLineTokenizer());

        Map<String, FieldSetMapper> fieldSetMappers = new HashMap<>(2);

        BeanWrapperFieldSetMapper<Customer> customerFieldSetMapper = new BeanWrapperFieldSetMapper<>();

        customerFieldSetMapper.setTargetType(Customer.class);
        fieldSetMappers.put("CUST*", customerFieldSetMapper);
        fieldSetMappers.put("TRANS*", new TransactionFieldSetMapper());

        PatternMatchingCompositeLineMapper lineMappers = new PatternMatchingCompositeLineMapper();
        lineMappers.setTokenizers(lineTokenizers);
        lineMappers.setFieldSetMappers(fieldSetMappers);

        return lineMappers;
    }

    @Bean
    DelimitedLineTokenizer customerLineTokenizer() {
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setNames("firstName", "middleInitial", "lastName", "address", "city", "state", "zipCode");
        // 로우에서 0번쨰 필드 즉 첫번쨰 필드의 값인 CUST or TRANS 여부를 나탸내는 건 제외하기 위함.
        lineTokenizer.setIncludedFields(1, 2, 3, 4, 5, 6, 7);
        return lineTokenizer;
    }

    @Bean
    DelimitedLineTokenizer transactionLineTokenizer() {
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setNames("prefix", "accountNumber", "transactionDate", "amount");
        return lineTokenizer;
    }

    @Bean
    ItemWriter itemWriter() {
        return items -> items.forEach(System.out::println);
    }
}

```

# 커맨드 라인 명령과 읽어들인 파일 확인

```shell
$ java -jar build/libs/batch-def-guide-0.0.1-SNAPSHOT.jar --customerFile=/input/customerMultiFormat2.csv,/input/customerMultiFormat1.csv
```

잡 파라미터 설정 (`--` 없이 인자 주는 것)이 파일 복수개가 잘 안먹어서
애플리케이션 CLI 인수로 파일 리스트를 주었다.

잡 파라미터로 여러개의 갑을 주는 건 인식이 잘 안되는 것 같다... 나중에 다시 시도해보기!

```java
    @Bean
    @StepScope
    MultiResourceItemReader multiCustomerReader(@Value("#{'${customerFile}'.split(',')}") Resource[] inputFiles) {
        return new MultiResourceItemReaderBuilder<>()
                .name("multiCustomerReader")
                .resources(inputFiles)
                .delegate(customerFileReader())
                .build();
    }
```

# 마무리

지금까지 플랫 파일과 연관된 여러 시나리오를 다뤘다.

- 고정 너비 레코드 읽기
- 구분자로 구분된 레코드 읽기
- 여러 줄로 구성된 레코드 읽기
- 다중 파일 입력 읽기

그러나 처리 대상으로 플랫 파일 타입만 있는 것은 아니다.
XML 은 인기가 시들해진 입력 타입이긴 하지만 여전히 엔터프라이즈 환경에서 파일 기반의
상당량을 차지하고 있다.

다음에는 XML 파일을 읽어들이는 법을 살펴보자.
