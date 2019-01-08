export default class AionInfo {
  constructor(txHash, blockNumber, blockHash) {
    this.aionTxHash = txHash;
    this.aionBlockNumber = blockNumber;
    this.aionBlockHash = blockHash;
  }
}