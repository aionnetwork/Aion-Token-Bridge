/* eslint-disable */
import React, {Component} from 'react';
import NCExplorerPage from 'components/common/NCExplorerPage';
import NCExplorerHead from "components/common/NCExplorerHead";
import {StageState} from "components/common/Types";
import * as nc from "lib/NCConstants";
import StateMap from "components/widgets/StateMap";
import {Button, Checkbox, Dialog, FormGroup, Icon, Intent, Position, Tooltip} from "@blueprintjs/core";
import NCEntityLabel from "components/common/NCEntityLabel";
import {
  nc_convertBignumberToAionTokenBaseUnits,
  nc_hexPrefix,
  nc_isValidAionAddress,
  nc_isValidAionTokenAmount,
  nc_trim
} from "lib/NCUtility";
import {NCEntity} from "lib/NCEnums";
import EthAbi from "web3-eth-abi";
import {bridgeApi} from 'network/NCNetwork';

const NCTransferStage = {
  GENERATE: "GENERATE",
  SEND: "SEND"
};
const ERR_REASON_AMT = <span>Positive Number, Max 8 Decimals</span>;
const ERR_REASON_ADDR = "A valid Aion address is 32 bytes (64 hex characters), first byte = 0xa0";
const ERR_REASON_CHECKBOX1 = "You must acknowledge that you've double-checked your Aion address before proceeding";

export default class NCTransfer extends Component
{
  constructor(props) {
    super(props);

    this.state = {
      showInstructionsDialog: false,
      stage: NCTransferStage.GENERATE,
      amount: "",
      amount_err: false,

      isGenerating: false,
      showInsufficientBalanceDialog: false,

      aionAddr: "",
      aionAddrShowLink: false,
      aionAddr_err: false,

      checkbox1: false,
      checkbox1_err: false,

      abi: null
    };
  }

  handleAmountInput = (event) => {
    this.setState({
      amount: event.target.value,
      amount_err: false
    });
  };

  handleAionAddrInput = (event) => {
    if (nc_isValidAionAddress(event.target.value)) {
      // ok to show the link to aion block explorer
      this.setState({
        aionAddr: event.target.value,
        aionAddrShowLink: true,
        aionAddr_err: false,
      });
    } else {
      this.setState({
        aionAddr: event.target.value,
        aionAddrShowLink: false,
        aionAddr_err: false
      });
    }
  };

  reset = () => {
    this.setState({
      showInstructionsDialog: false,
      stage: NCTransferStage.GENERATE,
      amount: "",
      aionAddr: "",
      aionAddrShowLink: false,
      checkbox1: false,

      isGenerating: false,
      showInsufficientBalanceDialog: false,

      amount_err: false,
      aionAddr_err: false,
      checkbox1_err: false
    });
  };

  generate = () => {
    const aionAddr = nc_hexPrefix(this.state.aionAddr);
    const amount = nc_trim(this.state.amount);

    const is_aion_addr_err = !nc_isValidAionAddress(aionAddr);
    let is_amount_err = !nc_isValidAionTokenAmount(amount);
    if (is_amount_err === true || is_aion_addr_err === true || this.state.checkbox1 === false) {
      this.setState({
        amount_err: is_amount_err,
        aionAddr_err: is_aion_addr_err,
        checkbox1_err: !this.state.checkbox1
      });
      return;
    }

    let hexAionBalance = "0x"+nc_convertBignumberToAionTokenBaseUnits(amount).toString(16);

    console.log("Request for ABI generation with balance [String: "+amount+"] -> [Hex: "+hexAionBalance+"]");

    // isGenerating = true
    // assume enough balance
    let isEnoughBalance = true;
    this.setState({
      isGenerating: true
    }, async () => {
      try {
        isEnoughBalance = await bridgeApi.isEnoughBridgeBalance(hexAionBalance);
      } catch (e) {
        console.log("bridgeApi.isEnoughBridgeBalance failed with error", e);
      } finally {
        if (isEnoughBalance === false) {
          this.setState({
            isGenerating: false,
            showInsufficientBalanceDialog: true
          });
        } else {
          // ok to generate the encoded abi (data payload)
          const abi = nc_hexPrefix(EthAbi.encodeFunctionCall(nc.TOKEN_BURN_ABI_OBJ,
            nc.TOKEN_BURN_ABI_ARRAY_FORMATTER(aionAddr, hexAionBalance)));

          this.setState({
            isGenerating: false,
            aionAddrShowLink: true,
            stage: NCTransferStage.SEND,
            amount_err: false,
            aionAddr_err: false,
            checkbox1_err: false,
            abi: abi
          });
        }
      }
    });
  };

  linkInstructions = () => {
    window.open("https://docs.aion.network/v1.1/docs/swap-overview", "_blank");
  };

  toggleInsufficientBalanceDialog = () => this.setState(
    (prevState, props) => ({ showInsufficientBalanceDialog: !prevState.showInsufficientBalanceDialog }));

  toggleCheckbox1 = () => this.setState((prevState, props) => ({
    checkbox1: !prevState.checkbox1,
    checkbox1_err: false
  }));

  render() {

    // case NCTransferStage.GENERATE
    const statemap = [
      {
        status: StageState.COUNT,
        title: ["Input Transfer", "Details"],
        tooltip: "Input destination Aion address and token transfer value"
      },
      {
        status: StageState.WAIT,
        title: ["Generate", "Transaction"],
        tooltip: "Generate ethereum transaction payload and details"
      },
      {
        status: StageState.WAIT,
        title: ["Send", "Transaction"],
        tooltip: "Send transaction with generated payload to Ethereum network",
        isLast: true
      }
    ];

    if (this.state.stage == NCTransferStage.SEND) {
      statemap[0].status = StageState.SUCCESS;
      statemap[1].status = StageState.SUCCESS;
      statemap[2].status = StageState.COUNT;
    }

    const isInputDisabled = this.state.stage != NCTransferStage.GENERATE || this.state.isGenerating === true;

    const page =
      <div className="NCTransfer">
        <NCExplorerHead
          title={"Generate Bridge Transfer"}
          subtitle={"Convert Aion ERC20 to Aion Coin"}
          rightElement={
            <div className="head-btn-group">
              <Tooltip content="Step by step instructions on how to Bridge your Aion ERC20 token(s) to Aion native coin(s)" position={Position.BOTTOM_RIGHT}>
                <Button
                  className="bridge-btn"
                  onClick={this.linkInstructions}
                  text="How Do I Use This?"
                  icon="info-sign"
                  intent={Intent.PRIMARY}/>
              </Tooltip>
            </div>
          }/>
        <StateMap statemap={statemap}/>
        <hr className="nc-hr"/>
        <div className="stage-generate">
          <div className="page-level-subtitle">
            <span className="title">Input Transaction Details</span>
            <Tooltip
              content="Input your Aion address and token amount to start the token swap process"
              position={Position.RIGHT}>
              <Button
                style={{
                  marginLeft: 0,
                  marginTop: -5
                }}
                className="aion-help-btn"
                minimal={true}
                onClick={() => {console.log("link me to instructions.")}}
                icon={"help"}/>
            </Tooltip>
          </div>
          <div className="interaction-statement">
            <span className={"desc "+
              (this.state.stage != NCTransferStage.GENERATE ? "pt-disabled" : "")}>Send ERC20</span>
            <FormGroup
              className="form-group form-group-amt"
              disabled={isInputDisabled}
              helperText={this.state.amount_err ? ERR_REASON_AMT : undefined}
              label="AION Amount"
              intent={this.state.amount_err ? Intent.DANGER : Intent.NONE}
              labelFor="bridge-amt">
              <input
                id="bridge-amt"
                className={
                  "pt-input "+
                  (this.state.amount_err ? "pt-intent-danger " : "")}
                disabled={isInputDisabled}
                placeholder="0.01"
                value={this.state.amount} onChange={this.handleAmountInput}/>
            </FormGroup>
            <span className={"desc "+
            (this.state.stage != NCTransferStage.GENERATE ? "pt-disabled" : "")}>{"to "+nc.getAionNetworkInfo(nc.AION_TARGET_NET).name}</span>
            <span className="aion-addr-group">
              <FormGroup
                className="form-group form-group-addr"
                disabled={isInputDisabled}
                helperText={this.state.aionAddr_err ? ERR_REASON_ADDR : undefined}
                label="Aion Address"
                intent={this.state.aionAddr_err ? Intent.DANGER : Intent.NONE}
                labelFor="bridge-addr">
                <input
                  id="bridge-addr"
                  className={
                    "pt-input "+
                    (this.state.aionAddr_err ? "pt-intent-danger " : "")}
                  disabled={isInputDisabled}
                  placeholder="0xa043552ee4504195134e239a71cb30f520a31403fb5aeb5305e72bdcb20e4533"
                  value={this.state.aionAddr} onChange={this.handleAionAddrInput}/>
              </FormGroup>
              {
                (this.state.aionAddrShowLink) &&
                <div className="addr-group-link">
                  <Icon icon="arrow-right" className="addr-group-link-icon"/>
                  <NCEntityLabel
                    entityType={NCEntity.USER}
                    entityName={"View on Aion Explorer"}
                    link={nc.AION_ENDPOINTS[nc.AION_TARGET_NET].EXPLORER_BASE + nc.AION_EXPLORER_PAGE_ADDR + this.state.aionAddr}/>
                </div>
              }
            </span>
            {
              (this.state.stage == NCTransferStage.GENERATE) &&
              <Button
                className="smt-btn"
                intent={Intent.PRIMARY}
                rightIcon="arrow-right"
                minimal={false}
                onClick={() => this.generate()}
                loading={this.state.isGenerating}
                text="Generate Transfer"/>
            }
            {
              (this.state.stage != NCTransferStage.GENERATE) &&
              <Button
                className="resmt-btn"
                intent={Intent.PRIMARY}
                icon="refresh"
                minimal={false}
                onClick={() => this.reset()}
                text="New Transfer"/>
            }
          </div>
          <div className="interaction-checkboxes">
            <FormGroup
              className="form-group checkbox-row"
              disabled={isInputDisabled}
              helperText={this.state.checkbox1_err ? ERR_REASON_CHECKBOX1 : undefined}
              label={null}
              intent={this.state.checkbox1_err ? Intent.DANGER : Intent.NONE}
              labelFor="bridge-checkbox-1">
              <Checkbox
                id="bridge-checkbox-1"
                disabled={isInputDisabled}
                checked={this.state.checkbox1}
                label={
                  <div className="bridge-tos">
                    <div className="line-1">I've verified that I have full access to the Aion wallet on the receiving
                      end of this transaction (using either desktop wallet, Coinomi or Kernel command line).</div>
                    <div className="line-2">By clicking transfer, I accept the&nbsp;
                      <a target="_blank" href="https://aion.network/token-transfer-bridge-terms-of-service/">
                        Aion Token Bridge Terms of Service</a>.</div>
                  </div>
                }
                onChange={this.toggleCheckbox1} />
            </FormGroup>
          </div>
        </div>
        {
          this.state.stage == NCTransferStage.SEND &&
          <div className="stage-send">
            <hr className="nc-hr"/>
            <div className="page-level-subtitle">
              <span className="title">Send Ethereum Transaction</span>
              <Tooltip
                content="Use hotlinks or transaction data payload to send bridge transfer"
                position={Position.RIGHT}>
                <Button
                  style={{
                    marginLeft: 0,
                    marginTop: -5
                  }}
                  className="aion-help-btn"
                  minimal={true}
                  onClick={() => {console.log("link me to instructions.")}}
                  icon={"help"}/>
              </Tooltip>
            </div>
            <div className="subsection-title row1">
              <span className="title">Using Pre-populated Wallet Hotlink</span>
              <div className="desc">
                <div>For convenience of web wallet users, we have generated hotlinks that open the wallet with appropriate transaction fields pre-populated.</div>
                <div style={{color: "#c23030", paddingTop: 5, fontWeight: 500}}>Please do not change the pre-populated fields, which should match the "Generated Transaction Fields" defined below (e.g. value field must be 0)</div>
              </div>
            </div>
            <div className="wallet-desc hotlink-row1">
              <span className="hotlink-text">
                Web-wallet hotlinks for
              </span>
              <Button
                text={
                  <span className="btn-inner">
                    <img src="/img/mew-icon.svg" className="btn-img"/>
                    <span className="btn-text">MyEtherWallet</span>
                  </span>
                }
                className={"hotlink-btn"}
                rightIcon="arrow-right"
                onClick={() => {
                  if (this.state.abi != null && this.state.abi.startsWith("0x")) {
                    window.open(nc.myEtherWalletUriGenerator(this.state.abi));
                  } else {
                    alert("Error occurred while encoding Ethereum function ABI. Please contact support@aion.network to report the issue.");
                  }
                }}
              />
              <span className="hotlink-text"> & </span>
              <Button
                text={
                  <span className="btn-inner">
                    <img src="/img/mycrypto-icon.png" className="btn-img"/>
                    <span className="btn-text">MyCrypto</span>
                  </span>
                }
                className={"hotlink-btn"}
                rightIcon="arrow-right"
                onClick={() => {
                  if (this.state.abi != null && this.state.abi.startsWith("0x")) {
                    window.open(nc.myCryptoUriGenerator(this.state.abi));
                  } else {
                    alert("Error occurred while encoding Ethereum function ABI. Please contact support@aion.network to report the issue.");
                  }
                }}
              />
            </div>
            <div className="or-separator">
              <span className="line"/>
              <span className="or">OR</span>
            </div>
            <div className="subsection-title row2">
              <span className="title">Manually Enter Transaction Details</span>
              <div className="desc">
                <span>For users of any other wallet software that supports transactions with data fields,
                  based on the information entered above, we have generated all the necessary transaction fields:</span>
              </div>
            </div>
            {/*
            <div className="wallet-desc hotlink-row2">
              <span className="hotlink-text">
                Examples on how to populated transaction fields using popular wallet software
              </span>
              <Button
                text={
                  <span className="btn-inner">
                    <img src="/img/jaxx-icon.png" className="btn-img jaxx"/>
                  </span>
                }
                className={"hotlink-btn"}
                rightIcon="arrow-right"
                onClick={() => { alert("Tutorial coming soon!"); }}
              />
              <span className="hotlink-text"> & </span>
              <Button
                text={
                  <span className="btn-inner">
                    <img src="/img/coinomi-icon.svg" className="btn-img coinomi"/>
                  </span>
                }
                className={"hotlink-btn"}
                rightIcon="arrow-right"
                onClick={() => { alert("Tutorial coming soon!"); }}
              />
            </div>
            */}
            <div className="manual-copy">
              <div className="top-desc">
                Generated Transaction Fields:
              </div>
              <div className="pt-card .pt-elevation-1 fields">
                <div className="copy-row">
                  <span className="head">To Address</span>
                  <pre className="value">{nc_hexPrefix(nc.TOKEN_ADDR)}</pre>
                  <Tooltip content="This is the Aion ERC20 Token address on Ethereum">
                    <Button
                      className="aion-help-btn"
                      minimal={true}
                      icon={"help"}/>
                  </Tooltip>
                </div>
                <div className="copy-row">
                  <span className="head">Value</span>
                  <pre className="value">{0}</pre>
                  <Tooltip content="This is a zero value transaction; contract call is non-payable.">
                    <Button
                      className="aion-help-btn"
                      minimal={true}
                      icon={"help"}/>
                  </Tooltip>
                </div>
                <div className="copy-row">
                  <span className="head">Gas Limit</span>
                  <pre className="value">{nc.TOKEN_GAS_LIMIT}</pre>
                  <Tooltip content="Need to allocate enough gas for successful function call.">
                    <Button
                      className="aion-help-btn"
                      minimal={true}
                      icon={"help"}/>
                  </Tooltip>
                </div>
                <div className="copy-row">
                  <span className="head">Data</span>
                  <pre className="value">{this.state.abi != null && this.state.abi.startsWith("0x") ? this.state.abi : "Not Available"}</pre>
                  <Tooltip content="Data field has the Ethereum function call encoded according to the Ethereum ABI specification.">
                    <Button
                      className="aion-help-btn"
                      minimal={true}
                      icon={"help"}/>
                  </Tooltip>
                </div>
              </div>
            </div>
          </div>
        }

        <Dialog
          isOpen={this.state.showInsufficientBalanceDialog}
          onClose={this.toggleInsufficientBalanceDialog}
          className="bridge-dialog"
          title="Bridge Transfer Disabled">
          <div className="pt-dialog-body dialog-body">
            <div className="">
              There is more traffic than expected on the token swap bridge right now. Please try again in 15 minutes.
              If the issue persists, please email support@aion.network.
            </div>
          </div>
        </Dialog>
      </div>;

    return (<NCExplorerPage page={page}/>);
  }
}

















































