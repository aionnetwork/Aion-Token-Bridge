export default class EthInfo {
  constructor(from, to, value, txHash, blockNumber, blockHash) {
    this.ethTxHash = txHash;
    this.ethBlockNumber = blockNumber;
    this.ethBlockHash = blockHash;

    this.ethAddress = from;
    this.aionAddress = to;
    this.aionTransferAmount = value;
  }
}