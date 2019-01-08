/* eslint-disable */
export const TransferState = {
  UNDEFINED: -1,
  ETH_SUBMITTED: "ETH_SUBMITTED", // submitted to eth, awaiting finalization
  BRIDGE_PROCESSING: "BRIDGE_PROCESSING", // finalized on eth, being processed by bridge
  AION_SUBMITTED: "AION_SUBMITTED", // submitted to aion, awaiting finalization
  FINALIZED: "FINALIZED" // transfer finalized according to bridge
};

export const isValidTransferState = function (state) {
  return (state == TransferState.ETH_SUBMITTED || state == TransferState.BRIDGE_PROCESSING ||
          state == TransferState.AION_SUBMITTED || state == TransferState.FINALIZED)
};

export default class BridgeTransfer {
  constructor(state, ethInfo, aionInfo, finality) {
    this.state = state;
    this.ethInfo = ethInfo;
    this.aionInfo = aionInfo;
    this.finality = finality;
  }
}





















