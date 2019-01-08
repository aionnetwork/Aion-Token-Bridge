/* eslint-disable */
import React, {Component} from 'react';
import NCExplorerPage from 'components/common/NCExplorerPage';
import {connect} from "react-redux";
import * as StoreTrack from "stores/StoreTrack";
import BridgeApiProvider from "network/providers/BridgeApiProvider";
import EthApiProvider from "network/providers/EthApiProvider";
import * as network from "network/NCNetwork";
import PageVisibility from 'react-page-visibility';
import NCExplorerHead from "components/common/NCExplorerHead";
import ValidationErrorDesc from "components/widgets/ValidationErrorDesc";
import StateMap from "components/widgets/StateMap";
import NCEntityLabel from "components/common/NCEntityLabel";
import {NCEntity} from "lib/NCEnums";
import * as nc from "lib/NCConstants";
import {nc_hexPrefix, nc_trim} from "lib/NCUtility";
import {Card, Elevation, Icon, Intent, Position, Tag, Tooltip} from "@blueprintjs/core";
import BigNumber from "bignumber.js";
import {TransferState} from "dto/BridgeTransfer";
import moment from "moment";
import {StageState} from "components/common/Types";
import {FinalityType} from "dto/FinalityCounter";
import BridgeError, {BridgeErrorCode} from "dto/BridgeError";
import NCToaster from "components/layout/NCToaster";


class NCTrack extends Component
{
  constructor(props) {
    super(props);

    this.bridgeApi = new BridgeApiProvider();
    this.ethApi = new EthApiProvider();
  }

  // ----------------------------------------------------------
  // Network

  trackTransaction = (_queryString) => {
    const queryString = nc_trim(_queryString);
    console.log("Received Query String: ", queryString);

    const nonce = this.props.track.queryNonce + 1;

    this.props.dispatch(StoreTrack.Reset({
      queryString: queryString,
      queryNonce: nonce
    }));

    const callback = () => {
      if (this.props.track.momentUpdated == null) {
        this.props.dispatch(StoreTrack.SetFullPageError(new BridgeError(BridgeErrorCode.BRIDGE_API_DOWN)));
      } else {
        NCToaster.show({
          intent: Intent.WARNING,
          timeout: -1,
          message:
            <span>
          {"Detected interruptions in the bridge tracker API service. Please refresh the page and try again in a few moments."}
              <br/>
          <br/>
              {"If you can retrieve a successful bridge transfer transaction receipt through Etherscan (with sufficient finality), "}<b>{"rest assured that your tokens will be bridged."}</b>
        </span>
        });
      }
    };

    network.track(queryString, nonce, callback)
      .catch(e => {
        console.log("network.track() threw exception", e)
      });
  };

  componentDidMount() {
    const ethTxHash = this.props.params.ethTxHash;
    this.trackTransaction(ethTxHash);
  };

  componentDidUpdate(prevProps, prevState, snapshot) {
    const txnHashPrev = prevProps.params.ethTxHash;
    const txnHashNew = this.props.params.ethTxHash;

    if (txnHashPrev != txnHashNew) {
      this.trackTransaction(txnHashNew); // OK, just start tracking this new transaction
    }
  };

  handleVisibilityChange = isVisible => {
    console.log("network.setTrackerPageVisiblity("+isVisible+")");
    network.setTrackerPageVisiblity(isVisible);
  };

  componentWillUnmount() {
    console.log("NCTrack will unmount. Cancelling the polling for Eth Transaction: "+this.props.track.queryString);
    const nonce = this.props.track.queryNonce + 1;
    this.props.dispatch(StoreTrack.Reset({
      queryString: "",
      queryNonce: nonce
    }));
  };

  // ----------------------------------------------------------
  // ----------------------------------------------------------

  // caution: this method mutates the input parameter
  mapTransferToStatemap = (transferStage, statemap) => {
    switch (transferStage) {
      case TransferState.ETH_SUBMITTED: {
        statemap[0].status = StageState.SUCCESS;
        statemap[1].status = StageState.LOADING;
        statemap[2].status = StageState.WAIT;
        statemap[3].status = StageState.WAIT;
        statemap[4].status = StageState.WAIT;
        break;
      }
      case TransferState.BRIDGE_PROCESSING: {
        statemap[0].status = StageState.SUCCESS;
        statemap[1].status = StageState.SUCCESS;
        statemap[2].status = StageState.SUCCESS;
        statemap[3].status = StageState.LOADING;
        statemap[4].status = StageState.WAIT;
        break;
      }
      case TransferState.AION_SUBMITTED: {
        statemap[0].status = StageState.SUCCESS;
        statemap[1].status = StageState.SUCCESS;
        statemap[2].status = StageState.SUCCESS;
        statemap[3].status = StageState.SUCCESS;
        statemap[4].status = StageState.LOADING;
        break;
      }
      case TransferState.FINALIZED: {
        statemap[0].status = StageState.SUCCESS;
        statemap[1].status = StageState.SUCCESS;
        statemap[2].status = StageState.SUCCESS;
        statemap[3].status = StageState.SUCCESS;
        statemap[4].status = StageState.SUCCESS;
        break;
      }
      default: {
        console.log("encountered unknown transfer.state: "+transfer.state);
      }
    }
  };


  // caution: this method mutates the input parameter
  mapFinalityCounterToStatemap = (counter, statemap) => {
    if (counter == null) {
      console.log("mapFinalityCounterToStatemap(): null counter object");
      return;
    }
    if (counter.type == null) {
      console.log("mapFinalityCounterToStatemap(): null counter.type");
      return;
    }
    if (counter.count == null) {
      console.log("mapFinalityCounterToStatemap(): null counter.count");
      return;
    }

    switch (counter.type) {
      case FinalityType.ETH: {
        statemap[1].count = counter.count;
        statemap[4].count = null;
        break;
      }
      case FinalityType.AION: {
        statemap[1].count = null;
        statemap[4].count = counter.count;
        break;
      }
      default: {
        console.log("encountered unknown counter.type: "+counter.type);
      }
    }
  };

  timeRemaining = (transfer) => {
    // We've already done the checks that transfer object is valid. So just use the values.

    let msg = "Transfer to be bridged momentarily.";
    const f = transfer.finality;

    switch (transfer.state) {
      case TransferState.ETH_SUBMITTED: {
        if (f != null && f.type == FinalityType.ETH && f.count != null) {
          const count = new BigNumber(f.count);
          const blocksRemaining = BigNumber(nc.CONFIRMATION_THOLD_ETH).minus(count).plus(12);

          if (blocksRemaining.lt(1)) {
            msg = "Picked up on Ethereum. Awaiting adequate finality.";
          } else {
            const secondsEthRemaining = blocksRemaining.multipliedBy(new BigNumber(nc.BLOCK_TIME_SECONDS_ETH));
            const durationEthRemaining = moment.duration(secondsEthRemaining.toNumber(), "seconds");
            msg = "~ "+durationEthRemaining.humanize()+" remaining before Ethereum finality and Bridging action."
          }
        } else {
          msg = "Picked up on Ethereum. Awaiting adequate finality.";
        }
        break;
      }
      case TransferState.BRIDGE_PROCESSING: {
        msg = "Transaction picked up by the Bridge network. Available on Aion within the next ~5 minutes.";
        break;
      }
      case TransferState.AION_SUBMITTED: {
        if (f != null && f.type == FinalityType.AION && f.count != null) {
          const count = new BigNumber(f.count);
          const blocksRemaining = BigNumber(nc.CONFIRMATION_THOLD_AION).minus(count);
          if (blocksRemaining.lt(1)) {
            msg = "Picked up on Aion. Awaiting adequate finality.";
          } else {
            const secondsRemaining = blocksRemaining.multipliedBy(new BigNumber(nc.BLOCK_TIME_SECONDS_AION));
            const durationRemaining = moment.duration(secondsRemaining.toNumber(), "seconds");
            msg = "~ "+durationRemaining.humanize()+" Bridge transfer is considered final on Aion."
          }
        } else {
          msg = "Picked up on Aion. Awaiting adequate finality.";
        }
        break;
      }
      case TransferState.FINALIZED: {
        msg = "Bridge transfer finalized.";
        break;
      }
    }

    return msg;
  };

  render() {
    const store = this.props.track;
    const state = store.transfer.state;
    let isError = store.fullPageError != null && store.queryString != null && store.transfer != null &&
      store.transfer.ethInfo != null && store.transfer.state != null;
    const ethTxHash = store.transfer.ethInfo.ethTxHash != null ? nc_hexPrefix(store.transfer.ethInfo.ethTxHash) : null;
    const value = (store.transfer.ethInfo.aionTransferAmount != null) ? store.transfer.ethInfo.aionTransferAmount : null;
    const ethAddr = (store.transfer.ethInfo.ethAddress != null) ? nc_hexPrefix(store.transfer.ethInfo.ethAddress) : null;
    const aionAddr = (store.transfer.ethInfo.aionAddress != null) ? nc_hexPrefix(store.transfer.ethInfo.aionAddress) : null;

    const statemap = [
      {
        status: StageState.SUCCESS,
        title: ["Ethereum", "Submitted"],
        tooltip: "Bridge transfer picked on Ethereum (transaction sealed into at least one block)"
      },
      {
        status: StageState.SUCCESS,
        title: ["Ethereum", "Awaiting Finality"],
        tooltip: "Awaiting > "+ nc.CONFIRMATION_THOLD_ETH +" block confirmations",
        count: null
      },
      {
        status: StageState.LOADING,
        title: ["Bridge", "Validating"],
        tooltip: "Bridge nodes are validating the confirmed Ethereum transaction"
      },
      {
        status: StageState.WAIT,
        title: ["Aion", "Submitted"],
        tooltip: "Aion balance updated (transaction sealed into at least one block)"
      },
      {
        status: StageState.WAIT,
        title: ["Aion", "Awaiting Finality"],
        tooltip: "Awaiting > "+ nc.CONFIRMATION_THOLD_AION+ " block confirmations",
        count: null,
        isLast: true
      }
    ];

    if (!isError) {
      this.mapTransferToStatemap(store.transfer.state, statemap);
      this.mapFinalityCounterToStatemap(store.transfer.finality, statemap);
    }

    const page =
      <div>
        <NCExplorerHead
          momentUpdated={store.momentUpdated}
          title={"Track Bridge Transfer"}
          subtitle={<NCEntityLabel
          entityType={NCEntity.TXN}
          entityName={ethTxHash}
          link={nc.ETH_ENDPOINTS[nc.ETH_TARGET_NET].EXPLORER_BASE + nc.ETH_EXPLORER_PAGE_TXN + ethTxHash}/>}/>
        <StateMap statemap={statemap}/>
        <hr className="nc-hr"/>
        <div className="page-level-subtitle">
          <span className="title">Status</span>
          <Tag intent={state == TransferState.FINALIZED ? Intent.SUCCESS : Intent.PRIMARY}
               large={true} minimal={true}>
            { this.timeRemaining(store.transfer) }
          </Tag>
        </div>
        <div className="tracker-view">
          <span className="valueBox">
            <Card  interactive={false} elevation={Elevation.TWO}>
              <span className="value">{value != null ? new BigNumber(value).toFormat(null) : "Not Available"}</span>
              <span className="units">AION</span>
            </Card>
            {/*<div className="title">Value Bridged</div>*/}
          </span>
          <div className="vis-container">
            <span className="left">
              <Icon icon="arrow-right" className={"arrow-right"}/>
              <img className="eth-icon" src="/img/eth-icon.png"/>
              <span className="logo-container"/>
              <div className="addr-container">
                <div className="title">Origin Address</div>
                <div className="subtitle">{"Ethereum "+nc.getEthNetworkInfo(nc.ETH_TARGET_NET).name}</div>
                <div className="addr">
                  <Tooltip content="View address on Etherscan" position={Position.BOTTOM}>
                    <NCEntityLabel
                      entityType={NCEntity.USER}
                      entityName={ethAddr}
                      link={nc.ETH_ENDPOINTS[nc.ETH_TARGET_NET].EXPLORER_BASE + nc.ETH_EXPLORER_PAGE_ADDR + ethAddr}/>
                  </Tooltip>
                </div>
              </div>
            </span>
            <span className="right">
              <Icon icon="arrow-right" className={"arrow-right"}/>
              <img className="aion-icon" src="/img/aion-icon.svg"/>
              <span className="logo-container"/>
              <div className="addr-container">
                <div className="title">Destination Address</div>
                <div className="subtitle">{"Aion "+nc.getAionNetworkInfo(nc.AION_TARGET_NET).name}</div>
                <div className="addr">
                  <Tooltip content="View address on Aion Explorer" position={Position.BOTTOM}>
                    <NCEntityLabel
                      entityType={NCEntity.USER}
                      entityName={aionAddr}
                      link={nc.AION_ENDPOINTS[nc.AION_TARGET_NET].EXPLORER_BASE + nc.AION_EXPLORER_PAGE_ADDR + aionAddr}/>
                  </Tooltip>
                </div>
              </div>
            </span>
          </div>
        </div>
      </div>;

    return (
      <PageVisibility onChange={this.handleVisibilityChange}>
        <NCExplorerPage
          page={page}
          isLoading={store.momentUpdated == null}
          loadingTitle="Retrieving Bridge Transfer"

          isError={isError}
          errorNonIdealState={isError ? <ValidationErrorDesc error={store.fullPageError} queryString={store.queryString} /> : null}/>
      </PageVisibility>
    );
  }
}

export default connect((state) => {
  return ({
    track: state.track
  })
})(NCTrack);
















































