package fund.cyber.pump

import fund.cyber.node.common.Chain
import fund.cyber.node.common.Chain.*
import fund.cyber.pump.bitcoin.BitcoinBlockchainInterface
import fund.cyber.pump.bitcoin.BitcoinKafkaStorageActionTemplateFactory
import fund.cyber.pump.bitcoin_cash.BitcoinCashBlockchainInterface
import fund.cyber.pump.cassandra.SimpleCassandraActionSourceFactory
import fund.cyber.pump.ethereum.EthereumBlockchainInterface
import fund.cyber.pump.ethereum_classic.EthereumClassicBlockchainInterface


object PumpsApplication {

    private val storages: List<StorageInterface> = listOf(
            PumpsContext.elassandraStorage, PumpsContext.kafkaStorage
    )

    @JvmStatic
    fun main(args: Array<String>) {
        PumpsConfiguration.chainsToPump.forEach { chain -> startChainPumper(chain) }
    }


    private fun startChainPumper(chain: Chain) {
        when (chain) {
            BITCOIN -> {
                val actionTemplateFactories = listOf(
                        SimpleCassandraActionSourceFactory(),
                        BitcoinKafkaStorageActionTemplateFactory()
                )
                val flowableInterface = ConcurrentPulledBlockchain(BitcoinBlockchainInterface())
                getChainPumper(flowableInterface, actionTemplateFactories).start()
            }
            BITCOIN_CASH -> {
                val flowableInterface = ConcurrentPulledBlockchain(BitcoinCashBlockchainInterface())
                getChainPumper(flowableInterface, listOf(SimpleCassandraActionSourceFactory())).start()
            }
            ETHEREUM -> {
                val flowableInterface = ConcurrentPulledBlockchain(EthereumBlockchainInterface())
                getChainPumper(flowableInterface, listOf(SimpleCassandraActionSourceFactory())).start()
            }
            ETHEREUM_CLASSIC -> {
                val flowableInterface = ConcurrentPulledBlockchain(EthereumClassicBlockchainInterface())
                getChainPumper(flowableInterface, listOf(SimpleCassandraActionSourceFactory())).start()
            }
        }
    }


    private fun <T : BlockBundle> getChainPumper(
            flowableInterface: FlowableBlockchainInterface<T>, actionFactories: List<StorageActionSourceFactory>
    ): ChainPump<T> {

        return ChainPump(
                blockchainInterface = flowableInterface, storageActionsFactories = actionFactories,
                storages = storages, stateStorage = PumpsContext.elassandraStorage
        )
    }
}
