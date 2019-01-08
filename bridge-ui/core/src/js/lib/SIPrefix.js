import {BigNumber} from 'bignumber.js';

var PREFIXES = {
  '24': 'Y',
  '21': 'Z',
  '18': 'E',
  '15': 'P',
  '12': 'T',
  '9': 'G',
  '6': 'M',
  '3': 'k',
  '0': '',
  '-3': 'm',
  '-6': 'µ',
  '-9': 'n',
  '-12': 'p',
  '-15': 'f',
  '-18': 'a',
  '-21': 'z',
  '-24': 'y'
};

// expects a bignumber as input
export function formatSI(num) {
  if (num.eq(0)) {
    return {
      sign: '',
      value: BigNumber(0),
      prefix: ''
    };
  }
  var sig = num.abs(); // significand
  var exponent = 0;
  while (sig.gte(1000) && exponent < 24) {
    sig = sig.div(1000);
    exponent += 3;
  }
  while (sig.lt(1) && exponent > -24) {
    sig = sig.times(1000);
    exponent -= 3;
  }

  var signPrefix = num.lt(0) ? '-' : '';
  
  return {
    sign: signPrefix,
    value: sig,
    prefix: PREFIXES[exponent]
  }
}

