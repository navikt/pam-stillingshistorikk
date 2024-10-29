package no.nav.arbeidsplassen.stihibi.kafka

import no.nav.arbeidsplassen.stihibi.nais.HealthService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.KafkaException
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.AuthorizationException
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime

abstract class KafkaRapidListener<T> {
    companion object {
        private val LOG = LoggerFactory.getLogger(KafkaRapidListener::class.java)
    }

    abstract val healthService: HealthService
    abstract val kafkaConsumer: KafkaConsumer<String?, T?>

    abstract fun startListener() : Thread
    abstract fun handleRecord(record: ConsumerRecord<String?, T?>): Unit?

    fun startListenerInternal() {
        LOG.info("Starter Kafka ad-rapid listener")
        var records: ConsumerRecords<String?, T?>?

        while (healthService.isHealthy()) {
            var currentPositions = mutableMapOf<TopicPartition, Long>()
            try {
                records = kafkaConsumer.poll(Duration.ofSeconds(10))
                if (records.count() > 0) {
                    currentPositions = records
                        .groupBy { TopicPartition(it.topic(), it.partition()) }
                        .mapValues { it.value.minOf { it.offset() } }
                        .toMutableMap()

                    LOG.info("Leste ${records.count()} rader. Keys: {}", records.mapNotNull { it.key() }.joinToString())
                    records.forEach { record ->
                        handleRecord(record)
                        currentPositions[TopicPartition(record.topic(), record.partition())] = record.offset() + 1
                    }
                }
            } catch (e: AuthorizationException) {
                LOG.error("AuthorizationException i consumerloop, restarter app ${e.message}", e)
                healthService.addUnhealthyVote()
            } catch (ke: KafkaException) {
                LOG.error("KafkaException occurred in consumeLoop ${ke.message}", ke)
                healthService.addUnhealthyVote()
            } catch (e: Exception) {
                // Catchall - impliserer at vi skal restarte app
                LOG.error("Uventet Exception i consumerloop, restarter app ${e.message}", e)
                healthService.addUnhealthyVote()
            } finally {
                kafkaConsumer.commitSync(currentPositions.mapValues { (_, offset) -> offsetMetadata(offset) })
                currentPositions.clear()
            }
        }

        kafkaConsumer.close()
    }

    private fun offsetMetadata(offset: Long): OffsetAndMetadata {
        val clientId = kafkaConsumer.groupMetadata().groupInstanceId().map { "\"$it\"" }.orElse("null")
        @Language("JSON")
        val metadata = """{"time": "${LocalDateTime.now()}","groupInstanceId": $clientId}"""
        return OffsetAndMetadata(offset, metadata)
    }
}