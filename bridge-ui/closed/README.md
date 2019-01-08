Folder structure assumed by `scripts/maintenance.sh`: 
```
404/ app/ maintenance.sh
```

Run the script `./maintenance.sh on`
* This moves 404 => app and app => app.404_ON

Run the script `./maintenance.sh off`
* This moves app => 404 and app.404_ON => app
