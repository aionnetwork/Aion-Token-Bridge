require('dotenv').config()
const axios = require('axios')
const nodemailer = require('nodemailer')
const smtpTransport = require('nodemailer-smtp-transport')
const twilio = require('twilio')
const moment = require('moment')
const config = require('./config')
const util = require('util')

// twilio(SID, AUTH_TOKEN)
const twilioClient = new twilio(process.env.TWILIO_SID, process.env.TWILIO_AUTH_TOKEN)
const smtpClient = nodemailer.createTransport(
  smtpTransport({ 
    service: 'SendGrid',
    auth: {
        user: process.env.SMTP_USER,
        pass: process.env.SMTP_PASS
    }
  }));

let url = config.API_URL;
let phones = process.env.PHONE ? process.env.PHONE.split(",").map(item => item.trim()) : [];
let emails = process.env.EMAIL ? process.env.EMAIL.split(",").map(item => item.trim()) : [];

const log = (msg="") => {
  console.log("[" + moment().format() + "] " + msg);
}

const inspect = (obj) => {
  console.log((util.inspect(obj, {showHidden: false, depth: null})));
}

log('-------------------------------------------------------');
log('Aion Bridge API Watchdog');
log('-------------------------------------------------------');
log('watching url: ' + url)
log();
log('sending emails to: ')
log(emails.length > 0 ? JSON.stringify(emails) : 'No emails provided');
log();
log('sending sms messages to:')
log(phones.length > 0 ? JSON.stringify(phones) : 'No phone numbers provided');

const TAG = config.TAG ? config.TAG : "";

// for each error, we have a { last triggered time / false }
let latch = {  
  error: {
    'STALE_DATA': {
      ts: null,
      errCount: 0,
      maxErr: 2
    },
    'EVENT_LOOP_ERR': {
      ts: null,
      errCount: 0,
      maxErr: 1
    },
    'HTTP_BAD_RESPONSE': {
      ts: null,
      errCount: 0,
      maxErr: 3
    },
  },
  poll: {
    ethFinalized: false,
    aionLatest: false
  }
}

const validateEndpoint = function() { 
  axios.get(url, {timeout: config.HTTP_TIMEOUT_MS})
  .then(response => {
    if (response == null)
      throw new Error("Null Response");

    if (response.status != 200)
      throw new Error("Non-200 Status Code");

    if (response.data == null)
      throw new Error("Null Body");

    log("Response OK.");

    latch.error['HTTP_BAD_RESPONSE'].errCount = 0;
    detectStaleData(response.data, 'STALE_DATA');
  })
  .catch(error => {
      const message = (error != null && error.message != null) ? error.message : "Unknown Error";
      const errStr = 'UNREACHABLE: '+message;
      
      log("Detected HTTP_BAD_RESPONSE");
      log(errStr);

      latch.error['HTTP_BAD_RESPONSE'].errCount++;
      notifyAll('HTTP_BAD_RESPONSE', errStr, '');
  }); 
}

const detectStaleData = function(json, latchKey) {
  let error = 'Bridge Api: Something went wrong with api response poll.';

  try {
    if (json == null || 
        json.eth == null ||
        json.aion == null ||
        json.eth.finalizedBlockNumber == null ||
        json.aion.latestBlockNumber == null) {
      inspect(json);
      throw "Bridge Api: Irregular api response.";
    }

    const ethFinalized = json.eth.finalizedBlockNumber;
    const aionLatest = json.aion.latestBlockNumber
    log("Eth Finalized: ["+ethFinalized+"], Aion Latest: ["+aionLatest+"]");
    
    // Eth Finalized
    // ---------------------------------------
    if (!latch.poll.ethFinalized) {
      latch.poll.ethFinalized = ethFinalized;
      log("Eth Finalized: Latch updating on first poll.");
    } 
    else {
      let blkDelta = ethFinalized - latch.poll.ethFinalized;
      
      // need the blkDelta move up every poll
      if (!blkDelta || blkDelta < 1) {
        throw "Eth Finalized: NOT UPDATING";
      }

      log("Eth Finalized: ("+ethFinalized+"-"+latch.poll.ethFinalized+") = "+blkDelta+". Progression OK.");

      // update the latch: 
      latch.poll.ethFinalized = ethFinalized;
    }

    // Aion Latest
    // ---------------------------------------
    if (!latch.poll.aionLatest) {
      latch.poll.aionLatest = aionLatest;
      log("Aion Latest: Latch updating on first poll.");
    } 
    else {
      let blkDelta = aionLatest - latch.poll.aionLatest;
      
      // need the blkDelta move up every poll
      if (!blkDelta || blkDelta < 1) {
        throw "Aion Latest: NOT UPDATING";
      }

      log("Aion Latest: ("+aionLatest+"-"+latch.poll.aionLatest+") = "+blkDelta+". Progression OK.");

      // update the latch: 
      latch.poll.aionLatest = aionLatest;
    }

    // we got here = success
    latch.error[latchKey].errCount = 0;
    return;
  } 
  catch (e) {
    error = e;
  }

  // we got here = error
  log(error);

  latch.error[latchKey].errCount++;
  notifyAll(latchKey, error, '');
}

// ----------------------------------------------------------------------------------------

const sendSms = function(recipients, subject) {
  for (let key in recipients) {
    twilioClient.messages.create({
      from: process.env.TWILIO_NUMBER,
      to: recipients[key],
      body: subject
    }, (err, res) => 
    {
      if (err) {
        log('Error occurred with SMS client.');
      } 
      else {
        log('SMS sent to recipient: ' + recipients[key])
      } 
    }); 
  }
}

const sendEmails = function(recipients, subject, body) {
  for (let key in recipients) {
    smtpClient.sendMail({
      from: process.env.SMTP_FROM, 
      to: recipients[key], 
      subject: subject,
      text: body
    }, (err, res) => {
      if (err) {
        log(err);
        log('Error occurred with Email client.');
      } 
      else {
        log('Email sent to recipient: ' + recipients[key])
      } 
    });    
  }
}


// this gets called if we get into an error situation
// handles latching login
const notifyAll = function(error_key, subject, body) 
{  
  if (latch.error[error_key].errCount < latch.error[error_key].maxErr) {
    log("["+error_key+"] Err OK. Count ["+latch.error[error_key].errCount+"] < Max ["+latch.error[error_key].maxErr+"]. Retrying ...");
    return; // don't notify 
  }

  log("["+error_key+"] Err Exceeded. Count ["+latch.error[error_key].errCount+"] >= Max ["+latch.error[error_key].maxErr+"]");

  lastNotificationTs = latch.error[error_key].ts;
  // ok now check if we've met the time requirement
  if (lastNotificationTs == null) {
    sendNotification()
  }
  else if (lastNotificationTs instanceof moment) {
    const timeSinceLastNotification = moment.duration(moment().diff(lastNotificationTs));
    if (timeSinceLastNotification.asMilliseconds() > config.LATCH_RESET_INTERVAL.asMilliseconds()) {
      sendNotification()
    } else {
      log("["+error_key+"] Notification not sent due to latch set.");
    }
  }
  else {
    // send notification without latching if somethings wrong with latching mechanism
    log('ERR: Inconsistant latch state');
    sendNotification()
  }

  function sendNotification() {
    // reset the error count and timestamp
    latch.error[error_key].ts = moment();
    latch.error[error_key].errCount = 0;

    log('+++++++++++++++++++++++++++++++++++++++++++++++++++++++');
    log("Sending Notification");
    log('+++++++++++++++++++++++++++++++++++++++++++++++++++++++');
    
    sendSms(phones, "["+TAG+"] "+subject);
    sendEmails(emails, "["+TAG+"] "+subject, body);
  }
}

const ncEventLoop = function() {
  try {
    validateEndpoint();
    latch.error['EVENT_LOOP_ERR'].errCount = 0;
  }
  catch(err) {
    const str = 'Error with watcher script. Please reboot.';
    log(str);
    log(err);

    latch.error['EVENT_LOOP_ERR'].errCount++;
    notifyAll('EVENT_LOOP_ERR', str, '');
  }
  finally {
    // a visual 'frame' divider
    log('-------------------------------------------------------');
    setTimeout(ncEventLoop, config.POLLING_INTERVAL);
  }
}
ncEventLoop();

