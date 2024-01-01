# 개요

파일 포맷의 차이는 파일의 포맷을 설명하는 메타데이터 양의 차이와 관련이 있다.
고정 너비 레코드에는 메타데이터가 거의 없어서 사전에 레코드 포맷의 정보가 가장 많이 필요하다.

XML 은 고정 너비 레코드와는 정반대이다. `XML` 은 파일 내 데이터를 설명할 수 있는 태그를
사용해서 `파일에 포함된 데이터를 완벽히 설명`하기 때문이다.

# XML 파서의 종류

XML 파서로 `DOM 파서` 와 `SAX 파서` 를 많이 사용한다.

## DOM 파서

노드를 탐색할 수 있도록 전체 파일을 메모리에 트리 구조로 읽어들인다. 이 방법은 성능상
큰 부하가 발생하므로 배치 처리에서는 유용하지 않다.

## SAX 파서

그렇다면 배치에서 사용할 것은 `SAX 파서` 뿐이다.
`SAX 파서` 특정 엘리먼트를 만나면 이벤트를 발생시키는 `이벤트 기반 파서` 이다.

스프링 배치에서는 `StAX` 파서를 사용한다.

## 스프링 배치의 XML 바인딩 기술

스프링 배치는 XML 바인딩 기술과 호환성이 좋아서 개발자가 선택한 특정 기술을
크게 가리지 않는다. 스프링은 xml 패키지에 포함된 `Castor`, `JAXB` 등의 구현체를 제공한다.
이 예제에서는 `JAXB` 를 사용하겠다.

`JAXB` 의존성과 함께 스프링 OXM 모듈로 `JAXB` 를 사용하는 스프링 컴포넌트 의존성을 구성한다.

```groovy
// 의존성 목록
implementation 'javax.xml.bind:jaxb-api:2.3.1'
implementation 'jakarta.activation:jakarta.activation-api:2.1.2'
implementation 'com.sun.xml.bind:jaxb-core:4.0.4'
implementation 'com.sun.xml.bind:jaxb-impl:4.0.4'
implementation 'org.springframework:spring-oxm:5.3.31'

```

# 고객 XML 파일

```xml

<customers>
    <customer>
        <firstName>Laura</firstName>
        <middleInitial>O</middleInitial>
        <lastName>Minella</lastName>
        <address>2039 Wall Street</address>
        <city>Omaha</city>
        <state>IL</state>
        <zipCode>35446</zipCode>
        <transactions>
            <transaction>
                <accountNumber>829433</accountNumber>
                <transactionDate>2010-10-14 05:49:58</transactionDate>
                <amount>26.08</amount>
            </transaction>
        </transactions>
    </customer>
    <customer>
        <firstName>Michael</firstName>
        <middleInitial>T</middleInitial>
        <lastName>Buffett</lastName>
        <address>8192 Wall Street</address>
        <city>Omaha</city>
        <state>NE</state>
        <zipCode>25372</zipCode>
        <transactions>
            <transaction>
                <accountNumber>8179238</accountNumber>
                <transactionDate>2010-10-27 05:56:59</transactionDate>
                <amount>-91.76</amount>
            </transaction>
            <transaction>
                <accountNumber>8179238</accountNumber>
                <transactionDate>2010-10-06 21:51:05</transactionDate>
                <amount>-25.99</amount>
            </transaction>
        </transactions>
    </customer>
</customers>
```

고객 파일은 각 고객 섹션이 모인 컬렉션 구조로 구성되어 있다.
각 고객 섹션에는 거래 섹션의 컬렉션이 포함돼어있다.

플랫 파일을 처리할 때는 스프링 배치가 각 줄을 `FieldSet` 으로 파싱했다.
XML 을 처리할 때 스프링 배치는 사용자가 정의한 XML 프래그먼트를 도메인 객체로 파싱한다.

스프링 배치는 파일 내에서 미리 지정한 XML 프래그먼트를 만날 때마다 이를 단일 레코드로 간주하고
처리 대상 아이템으로 변환한다.

고객 입력 파일에서 각 `<customer>` 프레그먼트 내에는 플랫 파일과 동일한 데이터가 포함되어있다.

XML 입력 파일을 파싱하려면 스프링 배치가 제공하는 `StaxEventItemReader` 를 사용한다.

# XML ItemReader 구성

```java

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
    StaxEventItemReader customerFileReader(@Value("#{jobParameters['customerFile']}") Resource inputFile) {
        return new StaxEventItemReaderBuilder<>()
                .name("customerFileReader")
                .resource(inputFile)
                .addFragmentRootElements("customer")
                .unmarshaller(customerMarshaller())
                .build();
    }

    /**
     * 각 XML 블록을 파싱하는 데 사용할 언마샬러 구성
     *
     * @return 언마샬러
     */
    @Bean
    Jaxb2Marshaller customerMarshaller() {
        Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
        jaxb2Marshaller.setClassesToBeBound(Customer.class, Transaction.class);
        return jaxb2Marshaller;
    }

    @Bean
    ItemWriter<Customer> itemWriter() {
        return items -> items.forEach(System.out::println);
    }
}

```

# 애플리케이션이 XML 을 파싱할 수 있게 구성하는 법

관련 의존성을 추가했으므로, `JAXB` 가 `XML 태그` 와 매핑할 수 있도록
`클래스에 애너테이션을 추가해서 매핑 대상임을 나타내야` 한다.

`Transaction` 클래스에는 간단히 `@XmlType(name="transaction")` 을 추가하자.
그러나 `Customer` 클래스에는 `@XmlRootElement` 애너테이션을 추가해 매칭되는 엘리먼트를 지정하는 작업 외에도,
거래 내역 컬렉션의 구조를 파서에게 알려주는 작업을 해야 한다.

```java
// Customer 클래스
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
    StaxEventItemReader customerFileReader(@Value("#{jobParameters['customerFile']}") Resource inputFile) {
        return new StaxEventItemReaderBuilder<>()
                .name("customerFileReader")
                .resource(inputFile)
                .addFragmentRootElements("customer")
                .unmarshaller(customerMarshaller())
                .build();
    }

    /**
     * 각 XML 블록을 파싱하는 데 사용할 언마샬러 구성
     *
     * @return 언마샬러
     */
    @Bean
    Jaxb2Marshaller customerMarshaller() {
        Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
        jaxb2Marshaller.setClassesToBeBound(Customer.class, Transaction.class);
        return jaxb2Marshaller;
    }

    @Bean
    ItemWriter<Customer> itemWriter() {
        return items -> items.forEach(System.out::println);
    }
}

```

# 잡 실행하기

```shell
$ java -jar build/libs/batch-def-guide-0.0.1-SNAPSHOT.jar customerFile=/input/customer.json
```

