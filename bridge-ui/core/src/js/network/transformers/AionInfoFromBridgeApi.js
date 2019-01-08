/* eslint-disable */
import {canonicalizeBytes32, validateNonNullUnsignedLong} from "lib/NCUtility";
import BridgeError, {BridgeErrorCode} from "dto/BridgeError";
import AionInfo from "dto/AionInfo";

export const getAionInfoFromBridgeApiResponse = (info) => {
  if (info == null) return null;

  if (info.aionTxHash == null || info.aionBlockHash == null || info.aionBlockNumber == null) {
    throw new BridgeError(BridgeErrorCode.BRIDGE_API_INCONSISTENT_DATA);
  }

  let aionTxHash = canonicalizeBytes32(info.aionTxHash);
  let aionBlockHash = canonicalizeBytes32(info.aionBlockHash);

  // process eth block number
  let aionBlockNumber = null;
  try {
    aionBlockNumber = validateNonNullUnsignedLong(info.aionBlockNumber)
  } catch (e) {
    console.log("bridge api: failed to process aion block number", e);
    throw new BridgeError(BridgeErrorCode.BRIDGE_API_INCONSISTENT_DATA);
  }

  if (aionBlockNumber == null) {
    console.log("bridge api: aion block number processing freak accident");
    throw new BridgeError(BridgeErrorCode.UNDEFINED);
  }
  // txHash, blockNumber, blockHash
  return new AionInfo(aionTxHash, aionBlockNumber, aionBlockHash);
};
