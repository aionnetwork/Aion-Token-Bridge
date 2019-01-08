#!/bin/bash

#libsodium
#	./configure
#	make && make check
#	sudo make install

export GO_VERSION=1.10
export GO_DOWNLOAD_URL=https://storage.googleapis.com/golang/go$GO_VERSION.linux-amd64.tar.gz

export GOPATH=$HOME/go
export GOROOT=/usr/local/go
export PATH=$PATH:$GOROOT/bin:$GOPATH/bin

sudo mkdir ${GOPATH}
sudo chown ${USER} -R ${GOPATH}

sudo apt update --fix-missing && apt upgrade -y
sudo apt install --no-install-recommends -y gcc

wget "$GO_DOWNLOAD_URL" -O golang.tar.gz
tar -zxvf golang.tar.gz
sudo mv go ${GOROOT}

go version


########################################## protoc
sudo apt install unzip
curl -OL https://github.com/google/protobuf/releases/download/v3.2.0/protoc-3.2.0-linux-x86_64.zip

# Unzip

unzip protoc-3.2.0-linux-x86_64.zip -d protoc3

# Move protoc to /usr/local/bin/

sudo mv protoc3/bin/* /usr/local/bin/

# Move protoc3/include to /usr/local/include/

sudo mv protoc3/include/* /usr/local/include/

protoc --version

######################################### libsodium-dev

sudo ldconfig
sudo apt-get install pkg-config
sudo apt-get install libsodium-dev

######################################### go dependencies

#    go get -d github.com/GoKillers/libsodium-go
#    cd $GOPATH/src/github.com/GoKillers/libsodium-go
#    ./build.sh

#    cd $GOPATH/enclave/main
#    go get ./...
