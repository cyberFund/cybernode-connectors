package fund.cyber.pump.bitcoin.client

import fund.cyber.search.model.bitcoin.JsonRpcBitcoinBlock
import fund.cyber.search.model.bitcoin.JsonRpcBitcoinTransaction
import fund.cyber.search.model.bitcoin.RegularTransactionInput
import io.micrometer.core.instrument.MeterRegistry
import org.ehcache.Cache
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong


private val log = LoggerFactory.getLogger(JsonRpcBlockToBitcoinBundleConverter::class.java)!!

//todo add cache
@Component
class JsonRpcBlockToBitcoinBundleConverter(
        private val client: BitcoinJsonRpcClient,
        private val txCache: Cache<String, JsonRpcBitcoinTransaction>? = null,
        monitoring: MeterRegistry
) {

    val inputTxesFromCache = monitoring.gauge("input_txes_from_cache", AtomicLong(0L))!!
    val totalInputTxes = monitoring.gauge("total_input_txes", AtomicLong(0L))!!

    private val transactionConverter = JsonRpcToDaoBitcoinTxConverter()
    private val blockConverter = JsonRpcToDaoBitcoinBlockConverter()


    fun convertToBundle(jsonRpcBlock: JsonRpcBitcoinBlock): BitcoinBlockBundle {

        jsonRpcBlock.tx.forEach { tx -> txCache?.put(tx.txid, tx) }

        val inputTransactions = getTransactionsInputs(jsonRpcBlock)
        val transactions = transactionConverter.convertToDaoTransactions(jsonRpcBlock, inputTransactions)
        val block = blockConverter.convertToDaoBlock(jsonRpcBlock, transactions)

        return BitcoinBlockBundle(
                hash = jsonRpcBlock.hash, parentHash = jsonRpcBlock.previousblockhash ?: "-1",
                number = jsonRpcBlock.height, block = block, transactions = transactions,
                blockSize = jsonRpcBlock.size
        )
    }


    private fun getTransactionsInputs(jsonRpcBlock: JsonRpcBitcoinBlock): List<JsonRpcBitcoinTransaction> {

        val incomingNonCoinbaseTransactionsIds = jsonRpcBlock.tx
                .flatMap { transaction -> transaction.vin }
                .filter { txInput -> txInput is RegularTransactionInput }
                .map { txInput -> (txInput as RegularTransactionInput).txid }

        if (incomingNonCoinbaseTransactionsIds.isEmpty()) return emptyList()

        if (txCache != null) {

            val txs = mutableListOf<JsonRpcBitcoinTransaction>()
            val idsWithoutCacheHit = mutableListOf<String>()

            for (id in incomingNonCoinbaseTransactionsIds) {
                val tx = txCache[id]
                if (tx != null) txs.add(tx) else idsWithoutCacheHit.add(id)
            }

            log.debug("Transactions - Total ids: ${incomingNonCoinbaseTransactionsIds.size}, Cache hits: ${txs.size}")
            totalInputTxes.set(incomingNonCoinbaseTransactionsIds.size.toLong())
            inputTxesFromCache.set(txs.size.toLong())

            txs.addAll(client.getTxes(idsWithoutCacheHit))
            return txs
        }

        return client.getTxes(incomingNonCoinbaseTransactionsIds)
    }
}
