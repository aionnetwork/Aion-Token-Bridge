/* eslint-disable */
import {canonicalizeBytes32, canonicalizeEthAddr, validateNonNullUnsignedLong} from "lib/NCUtility";
import EthInfo from "dto/EthInfo";
import * as nc from "lib/NCConstants";
import BridgeError, {BridgeErrorCode} from "dto/BridgeError";
import ReceiptError, {ReceiptErrorCode} from "dto/ReceiptError";
import {BigNumber} from "bignumber.js";

export const getEthInfoFromBridgeApiResponse = (info) => {
  if (info == null) return null;

  if (info.ethBlockHash == null || info.ethAddress == null || info.aionAddress == null ||
      info.ethBlockNumber == null || info.aionTransferAmount == null) {
    throw new BridgeError(BridgeErrorCode.BRIDGE_API_INCONSISTENT_DATA);
  }

  // noinspection JSUnusedAssignment
  let ethInfo = null;

  let ethTxHash = canonicalizeBytes32(info.ethTxHash);
  let ethBlockHash = canonicalizeBytes32(info.ethBlockHash);
  let fromEthAddr = canonicalizeEthAddr(info.ethAddress);
  let toAionAddr = canonicalizeBytes32(info.aionAddress);

  // process eth block number
  let ethBlockNumber = null;
  try {
    ethBlockNumber = validateNonNullUnsignedLong(info.ethBlockNumber)
  } catch (e) {
    console.log("bridge api: invalid eth block number");
    throw new BridgeError(BridgeErrorCode.BRIDGE_API_INCONSISTENT_DATA);
  }

  if (ethBlockNumber == null) {
    console.log("bridge api: block number processing freak accident");
    throw new BridgeError(BridgeErrorCode.UNDEFINED);
  }

  // process value field
  let value = null;
  try {
    // noinspection JSUnresolvedVariable
    value = new BigNumber(info.aionTransferAmount, 16); // interpret value transmitted by bridge api
  } catch (e) {
    console.log("eth api: token amount processing error #1", e);
    throw new BridgeError(BridgeErrorCode.ETH_API_INCONSISTENT_DATA);
  }
  // noinspection JSUnresolvedFunction
  if (!(value != null && BigNumber.isBigNumber(value) && value.isFinite())) {
    console.log("eth api: token amount processing error #2");
    throw new BridgeError(BridgeErrorCode.ETH_API_INCONSISTENT_DATA);
  }

  value = value.shiftedBy(-1*nc.AION_COIN_DECIMALS);

  if (!value.gt(0)) {
    console.log("bridge api: token amount lte 0");
    throw new ReceiptError(ReceiptErrorCode.RECEIPT_ZERO_VALUE);
  }

  ethInfo = new EthInfo(fromEthAddr, toAionAddr, value.toFixed(null), ethTxHash, ethBlockNumber, ethBlockHash);

  // noinspection JSValidateTypes
  if (ethInfo == null) {
    console.log("bridge api: ethInfo processing freak accident #2");
    throw new BridgeError(BridgeErrorCode.UNDEFINED);
  }

  return ethInfo;
};
