/* eslint-disable */
import { store } from 'stores/NCReduxStore'

// entity ------------------------------------------------------------

export const NCEntity = 
{
  UNKNOWN: 0,
  BLOCK: 1,
  TXN: 2,
  ACCOUNT: 3,
  USER: 4,
  LINK: 5
};

export let NCEntityInfo = {};

NCEntityInfo[NCEntity.LINK] = {
  icon: "pt-icon-globe",
  name: "Block",
}
NCEntityInfo[NCEntity.BLOCK] = {
  icon: "pt-icon-layers",
  name: "Block",
}
NCEntityInfo[NCEntity.TXN] = {
  icon: "pt-icon-applications",
  name: "Transaction",
}
NCEntityInfo[NCEntity.UNKNOWN] = {
  icon: "pt-icon-help",
  name: "Unknown Entity",
}
NCEntityInfo[NCEntity.ACCOUNT] = {
  icon: "pt-icon-document",
  name: "Account",
}
NCEntityInfo[NCEntity.USER] = {
  icon: "pt-icon-user",
  name: "User",
}

export let NCEntityServerMapping = {};
NCEntityServerMapping['block'] = NCEntity.BLOCK;
NCEntityServerMapping['transaction'] = NCEntity.TXN;
NCEntityServerMapping['account'] = NCEntity.ACCOUNT;

// tables -------------------------------------------------------------

export const NCSortType = {
  ASC: 0,
  DESC: 1,
};

// list types ---------------------------------------------------------

export const blkListType = {
  ALL: 0,
  BY_ACCOUNT: 1
}

export const txnListType = {
  ALL: 0,
  BY_BLOCK: 1,
  BY_ACCOUNT: 2
}

export const accListType = {
  ALL: 0
}

