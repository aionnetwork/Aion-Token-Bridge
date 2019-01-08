/* eslint-disable */
import {nc_trim} from "lib/NCUtility";

export const EthNetwork = {
  UNKNOWN: -1,
  MAINNET: 1,
  ROPSTEN: 3,
  RINKEBY: 4
};

export const AionNetwork = {
  UNKNOWN: -1,
  KILIMANJARO: 1,
  CONQUEST: 2,
  MASTERY: 3
};

export const AppMode = {
  PROD: "PROD",
  STAGING: "STAGING",
  LOCAL: "LOCAL"
};

// VERSION=major.minor.patch
// Update this every time we push an update to PROD
export const VERSION="1.0.5";
export const MODE=AppMode.PROD;

export let API_BRIDGE_BASE;
export let TOKEN_ADDR; // requires 0x at beginning
export let ETH_TARGET_NET;
export let AION_TARGET_NET;

switch (MODE) {
  case AppMode.PROD: {
    API_BRIDGE_BASE="https://bridge-api.aion.network/";
    TOKEN_ADDR="0x4CEdA7906a5Ed2179785Cd3A40A69ee8bc99C466";
    ETH_TARGET_NET=EthNetwork.MAINNET;
    AION_TARGET_NET=AionNetwork.KILIMANJARO;
    break;
  }
  case AppMode.STAGING: {
    API_BRIDGE_BASE="https://bridge-beta-api.aion.network/";
    TOKEN_ADDR="0x6b8b173f044B5F811D111aC4C6D152623f42cA33";
    ETH_TARGET_NET=EthNetwork.ROPSTEN;
    AION_TARGET_NET=AionNetwork.MASTERY;
    break;
  }
  default: { // local
    API_BRIDGE_BASE="http://127.0.0.1:8080/";
    TOKEN_ADDR="0x6b8b173f044B5F811D111aC4C6D152623f42cA33";
    ETH_TARGET_NET=EthNetwork.ROPSTEN;
    AION_TARGET_NET=AionNetwork.MASTERY;
  }
}

// Chain constants
export const BLOCK_TIME_SECONDS_ETH=15;
export const BLOCK_TIME_SECONDS_AION=10;

// Bridge constants
export const CONFIRMATION_THOLD_ETH=64;
export const CONFIRMATION_THOLD_AION=90;

// Polling constants
export const POLL_TRANSFER_TIMEOUT = 30000; // 30s
export const POLL_TRANSFER_MAX_ERROR = 10;

// App pages
export const PATH_TRANSFER = "/transfer";
export const PATH_TRACK = "/track";

// Token constants
export const TOKEN_BURN_FUNCTION_HASH="7a408454"; // NOTE: needs to be lowercase, without hex prefix
export const TOKEN_BURN_EVENT_HASH="0xc3599666213715dfabdf658c56a97b9adfad2cd9689690c70c79b20bc61940c9";

export const ETH_TOKEN_DECIMALS=8;
export const AION_COIN_DECIMALS=18;


export const TOKEN_BURN_ABI_OBJ={
  "constant":false,
  "inputs":[
    {
      "name":"_to",
      "type":"bytes32"
    },
    {
      "name":"_amount",
      "type":"uint256"
    }
  ],
  "name":"burn",
  "outputs":[
    {
      "name":"success",
      "type":"bool"
    }
  ],
  "payable":false,
  "stateMutability":"nonpayable",
  "type":"function"
};

export const TOKEN_BURN_ABI_ARRAY_FORMATTER = (to, amount) => {
  return [to, amount];
};

export const TOKEN_GAS_LIMIT="100000";
export const TOKEN_BURN_TX_MAX_LOGS=10; // maximum number of logs emitted by the burn transaction; to quick filter-out invalid transactions.

// Etherscan URLs
export const ETH_EXPLORER_PAGE_ADDR="address/";
export const ETH_EXPLORER_PAGE_TXN="tx/";
export const ETH_EXPLORER_PAGE_BLK="block/";

// Aion explorer urls
export const AION_EXPLORER_PAGE_ADDR="account/";
export const AION_EXPLORER_PAGE_TXN="transaction/";
export const AION_EXPLORER_PAGE_BLK="block/";

export const ETH_ENDPOINTS = {};

ETH_ENDPOINTS[EthNetwork.MAINNET] = {
  EXPLORER_BASE: "https://etherscan.io/",
  API_BASE : {
    INFURA: "https://mainnet.infura.io/metamask",
    MY_ETHER: "https://api.myetherwallet.com/eth",
    ETHERSCAN: "https://api.etherscan.io/api"
  },
};

ETH_ENDPOINTS[EthNetwork.RINKEBY] = {
  EXPLORER_BASE: "https://rinkeby.etherscan.io/",
  API_BASE : {
    INFURA: "https://rinkeby.infura.io/metamask",
    MY_ETHER: null, // not available
    ETHERSCAN: "https://api-rinkeby.etherscan.io/api"
  },
};

ETH_ENDPOINTS[EthNetwork.ROPSTEN] = {
  EXPLORER_BASE: "https://ropsten.etherscan.io/",
  API_BASE : {
    INFURA: "https://ropsten.infura.io/metamask",
    MY_ETHER: "https://api.myetherwallet.com/rop",
    ETHERSCAN: "https://api-ropsten.etherscan.io/api"
  },
};

export const AION_ENDPOINTS = {};
AION_ENDPOINTS[AionNetwork.KILIMANJARO] = {
  EXPLORER_BASE: "https://mainnet.aion.network/#/"
};
AION_ENDPOINTS[AionNetwork.CONQUEST] = {
  EXPLORER_BASE: "https://conquest.aion.network/#/"
};
AION_ENDPOINTS[AionNetwork.MASTERY] = {
  EXPLORER_BASE: "https://mastery.aion.network/#/"
};

// Wallet url generators
export const myEtherWalletUriGenerator = (data) => {
  return "https://www.myetherwallet.com/?"+
    "to="+TOKEN_ADDR+
    "&value=0"+
    "&gaslimit="+TOKEN_GAS_LIMIT+
    "&data="+encodeURIComponent(nc_trim(data))+
    "#send-transaction"
};

// https://mycrypto.com/account/send/?
export const myCryptoUriGenerator = (data) => {
  return "https://legacy.mycrypto.com/?"+
    "to="+TOKEN_ADDR+
    "&value=0"+
    "&gaslimit="+TOKEN_GAS_LIMIT+
    "&data="+encodeURIComponent(nc_trim(data))+
    "#send-transaction"
};


// Ethereum network details ------------------------------------------------------------

let EthNetworkInfo = {};

EthNetworkInfo[EthNetwork.UNKNOWN] = {
  name: "Unknown"
};
EthNetworkInfo[EthNetwork.MAINNET] = {
  name: "Mainnet"
};
EthNetworkInfo[EthNetwork.ROPSTEN] = {
  name: "Ropsten (Testnet)"
};
EthNetworkInfo[EthNetwork.RINKEBY] = {
  name: "Rinkeby (Testnet)"
};

export const getEthNetworkInfo = (k) => {
  let v = EthNetworkInfo[k];

  if (v == null)
    v = EthNetworkInfo[EthNetwork.UNKNOWN];

  return v;
};

// Aion network details ------------------------------------------------------------

let AionNetworkInfo = {};

AionNetworkInfo[AionNetwork.UNKNOWN] = {
  name: "Unknown"
};
AionNetworkInfo[AionNetwork.KILIMANJARO] = {
  name: "Kilimanjaro (Mainnet)"
};
AionNetworkInfo[AionNetwork.MASTERY] = {
  name: "Mastery (Testnet)"
};
AionNetworkInfo[AionNetwork.CONQUEST] = {
  name: "Conquest (Testnet)"
};

export const getAionNetworkInfo = (k) => {
  let v = AionNetworkInfo[k];

  if (v == null)
    v = AionNetworkInfo[AionNetwork.UNKNOWN];

  return v;
};

// Ethereum network details ------------------------------------------------------------

export const MetamaskStatus = {
  UNKNOWN: -1,
  LOADING: 0,
  CONNECTED: 1,
  NO_CONN: 2,
  NOT_LOGGED_IN: 3,
  INVALID_NETWORK: 4,
};

const MetamaskStatusInfo = {};

MetamaskStatusInfo[MetamaskStatus.UNKNOWN] = {
  title: "Metamask Error",
  msg: "Unknown error. Please refresh and try again.",
};
MetamaskStatusInfo[MetamaskStatus.LOADING] = {
  title: "Loading Metamask",
  msg: "Loading Metamask ...",
};
MetamaskStatusInfo[MetamaskStatus.CONNECTED] = {
  title: "Metamask Connected",
  msg: "Metamask connected to "+EthNetworkInfo[ETH_TARGET_NET].name,
};
MetamaskStatusInfo[MetamaskStatus.NO_CONN] = {
  title: "Metamask Not Detected",
  msg: "Browser extension not detected. Please install and setup Metamask to use dApp.", 
};
MetamaskStatusInfo[MetamaskStatus.NOT_LOGGED_IN] = {
  title: "Metamask Locked",
  msg: "Not logged in. Please unlock Metamask to continue.", 
};
MetamaskStatusInfo[MetamaskStatus.INVALID_NETWORK] = {
  title: "Network Misconfigured",
  msg: "Please switch Metamask network to "+EthNetworkInfo[ETH_TARGET_NET].name+". Application not available on current network.",
};

export const getMetamaskStatusInfo = (k) => {
  let v = MetamaskStatusInfo[k];

  if (v == null)
    v = MetamaskStatusInfo[MetamaskStatus.UNKNOWN];

  return v;
};

// Interaction state  ------------------------------------------------------------

export const InteractionState = {
  ENABLED: 0,
  LOADING: 1,
  DISABLED: 2
};

export const BlockInfoState = {
  STORED: 0, //Block has been stored in the data structure
  PENDING: 1, //Block has been return to the aion kernel (Or any endpoint) but has not been submitted
  SUBMITTED: 2, //Block has been confirmed to have been submitted to Aion
  AWAITING_FINALIZATION: 3, //Transaction has made it into at least 1 block
  FINALIZED: 4, //Block depth is more than Finalization_limit blocks old
  ERROR: 5, //Error occurred processing this block
  STALE: 6, //Sibling blocks not included in the main chain.
};






