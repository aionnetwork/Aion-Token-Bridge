# Aion Token Bridge

# Overview

The Aion Token Bridge is a purpose built blockchain interoperability application designed to transfer AION ERC-20 tokens from the Ethereum blockchain to native AION coins on the Aion blockchain. This repository contains implementations for:
* Relay: a multi-chain monitoring application which tracks blocks and transactions on the Aion and Ethereum blockchains.
* Signatories: Blockchain transaction verification and transaction signing
* Enclave: A standalone process managing private keys and signature cryptography

### System Requirements

* Ubuntu 18.04.1 LTS
* OpenJDK 11 (Relay + Signatories)
* Libsodium 
* Golang 1.11 (Enclave)
