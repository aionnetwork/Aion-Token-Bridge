/* eslint-disable */
import {bytes32Equals, ethAddressEquals, validateNonNullUnsignedLong, canonicalizeEthAddr, canonicalizeBytes32} from "lib/NCUtility";
import EthInfo from "dto/EthInfo";
import BridgeError, {BridgeErrorCode} from "dto/BridgeError";
import ReceiptError, {ReceiptErrorCode} from "dto/ReceiptError";
import {BigNumber} from 'bignumber.js';
import * as nc from "lib/NCConstants";
import {nc_sanitizeHex} from "lib/NCUtility";

// throws an error code on fail condition. otherwise returns bridge transfer details object;
export const getEthInfoFromRpcTxReceipt = async (ethTxReceipt) =>
{
  if (ethTxReceipt == null)
    throw new ReceiptError(ReceiptErrorCode.RECEIPT_NOT_AVAILABLE);

  if (ethTxReceipt.status != null && !BigNumber(ethTxReceipt.status).eq(1)) {
    console.log("eth api: receipt with failed status: " + ethTxReceipt.status);
    throw new ReceiptError(ReceiptErrorCode.RECEIPT_WITH_FAILED_STATUS);
  }

  if (ethTxReceipt.to == null) {
    console.log("eth api: ethTxReceipt.to should not be null");
    throw new BridgeError(BridgeErrorCode.ETH_API_INCONSISTENT_DATA);
  }

  if (!ethAddressEquals(ethTxReceipt.to, nc.TOKEN_ADDR)) {
    console.log("eth api: ethTxReceipt.to should be aion token address", ethTxReceipt.to + " != " + nc.TOKEN_ADDR);
    throw new ReceiptError(ReceiptErrorCode.RECEIPT_INCORRECT_TO_ADDR);
  }

  // input is not part of the receipt and is pulled from the transaction.
  // if not available, don't fret.
  if (ethTxReceipt.input != null) {
    const input = nc_sanitizeHex(ethTxReceipt.input);
    if (input.length < 8 || input.substring(0, 8).toLowerCase() != nc_sanitizeHex(nc.TOKEN_BURN_FUNCTION_HASH).toLowerCase()) {
      throw new ReceiptError(ReceiptErrorCode.RECEIPT_NOT_BURN_FUNCTION);
    }
  }

  if (!(ethTxReceipt.logs != null && Array.isArray(ethTxReceipt.logs))) {
    console.log("eth api: ethTxReceipt.logs should be an array", ethTxReceipt.logs);
    throw new BridgeError(BridgeErrorCode.ETH_API_INCONSISTENT_DATA);
  }

  if (ethTxReceipt.logs.length > nc.TOKEN_BURN_TX_MAX_LOGS) {
    console.log("eth api: ethTxReceipt.logs.length >  TOKEN_BURN_TX_MAX_LOGS ("+nc.TOKEN_BURN_TX_MAX_LOGS+")");
    throw new ReceiptError(ReceiptErrorCode.RECEIPT_NO_BURN_LOG);
  }

  if (ethTxReceipt.blockHash == null || ethTxReceipt.transactionHash == null) {
    console.log("ethTxReceipt.block or ethTxReceipt.transactionHash are null");
    throw new BridgeError(BridgeErrorCode.ETH_API_INCONSISTENT_DATA);
  }

  let ethTxHash = canonicalizeBytes32(ethTxReceipt.transactionHash);
  let ethBlockHash = canonicalizeBytes32(ethTxReceipt.blockHash);

  // ok to not use bloom here since we don't expect a large number events emitted here.
  for (let i=0; i< ethTxReceipt.logs.length; i++) {
    const log = ethTxReceipt.logs[i];

    if (log.topics != null && Array.isArray(log.topics) && log.topics[0] != null) {
      if (!bytes32Equals(log.topics[0], nc.TOKEN_BURN_EVENT_HASH)) {
        continue;
      }

      // ok so now the event signature matches;
      // do a bunch of checks to sanitize the input data.

      // noinspection JSUnresolvedVariable
      if (log.removed != null && (log.removed === true || log.removed === 'true')) {
        console.log("eth api: log claims to be removed from mainchain");
        throw new ReceiptError(ReceiptErrorCode.RECEIPT_NON_MAINCHAIN);
      }

      // sanity check: log was emitted by token contract
      if (!bytes32Equals(log.address, nc.TOKEN_ADDR)) {
        console.log("eth api: log.address should == token contract address", log.address + " != " + nc.TOKEN_ADDR);
        throw new BridgeError(BridgeErrorCode.ETH_API_INCONSISTENT_DATA);
      }

      if (log.topics[1] == null || log.topics[2] == null) {
        console.log("eth api: log fields are null");
        throw new BridgeError(BridgeErrorCode.ETH_API_INCONSISTENT_DATA);
      }

      let fromEthAddr = canonicalizeEthAddr(log.topics[1]);
      let toAionAddr = canonicalizeBytes32(log.topics[2]);

      // process eth block number
      let ethBlockNumber = null;
      try {
        ethBlockNumber = validateNonNullUnsignedLong(log.blockNumber)
      } catch (e) {
        console.log(e);
        return new BridgeError(BridgeErrorCode.ETH_API_INCONSISTENT_DATA);
      }
      if (ethBlockNumber == null) {
        console.log("eth api: eth blockNumber is null freak accident");
        throw new BridgeError(BridgeErrorCode.UNDEFINED);
      }

      // process value field
      if (log.data == null) {
        console.log("eth api: log.data == null");
        throw new BridgeError(BridgeErrorCode.ETH_API_INCONSISTENT_DATA);
      }
      let value = null;
      try {
        console.log("Ethereum Log of Interest: ", log);
        const sanitizedLogData = nc_sanitizeHex(log.data);
        if (sanitizedLogData.length < 64) {
          throw new BridgeError(BridgeErrorCode.ETH_API_INCONSISTENT_DATA);
        }
        console.log("Sanitized Log Data: "+sanitizedLogData);
        console.log("Substring of Interest: "+sanitizedLogData.substring(32, 64));

        value = new BigNumber(sanitizedLogData.substring(32, 64), 16); // first 16 bytes of data should be value

        console.log("Derived Value: "+value.toString(10));

      } catch (e) {
        console.log("eth api: token amount processing error #1", e);
        throw new BridgeError(BridgeErrorCode.ETH_API_INCONSISTENT_DATA);
      }
      // noinspection JSUnresolvedFunction
      if (!(value != null && BigNumber.isBigNumber(value) && value.isFinite())) {
        console.log("eth api: token amount processing error #2");
        throw new BridgeError(BridgeErrorCode.ETH_API_INCONSISTENT_DATA);
      }

      value = value.shiftedBy(-1*nc.ETH_TOKEN_DECIMALS);

      if (!value.gt(0)) {
        console.log("bridge api: token amount lte 0");
        throw new ReceiptError(ReceiptErrorCode.RECEIPT_ZERO_VALUE);
      }

      // from, to, value, txHash, blockNumber, blockHash
      return new EthInfo(fromEthAddr, toAionAddr, value.toFixed(null), ethTxHash, ethBlockNumber, ethBlockHash);
    } else {
      console.log("log topics malformed: ", log);
    }
  }

  console.log("No burn log found in log list: ", ethTxReceipt.logs);
  throw new ReceiptError(ReceiptErrorCode.RECEIPT_NO_BURN_LOG);
};

