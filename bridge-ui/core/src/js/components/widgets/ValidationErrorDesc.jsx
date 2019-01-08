/* eslint-disable */
import React, {Component} from 'react';
import {ReceiptErrorCode} from "dto/ReceiptError";
import * as nc from "lib/NCConstants";
import NCNonIdealState from "components/common/NCNonIdealState";
import {BridgeErrorCode} from "dto/BridgeError";

export default class ValidationErrorDesc extends Component
{
  render() {

    const error = this.props.error;
    const ethTxHash = this.props.queryString;

    let etherscan_ensure_one_block_confirmation =
      <span>
        Please ensure transaction is included in at-least one block using&#160;
        <a target={"_blank"} href={nc.ETH_ENDPOINTS[nc.ETH_TARGET_NET].EXPLORER_BASE + nc.ETH_EXPLORER_PAGE_TXN + ethTxHash}>Etherscan</a>
        &#160;before retrieving tracking information.
        </span>;

    let etherscan_check_tx_details =
      <span>
        Please review transaction details using&#160;
        <a target={"_blank"} href={nc.ETH_ENDPOINTS[nc.ETH_TARGET_NET].EXPLORER_BASE + nc.ETH_EXPLORER_PAGE_TXN + ethTxHash}>Etherscan</a>.
      </span>;

    let title = "Unable to Retrieve Bridge Transaction";
    let icon = "pt-icon-offline";
    let desc =
      <span>
        {"Unknown error occurred retrieving Ethereum transaction."}
      </span>;

    console.log(error);
    console.log(nc.ETH_TARGET_NET);

    if (error != null && error.code != null) {
      switch (error.code) {
        case BridgeErrorCode.BRIDGE_API_DOWN: {
          title = "Bridge Tracker Service Interruption";
          icon = "pt-icon-offline";
          desc =
            <span>
              {"Could not reach bridge tracker API server."}
              <br/>
              {"(You might not be able to track your bridge transfers momentarily)"}
              <br/>
              <br/>
              {"If you can retrieve a successful bridge transfer transaction receipt through Etherscan"}
              <br/>
              {" (with sufficient finality), "}<b>{"rest assured that your tokens will be bridged!"}</b>
            </span>;
          break;
        }
        case ReceiptErrorCode.INVALID_ETH_TXHASH: {
          title = "Transaction Hash Validation Failure";
          icon = "pt-icon-warning-sign";
          desc =
            <span>
              {"Invalid Ethereum transaction hash: "} <b>{"\""+ethTxHash+"\"."}</b>
              <br/>
              {"Valid Ethereum transaction hash is a 32 byte field (64 character hex string)."}
            </span>;
          break;
        }
        case ReceiptErrorCode.RECEIPT_NOT_AVAILABLE: {
          title = "Transaction Receipt Not Available";
          icon = "pt-icon-offline";
          desc =
            <span>
              {"Transaction does not seem to have made it onto Ethereum "+nc.getEthNetworkInfo(nc.ETH_TARGET_NET).name+"."}
              <br/>
              {etherscan_ensure_one_block_confirmation}
            </span>;
          break;
        }
        case ReceiptErrorCode.RECEIPT_WITH_FAILED_STATUS: {
          title = "Non-Success Status Transaction";
          icon = "pt-icon-warning-sign";
          desc =
            <span>
              {"Transaction receipt found to have a non-success status code."}
              <br/>
              {etherscan_check_tx_details}
            </span>;
          break;
        }
        case ReceiptErrorCode.RECEIPT_INCORRECT_TO_ADDR: {
          title = "Invalid Transaction Recipient";
          icon = "pt-icon-warning-sign";
          desc =
            <span>
              {"Transaction's recipient is not the Aion ERC20 Token address."}
              <br/>
              {etherscan_check_tx_details}
            </span>;
          break;
        }
        case ReceiptErrorCode.RECEIPT_NO_BURN_LOG: {
          title = "Insufficient Aion Token Balance";
          icon = "pt-icon-warning-sign";
          desc =
            <span>
              {"Requested transfer amount exceeds available AION token balance for the Ethereum address from which bridge transaction originated."}
              <br/>
              {etherscan_check_tx_details}
            </span>;
          break;
        }
        case ReceiptErrorCode.RECEIPT_NON_MAINCHAIN: {
          title = "Sidechain Transaction";
          icon = "pt-icon-warning-sign";
          desc =
            <span>
              {"Transaction ended up on a sidechain. Please try sending the transaction again."}
              <br/>
              {etherscan_check_tx_details}
            </span>;
          break;
        }
        case ReceiptErrorCode.RECEIPT_ZERO_VALUE: {
          title = "Zero Value Bridge Transfer";
          icon = "pt-icon-warning-sign";
          desc =
            <span>
              {"Zero value Bridge Transfer transactions are not tracked or processed by the bridge."}
              <br/>
              {etherscan_check_tx_details}
            </span>;
          break;
        }
        case ReceiptErrorCode.RECEIPT_NOT_BURN_FUNCTION: {
          title = "Wrong Contract Function Called";
          icon = "pt-icon-warning-sign";
          desc =
            <span>
              {"Attempting to retrieve non-bridge transaction call. Function signature does not match "}
              <code>burn(bytes32 _to, uint256 _amount)</code>{" ."}
              <br/>
              {etherscan_check_tx_details}
            </span>;
          break;
        }

      }
    }
    
    return (
      <NCNonIdealState
        paddingBottom={40}
        icon={icon}
        title={title}
        description={desc}
        showHomeLink={true}/>);
  };
}
















































