/* eslint-disable */
import React from 'react';
import moment from 'moment';
import {TransferState} from "dto/BridgeTransfer";
import FinalityCounter, {FinalityType} from "dto/FinalityCounter";
import {bytes32Equals} from "lib/NCUtility";
import {BigNumber} from "bignumber.js";

export const Reset = (data) => {
  return {
    type: 'TRACK_RESET',
    data: data,
  }
};

// input of some Error type
export const SetFullPageError = (data) => {
  return {
    type: 'TRACK_FULL_PAGE_ERROR',
    data: data,
  }
};

// input of type BridgeTransfer;
export const SetTransfer = (data) => {
  return {
    type: 'TRACK_SET_TRANSFER',
    data: data,
  }
};

const init =
{
  // data model
  momentUpdated: null,
  fullPageError: null,
  queryString: "1537c7a67540a73108f0ed758944fb11774aa0697c3c6bca96d45ca4ede25455",
  queryNonce: 0,
  transfer: {
    state: TransferState.ETH_SUBMITTED,
    finality: new FinalityCounter(FinalityType.ETH, 30),
    ethInfo: {
      ethAddress: "0x42516209ad4797a78a5dee81fdafee1fb4ee6835",
      aionAddress: "0xb9eb2d7cd5051f4531dbe4b4eb5971120db1b348d4237d93844185e8bd5934a5",
      aionTransferAmount: (new BigNumber("465934.0001")).toFixed(null),
      ethTxHash: "1537c7a67540a73108f0ed758944fb11774aa0697c3c6bca96d45ca4ede25455",
      ethBlockNumber: 500,
      ethBlockHash: "1537c7a67540a73108f0ed758944fb11774aa0697c3c6bca96d45ca4ede25455"
    },
    aionInfo: {
      aionTxHash: '0x3c0048f3572f96f920ad6c27e0e3c1cb61abc6a3abc7863ab756d5dea0c320c8',
      aionBlockNumber: 1000,
      aionBlockHash: "0xffa479679f1351b548ac11c9956c1cecb89b8fd5b084ba048872d02df95e2395"
    }
  }
};

export function reducer_track (state=JSON.parse(JSON.stringify(init)), action)
{
  switch(action.type)
  {
    case 'TRACK_RESET': {
      const _prevState = Object.assign({}, state);
      const _state = JSON.parse(JSON.stringify(init));
      _state.queryString = action.data.queryString;
      _state.queryNonce = action.data.queryNonce;
      return _state;
    }
    case 'TRACK_FULL_PAGE_ERROR': {
      const _state = Object.assign({}, state);
      _state.fullPageError = action.data;
      _state.momentUpdated = moment();
      return _state
    }
    case 'TRACK_SET_TRANSFER': {
      const _state = Object.assign({}, state);

      const transfer = action.data;

      if ((transfer != null && transfer.ethInfo != null) === false) {
        console.log("TRACK_SET_TRANSFER: passed in null transfer value");
        return _state;
      }

      if (!bytes32Equals(transfer.ethInfo.ethTxHash, _state.queryString)) {
        console.log("TRACK_SET_TRANSFER: spurious input; ethTxHash: "+ transfer.ethInfo.ethTxHash)
        return _state;
      }

      _state.transfer = transfer;
      _state.momentUpdated = moment();

      return _state;
    }
    default: {
      //console.log("encountered unknown action.type: "+action.type);
      return state;
    }
  }
}










