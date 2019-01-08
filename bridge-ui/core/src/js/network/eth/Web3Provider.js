import Web3 from 'web3';
import {promisify, validateNonNullUnsignedLong} from "lib/NCUtility";
import TxReceiptAndChainTip from "dto/TxReceiptAndChainTip";

export default class Web3Provider {
  constructor(url) {
    this.web3 = new Web3(new Web3.providers.HttpProvider(url));
  };

  getTxReceipt = async (ethTxHash) => {
    return this.web3.eth.getTransactionReceipt(ethTxHash);
  };

  getBlockNumber = async () => {
    return validateNonNullUnsignedLong(await this.web3.eth.getBlockNumber());
  };

  getTxReceiptAndChainTipAsRpcBatch = async (ethTxHash) => {
    const batch = new this.web3.BatchRequest();
    const promise_blockNumber = promisify(this.web3.eth.getBlockNumber, batch);
    const promise_txReceipt = promisify(this.web3.eth.getTransactionReceipt(ethTxHash), batch);
    batch.execute();

    // noinspection JSCheckFunctionSignatures
    const resp = await Promise.all([promise_blockNumber, promise_txReceipt]);

    /*
    const obj = {};
    obj.chainTip = validateNonNullUnsignedLong(resp[0]);
    obj.txReceipt = resp[1];
    return obj;*/

    return new TxReceiptAndChainTip(validateNonNullUnsignedLong(resp[0]), resp[1]);
  };

  getTxReceiptAndChainTip = async (ethTxHash) => {
    // noinspection JSCheckFunctionSignatures
    const resp = await Promise.all([this.getBlockNumber(), this.getTxReceipt(ethTxHash)]);

    /*
    const obj = {};
    obj.chainTip = validateNonNullUnsignedLong(resp[0]);
    obj.txReceipt = resp[1];
    return obj;*/

    return new TxReceiptAndChainTip(validateNonNullUnsignedLong(resp[0]), resp[1]);
  };
}