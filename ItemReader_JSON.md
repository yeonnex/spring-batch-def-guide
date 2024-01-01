# 개요
스프링 배치는 JSON 을 읽어들일 수 있는 `ItemReader` 를 제공한다.

`JsonItemReader` 는 JSON 청크를 읽어서 객체로 파싱한다는 점에서 `StaxEventItemReader` 의 동작 방식과 거의 동일하다.
JSON 문서는 객체로 구성된 배열이 최상단에 하나만 존재하는 완전한 형태의 문서여야 한다.

`JsonItemReader` 가 동작할 때 실제 파싱 작업은 `JsonObjectReader` 인터페이스의 구현체에게 위임된다.

`JsonObjectReader` 인터페이스는 `StaxEventItemReader` 에서 언마샬러가 XML 을 객체로 파싱하는 것과 유사한 방법으로 
실제로 JSON 을 객체로 파싱하는 역할을 한다.

스프링 배치는 애플리케이션 개발에 즉시 사용할 수 있도록 `JsonObjectReader` 인터페이스 구현체 두 개를 제공한다.

- `Jackson` 파싱 엔진
- `Gson` 파싱 엔진

뒤이은 예에서는 `Jackson`을 사용하도록 하겠다.

# 잡 구성
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
```
- 1. 먼저 `ObjectMapper` 인스턴스를 생성했다. `ObjectMapper` 클래스는 Jackson 이 Json 을 읽고 쓰는 데 사용하는 주요클래스다.
    사용하는 예제에서는 입력 파일의 날짜 형식을 지정해야 하는데, 즉 커스터마이징이 필요하기 때문에 이렇게 설정했다. 애플리케이션을 개발할 때
    대부분은 `ObjectMapper` 를 생성하는 코드를 직접 작성할 필요가 없다.
- 2. 이어서 `JacksonJsonObejctReader` 를 생성한다.

## 도메인 클래스
```java
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
```

# 마무리
스프링 배치는,
- 고정 너비 파일
- 구분자로 구분된 파일
- XML 파일
- JSON 파일

같은 다양한 형태로 레코드 구성을 다룰 수 있다.
코드를 작성할 필요가 없거나, 아주 조금만 수정하여 사용할 수 있다.

그러나 파일 입력만 있는 것이 아니다. 관계형 데이터베이스는 배치 처리 입력에서 큰 부분을 차지한다.

다음 시간에는 스프링 배치가 제공하는 데이터베이스 입력에 대해 알아보자.

