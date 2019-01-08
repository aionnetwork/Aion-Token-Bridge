#!/usr/bin/env bash

APP_DIR="app"
ERR_DIR="404"
MAINT_DIR="app.404_ON"

if [ "${1,,}" = "on" ]; then

  read -p "Turn maintenance ON? Press (Y|y) to proceed:" -n 1 -r
  echo
  if [[ ! $REPLY =~ ^[Yy]$ ]]
  then
      exit 1
  fi

  if [ -d "$APP_DIR" ] && [ -d "$ERR_DIR" ] && [ ! -d "$MAINT_DIR" ]; then
    mv "$APP_DIR" "$MAINT_DIR" && mv "$ERR_DIR" "$APP_DIR"
    echo "Turned ON Maintenance";
    exit 1;
  fi

  echo "Maintenance Already ON!";

elif [ "${1,,}" = "off" ]; then
  read -p "Turn maintenance OFF? Press (Y|y) to proceed:" -n 1 -r
  echo
  if [[ ! $REPLY =~ ^[Yy]$ ]]
  then
      exit 1
  fi

  if [ -d "$APP_DIR" ] && [ ! -d "$ERR_DIR" ] && [ -d "$MAINT_DIR" ]; then
    mv "$APP_DIR" "$ERR_DIR" && mv "$MAINT_DIR" "$APP_DIR"
    echo "Turned OFF Maintenance";
    exit 1;
  fi

  echo "Maintenance already OFF!";
else
  echo "usage"
  echo "-----"
  echo "maintenance.sh [on | off]"
fi


