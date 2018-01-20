package demo

import com.natpryce.hamkrest.assertion.assert
import com.natpryce.hamkrest.equalTo
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.*
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.KStreamBuilder
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.*

class HelloKafkaTest {

    @get:Rule
    val kafkaBrokerRule = KafkaBrokerRule()

    private val inputTopic = "inputTopic"

    private val outputTopic = "outputTopic"

    @Before
    fun setUp() {
        kafkaBrokerRule.createTopic(inputTopic)
        kafkaBrokerRule.createTopic(outputTopic)
    }

    @Test
    fun shouldUppercaseTheInput() {
        val inputValues = listOf("hello", "world")
        val expectedValues = listOf("HELLO", "WORLD")

        val builder = KStreamBuilder()

        val streamsConfiguration = Properties()
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "map-function-lambda-integration-test")
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokerRule.bootstrapServers())
        streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.ByteArray().javaClass.name)
        streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().javaClass.name)
        streamsConfiguration.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

        val input: KStream<ByteArray, String> = builder.stream(inputTopic)
        val uppercased = input.mapValues({ it.toUpperCase() })
        uppercased.to(outputTopic)

        val streams = KafkaStreams(builder, streamsConfiguration)
        streams.start()

        //
        // Step 2: Produce some input data to the input topic.
        //
        val producerConfig = Properties()
        producerConfig[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaBrokerRule.bootstrapServers()
        producerConfig[ProducerConfig.ACKS_CONFIG] = "all"
        producerConfig[ProducerConfig.RETRIES_CONFIG] = 0
        producerConfig[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = ByteArraySerializer::class.java
        producerConfig[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        IntegrationTestUtils.produceValuesSynchronously(inputTopic, inputValues, producerConfig)

        //
        // Step 3: Verify the application's output data.
        //
        val consumerConfig = Properties()
        consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokerRule.bootstrapServers())
        consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, "map-function-lambda-integration-test-standard-consumer")
        consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer::class.java)
        consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
        val actualValues:List<String> = IntegrationTestUtils.waitUntilMinValuesRecordsReceived(consumerConfig,
                outputTopic, expectedValues.size)
        streams.close();
        assert.that(actualValues, equalTo(expectedValues))
    }
}