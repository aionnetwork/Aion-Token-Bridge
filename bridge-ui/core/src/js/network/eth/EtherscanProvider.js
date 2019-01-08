/* eslint-disable */
import axios from 'axios';
import {validateNonNullUnsignedLong} from "lib/NCUtility";
import TxReceiptAndChainTip from "dto/TxReceiptAndChainTip";

export default class EtherscanProvider {
  constructor(baseUrl) {
    this.baseUrl = baseUrl;
    this.net = axios.create({
      baseURL: baseUrl,
      timeout: 5000 // 5s timeout
    })
  }

  request = (callForm) => {
    callForm.jsonrpc = "2.0";
    callForm.id = "0";

    const options = {
      method: 'get',
      headers: {
        'Content-Type': 'application/json'
      },
      params: callForm
    };

    return new Promise((resolve, reject) => {
      this.net(options).then(response => {
        if (response == null) {
          reject("Received empty response object");
          return;
        }

        if (response.status != 200) {
          reject("Received non-200 status code");
          return;
        }

        if (response.data == null) {
          reject("Rpc endpoint returned bad response");
          return;
        }

        if (response.data.error != null) {
          console.log(response.data.error);
          reject("Rpc endpoint responded with error");
          return;
        }

        if (response.data.result === undefined) {
          reject("Rpc endpoint returned non-error undefined result");
          return;
        }

        resolve(response.data.result);

      }).catch(err => {
        console.log(err);
        reject("Caught top-level error in json rpc call.");
      });
    });
  };

  getTxReceipt = async (ethTxHash) => {
    const r = await Promise.all([
      this.request({
        'module': 'proxy',
        'action': 'eth_getTransactionReceipt',
        'txhash': ethTxHash
      }),
      this.request({
        'module': 'proxy',
        'action': 'eth_getTransactionByHash',
        'txhash': ethTxHash
      })
    ]);

    const receipt = r[0];
    const transaction = r[1];

    if (receipt != null && transaction != null) {
      receipt.input = transaction.input;

      if (receipt.to == null)
        receipt.to = transaction.to;

      if (receipt.from == null)
        receipt.from = transaction.from;
    }

    console.log("Receipt from ["+this.baseUrl+"]", receipt);

    return receipt;
  };

  getBlockNumber = async () => {
    const r = await this.request({
      'module': 'proxy',
      'action': 'eth_blockNumber'
    });

    return validateNonNullUnsignedLong(r);
  };

  getTxReceiptAndChainTip = async (ethTxHash) => {
    // noinspection JSCheckFunctionSignatures
    const r = await Promise.all([this.getBlockNumber(), this.getTxReceipt(ethTxHash)]);

    return new TxReceiptAndChainTip(r[0], r[1]);
  };
}