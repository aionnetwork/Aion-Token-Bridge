const moment = require('moment');
const ms = require('ms');

module.exports.POLLING_INTERVAL = ms('1m');
module.exports.LATCH_RESET_INTERVAL = moment.duration(15, 'minutes');
module.exports.HTTP_TIMEOUT_MS = ms('10s');

module.exports.API_URL = "https://bridge-api.aion.network/status";
module.exports.TAG = "Prod API";