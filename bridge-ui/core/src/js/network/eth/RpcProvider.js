/* eslint-disable */
import axios from 'axios';
import {validateNonNullUnsignedLong} from "lib/NCUtility";
import TxReceiptAndChainTip from "dto/TxReceiptAndChainTip";

export default class RpcProvider {
  constructor(url) {
    this.baseUrl = url;
    this.net = axios.create({
      baseURL: url,
      timeout: 5000 // 5s timeout
    })
  }

  request = (callForm) => {
    callForm.jsonrpc = "2.0";
    callForm.id = "0";

    const options = {
      method: 'post',
      headers: {
        'Content-Type': 'application/json'
      },
      data: callForm
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



  batchRequest = (payload) => {
    console.log("payload: ", payload);
    const options = {
      method: 'post',
      headers: {
        'Content-Type': 'application/json'
      },
      data: payload
    };

    return new Promise((resolve, reject) => {
      this.net(options)
        .then(response => {
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

          if (!Array.isArray(response.data)) {
            console.log(response.data);
            reject("Received non-array body response for batch call");
            return;
          }

          const rpcResponse = [];

          // all or nothing. either all calls are successful or failure
          response.data.forEach((v, i) => {
            if (v.error != null) {
              console.log(response.data.error);
              reject("Rpc endpoint responded with error");
              return;
            }

            if (v.id == null) {
              reject("Rpc endpoint returned bad id");
              return;
            }

            if (v.result === undefined) {
              reject("Rpc endpoint returned non-error undefined result");
              return;
            }

            rpcResponse[v.id] = v.result;
          });

          resolve(rpcResponse);
        })
        .catch(err => {
          console.log(err);
          reject("Caught top-level error in json rpc call.");
        });
    });
  };

  getTxReceipt = async (ethTxHash) => {
    const r = await this.request({
      method: "eth_getTransactionReceipt",
      params: [ethTxHash]
    });

    return r;
  };

  getTransaction = async (ethTxHash) => {
    const r = await this.request({
      method: "eth_getTransactionByHash",
      params: [ethTxHash]
    });

    return r;
  };

  getBlockNumber = async () => {
    const r = await this.request({
      method: "eth_blockNumber",
      params: []
    });

    return validateNonNullUnsignedLong(r);
  };

  getTxReceiptAndChainTipAsRpcBatch = async (ethTxHash) => {
    const batch = [];
    batch.push({
      jsonrpc: "2.0",
      id: 0,
      method: "eth_blockNumber",
      params: []
    });
    batch.push({
      jsonrpc: "2.0",
      id: 1,
      method: "eth_getTransactionReceipt",
      params: [ethTxHash]
    });
    batch.push({
      jsonrpc: "2.0",
      id: 2,
      method: "eth_getTransactionByHash",
      params: [ethTxHash]
    });

    const resp = await this.batchRequest(batch);

    const blockNumber = validateNonNullUnsignedLong(resp[0]);
    const receipt = resp[1];
    const transaction = resp[2];

    if (receipt != null && transaction != null) {
      receipt.input = transaction.input;

      if (receipt.to == null)
        receipt.to = transaction.to;

      if (receipt.from == null)
        receipt.from = transaction.from;
    }

    console.log("Receipt from ["+this.baseUrl+"]", receipt);

    return new TxReceiptAndChainTip(blockNumber, receipt);
  };

  getTxReceiptAndChainTip = async (ethTxHash) => {
    // noinspection JSCheckFunctionSignatures
    const resp = await Promise.all([this.getBlockNumber(), this.getTxReceipt(ethTxHash)]);

    return new TxReceiptAndChainTip(resp[0], resp[1]);
  };
}