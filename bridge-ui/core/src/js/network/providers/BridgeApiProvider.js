/* eslint-disable */
import axios from 'axios';
import {validateNullableUnsignedLong} from "lib/NCUtility";
import * as nc from "lib/NCConstants";
import {BigNumber} from "bignumber.js";

export const BundleState = {
  NOT_FOUND: 'NOT_FOUND',
  STORED: 'STORED',
  SUBMITTED: 'SUBMITTED',
};

export const isValidBundleState = function (state) {
  return (state == BundleState.NOT_FOUND || state == BundleState.STORED || state == BundleState.SUBMITTED)
};

export default class BridgeApiProvider {
  constructor() {
    this.net = axios.create({
      baseURL: nc.API_BRIDGE_BASE,
      timeout: 10000 // 10s default timeout
    });

    this.endpoint = {
      status: {
        link: '/status',
        params: []
      },
      transaction: {
        link: '/transaction',
        params: ['ethTxHash']
      },
      batch: {
        link: '/batch',
        params: ['ethTxHash']
      },
      balance: {
        link: '/balance',
        params: []
      }
    }
  }

  request = async (endpoint, params, timeout=null) => {
    return new Promise((resolve, reject) => {
      let args = { params: {} };

      // set timeout if not null
      if (timeout) { args.timeout = timeout; }

      if (Array.isArray(params)) {
        params.forEach((value, i) => {
          args.params[endpoint.params[i]] = value;
        });
      }

      this.net.get(endpoint.link, args)
        .then((response) => {
          //console.log(response)
          if (response.status == 200 && response.data)
            resolve(response.data);
          else {
            reject("non-200 response or empty body");
          }
        })
        .catch((error) => {
          reject(error);
        });
    });
  };

  // noinspection JSUnresolvedVariable
  getBridgeStatus = async () => {
    const status = await this.request(this.endpoint.status);
    return this.validateStatusResponse(status);
  };

  getEthTxStatus = async (ethTxHash) => {
    const transfer = await this.request(this.endpoint.transaction, [ethTxHash]);
    return this.validateTransactionResponse(transfer);
  };

  getBatchResponse = async (ethTxHash) => {
    const apiResponse = await this.request(this.endpoint.batch, [ethTxHash]);
    const result = {
      transaction: {},
      status: null
    };

    if (apiResponse.transaction == null || apiResponse.status == null)
      throw new Error("batch response failed to respond with correct datastructure");

    result.transaction = this.validateTransactionResponse(apiResponse.transaction);
    try {
      result.status = this.validateAionLatestResponse(apiResponse.status);
    } catch (e) {
      console.log("failed to process the status reponse.", e);
    }

    return result;
  };

  timeout = async (seconds) => {
    return new Promise(resolve => setTimeout(resolve, seconds * 1000));
  };

  isEnoughBridgeBalance = async (requestedTokenBalance) => {
    const status = await this.request(this.endpoint.balance, 5000); // 5s timeout for this request

    if (status.balance == null) {
      console.log("API call response: ["+this.endpoint.balance.link+"]:", status);
      throw new Error("Malformed api response from "+this.endpoint.balance.link);
    }

    let availableAionBalance = null;
    let requestedAionBalance = null;
    try {
      availableAionBalance = (new BigNumber(status.balance, 16)).shiftedBy(-1*nc.AION_COIN_DECIMALS);
      requestedAionBalance = (new BigNumber(requestedTokenBalance, 16)).shiftedBy(-1*nc.ETH_TOKEN_DECIMALS);
    } catch (e) {
      throw new Error("Failed to parse balance api response (#1) from "+this.endpoint.balance.link);
    }

    // noinspection JSUnresolvedFunction
    if (availableAionBalance == null || !BigNumber.isBigNumber(availableAionBalance) || !availableAionBalance.isFinite()) {
      throw new Error("Failed to parse balance api response (#2) from \"+this.endpoint.balance.link");
    }

    // noinspection JSUnresolvedFunction
    if (requestedAionBalance == null || !BigNumber.isBigNumber(requestedAionBalance) || !requestedAionBalance.isFinite()) {
      throw new Error("Invalid input balance provided");
    }

    const isEnough = BigNumber(availableAionBalance).gt(requestedAionBalance);

    console.log("Requested Balance: "+requestedAionBalance.toString(10)+" , Available Balance: "+availableAionBalance.toString(10));
    console.log("isEnoughBridgeBalance: "+isEnough);

    return isEnough;
  };

  validateStatusResponse = (status) => {
    let response = {
      eth: {},
      aion: {}
    };

    if (status.eth == null || status.aion == null) throw new Error("Malformed api response "+this.endpoint.status.link);

    response.eth.finalizedBlockNumber = validateNullableUnsignedLong(status.eth.finalizedBlockNumber);
    response.eth.finalizedBundleId = validateNullableUnsignedLong(status.eth.finalizedBundleId);

    response.aion.latestBlockNumber = validateNullableUnsignedLong(status.aion.latestBlockNumber);
    response.aion.finalizedBundleId = validateNullableUnsignedLong(status.aion.finalizedBundleId);
    response.aion.finalizedBlockNumber = validateNullableUnsignedLong(status.aion.finalizedBlockNumber);

    return response;
  };

  validateAionLatestResponse = (status) => {
    let response = {
      aion: {}
    };

    if (status.aion == null) throw new Error("Malformed api response "+this.endpoint.status.link);

    response.aion.latestBlockNumber = validateNullableUnsignedLong(status.aion.latestBlockNumber);

    return response;
  };

  validateTransactionResponse = (transfer) => {
    if (transfer.state == null) throw new Error("Malformed api response from "+this.endpoint.transaction.link);
    return transfer;
  }
}

















