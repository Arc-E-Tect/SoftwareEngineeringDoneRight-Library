package example;

import io.github.microcks.testcontainers.MicrocksContainersEnsemble;
import io.github.microcks.testcontainers.connection.KafkaConnection;
import io.github.microcks.testcontainers.model.TestRequest;
import io.github.microcks.testcontainers.model.TestResult;
import io.github.microcks.testcontainers.model.TestRunnerType;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.DeserializationFeature;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two identical AsyncAPI tests against one freshly started Microcks ensemble. Each starts a
 * Microcks test on a Kafka topic of its own, publishes one message that conforms to the
 * contract, and checks that the message is on the topic and that Microcks reports a success.
 *
 * <p>Both should pass. The first, run by an async minion whose Kafka client is still cold, can
 * fail with "Timeout: no message received" while the message is on the topic and the minion's
 * consumer fetched it; the second, run by the same minion once warm, passes. See README.adoc.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ColdAsyncMinionTest {

    private static final String MICROCKS_IMAGE = "quay.io/microcks/microcks-uber:1.15.0";
    private static final String KAFKA_IMAGE = "confluentinc/cp-kafka:7.8.0";
    private static final String SERVICE_ID = "Order Events:1.0.0";
    private static final String OPERATION = "SEND publishOrderPlaced";
    private static final String MESSAGE = "{\"orderId\":\"order-1\",\"quantity\":2}";
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration SUBSCRIPTION_DELAY = Duration.ofSeconds(1);
    private static final Duration RESULT_WAIT = Duration.ofSeconds(30);

    /** The minion's log lines that show when it consumes, and what becomes of what it consumed. */
    private static final Pattern TIMELINE = Pattern.compile(
            "Accepting an ASYNC_API_SCHEMA test|Starting consuming messages|Initializing the Kafka consumer"
                    + "|Kafka consumer initialized|Seeking to offset|returned fetch data .*highWatermark=[1-9]"
                    + "|Revoke previously assigned|Kafka consumer has been closed|was timed-out"
                    + "|Consumption ends and we got|No consumed message to validate");

    private static Network network;
    private static ConfluentKafkaContainer kafka;
    private static MicrocksContainersEnsemble ensemble;

    @BeforeAll
    static void startContainers() {
        network = Network.newNetwork();
        kafka = new ConfluentKafkaContainer(KAFKA_IMAGE)
                .withNetwork(network)
                .withNetworkAliases("kafka")
                .withListener("kafka:19092");
        kafka.start();

        ensemble = new MicrocksContainersEnsemble(network, MICROCKS_IMAGE)
                .withAsyncFeature()
                .withKafkaConnection(new KafkaConnection("kafka:19092"))
                .withMainArtifacts("orders-asyncapi.yaml")
                // To see the minion's consumption and its Kafka consumer in its log.
                .withAsyncMinionEnv("QUARKUS_LOG_CONSOLE_LEVEL", "DEBUG")
                .withAsyncMinionEnv("QUARKUS_LOG_CATEGORY__IO_GITHUB_MICROCKS__LEVEL", "DEBUG")
                .withAsyncMinionEnv("QUARKUS_LOG_CATEGORY__ORG_APACHE_KAFKA_CLIENTS_CONSUMER__LEVEL", "DEBUG");
        String minionCpus = System.getProperty("minionCpus");
        if (minionCpus != null) {
            long nanoCpus = (long) (Double.parseDouble(minionCpus) * 1_000_000_000L);
            ensemble.getAsyncMinionContainer().withCreateContainerCmdModifier(
                    cmd -> cmd.getHostConfig().withNanoCPUs(nanoCpus));
            System.out.println("The async minion container is limited to " + minionCpus + " CPU(s).");
        }
        ensemble.start();
    }

    @AfterAll
    static void printMinionTimelineAndStop() {
        if (ensemble != null) {
            System.out.println("===== Async minion timeline =====");
            ensemble.getAsyncMinionContainer().getLogs().lines()
                    .filter(line -> TIMELINE.matcher(line).find())
                    .map(line -> line.length() > 240 ? line.substring(0, 240) + " ..." : line)
                    .forEach(System.out::println);
            System.out.println("===== end of async minion timeline =====");
            ensemble.stop();
        }
        if (kafka != null) kafka.stop();
        if (network != null) network.close();
    }

    @Test
    @Order(1)
    void firstTestOfAFreshMinion() throws Exception {
        publishAndExpectMicrocksToReceiveIt();
    }

    @Test
    @Order(2)
    void secondTestOfTheSameMinion() throws Exception {
        publishAndExpectMicrocksToReceiveIt();
    }

    private static void publishAndExpectMicrocksToReceiveIt() throws Exception {
        String topic = "orders-" + UUID.randomUUID();
        TestRequest request = new TestRequest.Builder()
                .serviceId(SERVICE_ID)
                .filteredOperations(List.of(OPERATION))
                .runnerType(TestRunnerType.ASYNC_API_SCHEMA)
                // startOffset=0: the topic is this test's own, so the message is at offset 0, and is
                // read even if the minion's consumer is assigned its partition after it was sent.
                .testEndpoint("kafka://kafka:19092/" + topic + "?startOffset=0")
                .timeout(TEST_TIMEOUT)
                .build();
        CompletableFuture<TestResult> future = ensemble.getMicrocksContainer().testEndpointAsync(request);
        Thread.sleep(SUBSCRIPTION_DELAY.toMillis());
        publish(topic);

        TestResult result = finalResult(future.get());
        List<String> onTheTopic = readTopic(topic);
        String verdict = result.getTestCaseResults().stream()
                .flatMap(testCase -> testCase.getTestStepResults().stream())
                .map(step -> String.valueOf(step.getMessage()))
                .toList().toString();
        System.out.println("Topic " + topic + ": " + onTheTopic.size() + " message(s) on it; Microcks: success="
                + result.isSuccess() + ", inProgress=" + result.isInProgress() + ", steps=" + verdict);

        assertEquals(List.of(MESSAGE), onTheTopic, "The message is on the topic.");
        assertTrue(result.isSuccess(), "Microcks reports the message conforms; it reported " + verdict);
    }

    private static void publish(String topic) {
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName(),
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()))) {
            producer.send(new ProducerRecord<>(topic, MESSAGE));
            producer.flush();
        }
    }

    /**
     * Microcks' final result: microcks-testcontainers stops waiting one second after the test's
     * timeout and returns the result as it stands, so ask Microcks again until it is final.
     */
    private static TestResult finalResult(TestResult result) throws Exception {
        long deadline = System.nanoTime() + RESULT_WAIT.toNanos();
        String url = ensemble.getMicrocksContainer().getHttpEndpoint() + "/api/tests/" + result.getId();
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try (HttpClient client = HttpClient.newHttpClient()) {
            while (result.isInProgress() && System.nanoTime() < deadline) {
                Thread.sleep(200);
                HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                result = mapper.readValue(response.body(), TestResult.class);
            }
        }
        return result;
    }

    /** Every message on {@code topic}, read from offset 0 by a consumer of the test's own. */
    private static List<String> readTopic(String topic) {
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName(),
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()))) {
            TopicPartition partition = new TopicPartition(topic, 0);
            consumer.assign(List.of(partition));
            consumer.seekToBeginning(List.of(partition));
            List<String> messages = new ArrayList<>();
            long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
            while (System.nanoTime() < deadline) {
                for (ConsumerRecord<String, String> consumed : consumer.poll(Duration.ofMillis(500))) {
                    messages.add(consumed.value());
                }
            }
            return messages;
        }
    }
}
