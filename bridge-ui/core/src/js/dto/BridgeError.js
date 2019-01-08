// error codes
export const BridgeErrorCode = {
  UNDEFINED: "BRIDGE_ERROR_UNDEFINED",

  // generic api-call failure cases
  ETH_API_CALL_FAILURE: "ETH_API_CALL_FAILURE",
  ETH_API_INCONSISTENT_DATA: "ETH_API_INCONSISTENT_DATA",

  BRIDGE_API_CALL_FAILURE: "BRIDGE_API_CALL_FAILURE",
  BRIDGE_API_INCONSISTENT_DATA: "BRIDGE_API_INCONSISTENT_DATA",

  BRIDGE_API_DOWN: "BRIDGE_API_DOWN",
};

export default class BridgeError {
  constructor(code) {
    this.code = code;
  }
}