# Aion Explorer Watchdog Script

### What does this script do?

This script polls the `https://bridge-api.aion.network/status` endpoint and validates: 
1. The http request did not time out or returned non-200 response, which means webserver is down 
2. That for every `POLLING_INTERVAL`, the eth finalized and aion latest block numbers reported in the status json increases. If that number does not move up for some reason or moves down, then something went wrong in the service. 

Ideally, something like this should be outsourced to a service like datadog or pingdom, but this works for now.   

### Environment Variables

All sensitive strings like detination phone numbers & emails, twilio and gmail auth credentials can be stored in environment or can be loaded by the nodejs runtime by defining these variables in a .env files in the application root: 

Working .env file is available on LastPass. 

```
SMTP_USER=[]
SMTP_PASS=[]

TWILIO_SID=[]
TWILIO_AUTH_TOKEN=[]
TWILIO_NUMBER=[]

PHONE=19990008888,12220009999
EMAIL=email1@aion.network,email2@aion.network
```

### Twilio and Mailerdaemon Accounts. 

- Mailerdaemon account is used to send out email notifications 
- Twilio account is used to send out sms notifications

### Running Application as Headless Server Application

Run the application using pm2: `http://pm2.keymetrics.io/docs/usage/quick-start/`

Pm2 will reboot the application if it crashes + sends you notifications if the app crashes
> pm2 start app.js

The following keeps the process list intact across server boots
> sudo env PATH=$PATH:/usr/local/bin pm2 startup -u nuco
