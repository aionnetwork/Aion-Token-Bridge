/* eslint-disable */
import FinalityCounter, {FinalityType} from "dto/FinalityCounter";
import {TransferState} from "dto/BridgeTransfer";
import {BundleState} from "network/providers/BridgeApiProvider";
import {BigNumber} from 'bignumber.js';
import * as nc from "lib/NCConstants";

// throws an error code on fail condition. otherwise returns bridge transfer details object;
export const getStateInfo = async (transferState, ethInfo, aionInfo, ethChainTip, aionChainTip) =>
{
  const response = {
    state: TransferState.UNDEFINED,
    finality: new FinalityCounter(FinalityType.UNDEFINED, null)
  };

  if (transferState == null) {
    console.log("[getStateInfo] transferState = null")
    return response;
  }

  console.log("transferState: ", transferState);

  switch (transferState) {
    case BundleState.NOT_FOUND: {
      response.state = TransferState.ETH_SUBMITTED;
      break;
    }
    case BundleState.STORED: {
      response.state = TransferState.BRIDGE_PROCESSING;
      break;
    }
    case BundleState.SUBMITTED: {
      response.state = TransferState.AION_SUBMITTED;
      break;
    }
  }

  switch (response.state) {
    case TransferState.ETH_SUBMITTED: {
      try {
        if (ethChainTip != null && ethInfo != null && ethInfo.ethBlockNumber != null) {
          const nowBlock = new BigNumber(ethChainTip);
          const receiptBlock = new BigNumber(ethInfo.ethBlockNumber);

          const count = nowBlock.minus(receiptBlock);
          response.finality = new FinalityCounter(FinalityType.ETH, count);
        } else {
          console.log("TransferState.ETH_SUBMITTED: passed in null data");
        }
      } catch (e) {
        console.log("could not determine eth finality due to error: ", e);
      }
      break;
    }
    case TransferState.AION_SUBMITTED: {
      try {
        if (aionChainTip != null && aionInfo != null && aionInfo.aionBlockNumber != null) {
          const nowBlock = new BigNumber(aionChainTip);
          const receiptBlock = new BigNumber(aionInfo.aionBlockNumber);

          const count = nowBlock.minus(receiptBlock);
          response.finality = new FinalityCounter(FinalityType.AION, count);

          // change the state to finalized if we've seen confirmation threshold + 5 confirmations
          if (count.gt(new BigNumber(nc.CONFIRMATION_THOLD_AION))) {
            response.state = TransferState.FINALIZED;
          }
        } else {
          console.log("TransferState.AION_SUBMITTED: passed in null data");
        }
      } catch (e) {
        console.log("could not determine eth finality due to error: ", e);
      }
      break;
    }
  }

  return response;
};

