/* eslint-disable */
import BridgeTransfer, {isValidTransferState} from "dto/BridgeTransfer";
import {BundleState, isValidBundleState} from "network/providers/BridgeApiProvider";
import {bytes32Equals, nc_hexPrefix, nc_isValidByte32} from "lib/NCUtility";

import BridgeError, {BridgeErrorCode} from "dto/BridgeError";
import ReceiptError, {ReceiptErrorCode} from "dto/ReceiptError";

import {getEthInfoFromBridgeApiResponse} from "network/transformers/EthInfoFromBridgeApi";
import {getEthInfoFromRpcTxReceipt} from "network/transformers/EthInfoFromWeb3";
import {getStateInfo} from "network/transformers/StateInfoFromConsolidatedApi";
import {getAionInfoFromBridgeApiResponse} from "network/transformers/AionInfoFromBridgeApi";

export const trackBridgeTranfer = async (ethApi, bridgeApi, ethTxHash, prevEthTx=null) =>
{
  console.log("Ethereum Transaction Hash Queried: "+ethTxHash);

  // check if the transfer is valid
  if (!nc_isValidByte32(ethTxHash))
    throw new ReceiptError(ReceiptErrorCode.INVALID_ETH_TXHASH);

  // used for determining finality
  let ethChainTip = null;
  let aionChainTip = null;

  let bridgeTransfer = null;
  try {
    const bridgeQuery = await bridgeApi.getBatchResponse(ethTxHash);
    console.log("bridgeApi.getBatchResponse("+ethTxHash+")", bridgeQuery);

    bridgeTransfer = bridgeQuery.transaction;

    if (bridgeQuery.status != null && bridgeQuery.status.aion != null)
      aionChainTip = bridgeQuery.status.aion.latestBlockNumber;
  }
  catch (e) {
    console.log("bridge api: call failure", e);
    throw new BridgeError(BridgeErrorCode.BRIDGE_API_CALL_FAILURE);
  }

  if (bridgeTransfer == null || bridgeTransfer.state == null || !isValidBundleState(bridgeTransfer.state)) {
    console.log("bridge api: bridgeTfr processing freak accident");
    throw new BridgeError(BridgeErrorCode.BRIDGE_API_INCONSISTENT_DATA);
  }

  let ethInfo = null;
  let aionInfo = null;

  // fist see if we can find this transaction in the bridge api database
  if (bridgeTransfer.state != BundleState.NOT_FOUND) {
    // transparently re-throw exceptions out of the transformer
    ethInfo = getEthInfoFromBridgeApiResponse(bridgeTransfer.ethInfo);
    aionInfo = getAionInfoFromBridgeApiResponse(bridgeTransfer.aionInfo);

    console.log("ethInfo: ", ethInfo);
    console.log("aionInfo: ", aionInfo);
  }
  // see if we can use the cached value
  else if (prevEthTx != null && prevEthTx.queryString != null && bytes32Equals(prevEthTx.queryString.queryString, ethTxHash)) {
    ethInfo = prevEthTx;
  }
  // hit an Ethereum client for the Eth transaction since we couldn't find it on bridge database
  else {

    console.log("going to ethereum chain for info");
    let ethTxReceipt = null;
    try {
      const ethApiResponse = await ethApi.getTxReceiptAndChainTip(nc_hexPrefix(ethTxHash));
      ethTxReceipt = ethApiResponse.txReceipt;
      ethChainTip = ethApiResponse.chainTip;
    } catch (e) {
      console.log("eth api: call failed: ", e);
      throw new BridgeError(BridgeErrorCode.ETH_API_CALL_FAILURE);
    }

    // don't check for a null receipt, since the proceeding function check for a null receipt received from ethereum
    // re-throw any exceptions that come out of here
    ethInfo = await getEthInfoFromRpcTxReceipt(ethTxReceipt);
  }

  if (ethInfo == null) {
    console.log("bridge api: ethInfo processing freak accident #2");
    throw new BridgeError(BridgeErrorCode.UNDEFINED);
  }

  console.log("bridgeTransfer: ", bridgeTransfer);

  let stateInfo = null;
  try {
    // transferState, ethInfo, aionInfo, aionChainTip, ethChainTip
    stateInfo = await getStateInfo(bridgeTransfer.state, ethInfo, aionInfo, ethChainTip, aionChainTip)
  } catch (e) {
    console.log("Error resolving state info #1", e);
  }

  console.log("stateInfo: ", stateInfo);

  if (stateInfo == null || !isValidTransferState(stateInfo.state)) {
    console.log("Error resolving state info #2");
    throw new BridgeError(BridgeErrorCode.UNDEFINED);
  }

  console.log("returning BridgeTransfer with state: ", stateInfo.state);

  return new BridgeTransfer(stateInfo.state, ethInfo, aionInfo, stateInfo.finality);
};
