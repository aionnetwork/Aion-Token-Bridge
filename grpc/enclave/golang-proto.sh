#!/bin/bash

currentver="$(protoc --version)"
requiredver="libprotoc 3.6.0"
 if [ "$currentver" = "$requiredver" ]; then 
        protoc -I proto/ proto/enclave.proto --go_out=plugins=grpc:.
 else
        echo "protoc version 3.6.0 required."
 fi


