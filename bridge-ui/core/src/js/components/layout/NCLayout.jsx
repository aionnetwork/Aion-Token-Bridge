/* eslint-disable */
import React, {Component} from 'react';
import {hashHistory, Link} from 'react-router'
import {Button, Dialog, Intent, Position, Tooltip} from "@blueprintjs/core";
import * as nc from "lib/NCConstants";

import TopLevelSearch from 'components/widgets/TopLevelSearch';

export default class NCLayout extends Component {

  getDefaultTab = () => {
    return nc.PATH_TRANSFER;
  };

  isPageTransfer = () => {
    const pathname = this.props.location.pathname;

    // noinspection RedundantIfStatementJS
    if (pathname.startsWith(nc.PATH_TRANSFER)) {
      return true;
    }

    return false;
  };

  toggleDisclaimerDialog = () => this.setState(
    (prevState, props) => ({ showDisclaimerDialog: !prevState.showDisclaimerDialog }));

  constructor(props) {
    super(props);

    const isPageTransfer = this.isPageTransfer();
    this.state = {
      showDisclaimerDialog: isPageTransfer
    };

    this.buildHash = null;
    try {
      console.log("Retrieving build hash");
      const file = String(document.currentScript.src);
      const fileName = file.substring(file.lastIndexOf("/") + 1);
      const parts = fileName.split(".");
      console.log(parts);
      if (Array.isArray(parts) && parts[0] != null && parts[1] != null) {
        if (parts[0] == "main") {
          this.buildHash = parts[1];
        }
      }
    } catch (e) {
      console.log("Build hash retrieve failed.");
      console.log(e);
    }
  }

  render() 
  {
    let version = "v"+nc.VERSION;
    if (this.buildHash != null) {
      version += "."+this.buildHash;
    }

    return (
      <div className="NCPage">
        <div className="NCHeader pt-navbar">
          <div className="row">
            <div className="pt-navbar-group navbar-group-left">
              <Link to={this.getDefaultTab()} className="logo">
                <img className="logo-img" src="/img/aion-icon.svg" alt="logo"/>
                <span className="title" style={{
                  width: `118px`
                }}>
                  <span>Token Bridge</span>
                </span>
              </Link>
              <span className="pt-navbar-divider"/>
              <span className="nav-tray">
                <Tooltip content="Transfer Erc-20 Aion Tokens (Ethereum) to Aion Mainchain Coin"
                         position={Position.BOTTOM} intent={Intent.NONE}>
                  <Button
                    className={"navbar-btn "+(this.isPageTransfer() ? "primary" : "default")}
                    minimal={true}
                    icon="plus"
                    onClick={() => { hashHistory.push(nc.PATH_TRANSFER); }}
                    text="Bridge Transfer"/>
                </Tooltip>
              </span>
            </div>
            <div className="pt-navbar-group navbar-group-right">
              <TopLevelSearch/>
              <Tooltip
                content="After sending bridge transfer, you can use this tracker to view the progress of your transfer."
                position={Position.BOTTOM_RIGHT}>
                <Button
                  style={{
                    marginLeft: 5
                  }}
                  className="aion-help-btn"
                  minimal={true}
                  icon={"help"}/>
              </Tooltip>
            </div>
          </div>
        </div>

        <div className="NCPageContent">
          <div className="container">
            { this.props.children }
          </div>
        </div>
        <div className="NCFooter">
          <div className="footer-container">
            <div className="link-container">
              <a className="link" href="https://aion.network" target="_blank">
                <span className="text">Powered By</span>
                <img className="logo" src="/img/aion-icon.svg" alt="logo"/>
              </a>
            </div>
            <div className="version">
              { version }
            </div>
          </div>
        </div>

        <Dialog
          isOpen={this.state.showDisclaimerDialog}
          onClose={this.toggleDisclaimerDialog}
          className="bridge-dialog"
          title="Aion Token Bridge Getting Started">
          <div className="pt-dialog-body dialog-body">
            <div className="dialog-text">
              Welcome to the Aion Token Transfer Bridge for the Token Swap. Using the bridge involves complex
              transactions and is for advanced users. Please begin your swap journey&nbsp;
              <a href="https://docs.aion.network/v1.1/docs/create-and-set-up-wallet" target="_blank">here</a>&nbsp;and follow the instructions carefully.
            </div>
            <div className="dialog-tray">
              <Button
                className="bridge-btn"
                onClick={() => {
                  window.open("https://docs.aion.network/v1.1/docs/swap-overview", "_blank");
                  this.toggleDisclaimerDialog();
                }}
                text="Begin Your Token Swap Journey Here"
                rightIcon="arrow-right"
                intent={Intent.PRIMARY}/>
            </div>
          </div>
        </Dialog>

      </div>
    );
  }
}

































