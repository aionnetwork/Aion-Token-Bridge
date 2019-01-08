// error codes
export const ReceiptErrorCode = {

  // invalid input queryString
  INVALID_ETH_TXHASH: "INVALID_ETH_TXHASH",

  // receipt errors
  RECEIPT_NOT_AVAILABLE: "RECEIPT_NOT_AVAILABLE",
  RECEIPT_WITH_FAILED_STATUS: "RECEIPT_WITH_FAILED_STATUS",
  RECEIPT_INCORRECT_TO_ADDR: "RECEIPT_INCORRECT_TO_ADDR",
  RECEIPT_NO_BURN_LOG: "RECEIPT_NO_BURN_LOG",
  RECEIPT_NON_MAINCHAIN: "RECEIPT_NON_MAINCHAIN",
  RECEIPT_ZERO_VALUE: "RECEIPT_ZERO_VALUE",
  RECEIPT_NOT_BURN_FUNCTION: "RECEIPT_NOT_BURN_FUNCTION",
};

export default class ReceiptError {
  constructor(code) {
    this.code = code;
    this._type = "ReceiptError";
  }
}