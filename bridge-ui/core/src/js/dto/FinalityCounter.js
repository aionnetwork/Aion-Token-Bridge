export const FinalityType = {
  UNDEFINED: -1,
  ETH: 0,
  AION: 1
};

export default class FinalityCounter {
  constructor(type, count) {
    this.type = type;
    this.count = count;
  }
}