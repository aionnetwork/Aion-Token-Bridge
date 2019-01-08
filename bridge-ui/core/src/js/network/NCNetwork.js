/* eslint-disable */
import * as StoreTrack from "stores/StoreTrack";
import {trackBridgeTranfer} from "network/BridgeTracker";
import ReceiptError from "dto/ReceiptError";
import BridgeApiProvider from "network/providers/BridgeApiProvider";
import EthApiProvider from "network/providers/EthApiProvider";
import {bytes32Equals} from "lib/NCUtility";
import {store} from 'stores/NCReduxStore';
import {TransferState} from "dto/BridgeTransfer";
import * as nc from "lib/NCConstants";

// Constants
// ---------

export const bridgeApi = new BridgeApiProvider();
export const ethApi = new EthApiProvider();

let isTrackerPageVisible = true;
export function setTrackerPageVisiblity(x) {
  isTrackerPageVisible = x;
}

/**
 * function called by UI to track a particular transaction hash through completion.
 */
export const track = async (ethTxHash, queryNonce, failureCallback) => {
  console.log("track called for queryString=["+ethTxHash+"] & queryNonce=["+queryNonce+"]");

  const transferLabel = "pollTransfer("+ethTxHash+")";
  const finalityLabel = "pollFinality("+ethTxHash+")";

  let silentlyStopPolling = false;
  let errCount = 0;

  const shouldModifyStore = () => {
    console.log("store.getState().track.queryNonce: ", store.getState().track.queryNonce);
    console.log("this.queryNonce: "+queryNonce);
    return (bytes32Equals(ethTxHash, store.getState().track.queryString) && (store.getState().track.queryNonce == queryNonce));
  };

  const safeStoreMutation = (mutation) => {
    if (shouldModifyStore()) {
      mutation();
      return true;
    }

    return false;
  };

  const setTransfer = (transfer) => {
    return safeStoreMutation(() => store.dispatch(StoreTrack.SetTransfer(transfer)));
  };

  const setFullPageError = (error) => {
    return safeStoreMutation(() => store.dispatch(StoreTrack.SetFullPageError(error)));
  };

  const pollTransfer = () => {
    console.log(transferLabel + " called");

    // silently stop polling
    if (silentlyStopPolling === true) {
      console.log("silently stop polling for Ethereum Transaction Hash: " + ethTxHash);
      return;
    }

    // call the failure callback to notify user something bad happened
    if (errCount > nc.POLL_TRANSFER_MAX_ERROR) {
      console.log("exceeded error count");
      try {
        failureCallback();
      } catch (e) {
        console.log("failed to call error callback");
      }
      return;
    }

    if (!isTrackerPageVisible) {
      console.log(transferLabel + ": tracker page not visible. empty poll.");
      setTimeout(pollTransfer, 1000); // we can empty poll fast since there is no real processing happening here.
      return;
    }

    trackBridgeTranfer(ethApi, bridgeApi, ethTxHash)
      .then(transfer => {
        if (setTransfer(transfer)) {
          if (transfer != null && transfer.state == TransferState.FINALIZED) {
            silentlyStopPolling = true;
          }
          errCount = 0; // reset errCount
        } else {
          console.log("setTransfer detected unsafe store mutation. Store no-longer has our Ethereum Transaction Hash of-interest.");
          silentlyStopPolling = true;
        }
      })
      .catch(e => {
        if (e instanceof ReceiptError) {
          if (!setFullPageError(e)) {
            console.log("setFullPageError detected unsafe store mutation. Store no-longer has our Ethereum Transaction Hash of-interest.");
          }
          silentlyStopPolling = true; // regardless of safe mutation status, stop polling this one
        } else {
          console.log(transferLabel + ": unrecoverable error", e);
          errCount++;
        }
      })
      .finally(() => {
        console.log("trying again ...");
        setTimeout(pollTransfer, nc.POLL_TRANSFER_TIMEOUT);
      });
  };

  pollTransfer();

  return "started pollTransfer & pollFinality for tx: "+ethTxHash;
};