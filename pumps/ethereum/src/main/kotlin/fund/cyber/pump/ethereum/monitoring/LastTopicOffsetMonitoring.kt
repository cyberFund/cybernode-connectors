package fund.cyber.pump.ethereum.monitoring

import fund.cyber.common.kafka.readTopicLastOffset
import fund.cyber.search.model.chains.EthereumFamilyChain
import fund.cyber.search.model.events.blockPumpTopic
import fund.cyber.search.model.events.txPumpTopic
import fund.cyber.search.model.events.unclePumpTopic
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import org.apache.kafka.clients.consumer.Consumer
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

private val log = LoggerFactory.getLogger(LastTopicOffsetMonitoring::class.java)!!

private const val LAST_NETWORK_BLOCK_NUMBER_TIMEOUT = 10 * 1000L

@Component
class LastTopicOffsetMonitoring(
        monitoring: MeterRegistry,
        private val consumer: Consumer<Any, Any>,
        private val chain: EthereumFamilyChain
) {

    private val lastTxTopicOffsetMonitor = monitoring.gauge("pump_topic_last_offset",
            Tags.of("topic", chain.txPumpTopic), AtomicLong(consumer.readTopicLastOffset(chain.txPumpTopic)))!!

    private val lastBlockTopicOffsetMonitor = monitoring.gauge("pump_topic_last_offset",
            Tags.of("topic", chain.blockPumpTopic), AtomicLong(consumer.readTopicLastOffset(chain.blockPumpTopic)))!!

    private val lastUncleTopicOffsetMonitor = monitoring.gauge("pump_topic_last_offset",
            Tags.of("topic", chain.unclePumpTopic), AtomicLong(consumer.readTopicLastOffset(chain.unclePumpTopic)))!!

    @Scheduled(fixedRate = LAST_NETWORK_BLOCK_NUMBER_TIMEOUT)
    fun getLastNetworkBlockNumber() {
        try {
            val lastTxTopicOffset = consumer.readTopicLastOffset(chain.txPumpTopic)
            lastTxTopicOffsetMonitor.set(lastTxTopicOffset)

            val lastBlockTopicOffset = consumer.readTopicLastOffset(chain.blockPumpTopic)
            lastBlockTopicOffsetMonitor.set(lastBlockTopicOffset)

            val lastUncleTopicOffset = consumer.readTopicLastOffset(chain.unclePumpTopic)
            lastUncleTopicOffsetMonitor.set(lastUncleTopicOffset)
        } catch (e: Exception) {
            log.error("Error getting last network block number", e)
        }
    }

}
