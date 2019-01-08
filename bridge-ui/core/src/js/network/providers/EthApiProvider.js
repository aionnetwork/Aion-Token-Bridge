/* eslint-disable */
import RpcProvider from "network/eth/RpcProvider";
import EtherscanProvider from "network/eth/EtherscanProvider";
import * as nc from "lib/NCConstants";

export default class EthApiProvider {
  constructor() {
    const API_BASE = nc.ETH_ENDPOINTS[nc.ETH_TARGET_NET].API_BASE;

    if (API_BASE.ETHERSCAN != null)
      this.etherscan = new EtherscanProvider(API_BASE.ETHERSCAN);

    if (API_BASE.INFURA != null)
      this.infura = new RpcProvider(API_BASE.INFURA);

    if (API_BASE.MY_ETHER != null)
      this.myEther = new RpcProvider(API_BASE.MY_ETHER);

    this.request = this.request.bind(this);
  }

  request = async (failoverList, failoverIndex=0) => {
    console.log("calling api at failoverIndex = "+failoverIndex);

    if (failoverList == null || !Array.isArray(failoverList) || failoverList.length == 0) {
      throw new Error('Invalid failoverList provided');
    }

    if (failoverList[failoverIndex] == null) {
      throw new Error('FailoverList exhausted or invalid invokation with out of bounds index');
    }

    try {
      return await failoverList[failoverIndex]();
    } catch(e) {
      console.log(e);
      console.log('API at index: '+failoverIndex+' failed to respond. Attempting API at index: '+(failoverIndex+1));
      // noinspection JSPotentiallyInvalidUsageOfClassThis
      return await this.request(failoverList, failoverIndex+1)
    }
  };

  getTxReceipt = async (ethTxHash) => {
    const reqs = [];

    if (this.infura)
      reqs.push(() => this.infura.getTxReceipt(ethTxHash));

    if (this.myEther)
      reqs.push(() => this.myEther.getTxReceipt(ethTxHash));

    if (this.etherscan)
      reqs.push(() => this.etherscan.getTxReceipt(ethTxHash));

    return await this.request(reqs);
  };

  getBlockNumber = async () => {
    const reqs = [];

    if (this.infura)
      reqs.push(() => this.infura.getBlockNumber());

    if (this.myEther)
      reqs.push(() => this.myEther.getBlockNumber());

    if (this.etherscan)
      reqs.push(() => this.etherscan.getBlockNumber());

    return await this.request(reqs);
  };

  getTxReceiptAndChainTip = async (ethTxHash) => {
    const reqs = [];

    if (this.infura)
      reqs.push(() => this.infura.getTxReceiptAndChainTipAsRpcBatch(ethTxHash));

    if (this.myEther)
      reqs.push(() => this.myEther.getTxReceiptAndChainTipAsRpcBatch(ethTxHash));

    if (this.etherscan)
      reqs.push(() => this.etherscan.getTxReceiptAndChainTip(ethTxHash));

    return await this.request(reqs);
  };
}
