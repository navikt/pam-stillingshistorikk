package no.nav.arbeidsplassen.stihibi

import io.micronaut.configuration.kafka.annotation.KafkaListener
import io.micronaut.configuration.kafka.annotation.OffsetReset
import io.micronaut.configuration.kafka.annotation.OffsetStrategy
import io.micronaut.configuration.kafka.annotation.Topic
import io.micronaut.context.annotation.Requires
import org.apache.kafka.clients.consumer.Consumer
import org.slf4j.LoggerFactory


@Requires(property = "adlistener.enabled", value = "true")
@KafkaListener(clientId = AD_LISTENER_CLIENT_ID, groupId = "\${adlistener.group-id:pam-stihibi}", threads = 1, offsetReset = OffsetReset.EARLIEST,
        batch = true, offsetStrategy = OffsetStrategy.DISABLED)
class AdTopicListenerDepricated(private val bigQueryService: BigQueryService, private val kafkaStateRegistry: KafkaStateRegistry) {

    companion object {
        private val LOG = LoggerFactory.getLogger(AdTopicListenerDepricated::class.java)
    }


    @Topic("\${adlistener.topic:teampam.stilling-historikk}")
    fun receive(ads: List<AdTransport>, offsets: List<Long>, partitions: List<Int>, topics: List<String>, kafkaconsumer: Consumer<*, *>) {
        LOG.info("Received batch with {} ads", ads.size)
        if (kafkaStateRegistry.hasError()) {
            LOG.error("Kafka state is set to error, skipping this batch to avoid message loss. Consumer should be set to pause")
            return
        }
        if (ads.isNotEmpty()) {
            val response = bigQueryService.sendBatch(ads, offsets, partitions, topics)
            if (response.hasError) {
                LOG.error("We got error while inserting to bigquery, rows failed {}", response.rowsError)
                LOG.error("failed at start batch offset ${offsets[0]} partition ${partitions[0]}")
                throw Throwable("Rows inserts failed!")
            }
            LOG.info("Insert successfully, committing offset")
            kafkaconsumer.commitSync()
        }
     }
}

const val AD_LISTENER_CLIENT_ID="pam-stihibi-ad-topic-listener"
