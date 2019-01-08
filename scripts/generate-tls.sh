#!/usr/bin/env bash

# Useful Notes:
# https://gist.github.com/lloesche/d35b820b99d6da5596525ab3b2168ab9

# Changes these CN's to match your hosts in your environment if needed.
SIGNATORY_IPS="10.0.10.9;10.0.10.10;10.0.10.11;10.0.10.12;10.0.10.13;"; # Signatory
RELAYER_IPS="10.0.10.14;10.0.10.5"; # Relayers

server_ip_list=$(echo $SIGNATORY_IPS | tr ";" "\n");
SERVER_SAN="";

i=0;
for ip in $server_ip_list
do
    SERVER_SAN+="IP.$i = $ip\n";
    i=$((i+1));
done

client_ip_list=$(echo $RELAYER_IPS | tr ";" "\n")
CLIENT_SAN="";

i=0;
for ip in $client_ip_list
do
    CLIENT_SAN+="IP.$i = $ip\n";
    i=$((i+1));
done

echo "Cleaning Out all ca.*, client.*, server.*"
rm ca.* client.* server.*

# --------------------------------------------------------

echo Generate CA Key:
openssl genrsa -passout pass:1111 -des3 -out ca.key 4096

echo Generate CA Certificate:
# Generates ca.crt which is the trustCertCollectionFile
openssl req -passin pass:1111 -new -x509 -days 365 -key ca.key -out ca.crt -subj "/C=CA/ST=ON/O=Aion Foundation"

# --------------------------------------------------------

echo Generate Server Key:
openssl genrsa -passout pass:1111 -des3 -out server.key 4096

echo Generate Server Signing Request:
openssl req -passin pass:1111 -new -key server.key -out server.csr -subj "/C=CA/ST=ON/O=Aion Foundation" 

echo Self-signed Server Certificate:
# Generates server.crt which is the certChainFile for the server
openssl x509 -req -passin pass:1111 -days 365 -in server.csr -CA ca.crt -CAkey ca.key -set_serial 01 -out server.crt -extensions SAN \
-extfile <(cat /etc/ssl/openssl.cnf <(printf "\n[SAN]\nsubjectAltName = @san_names\n[san_names]\n${SERVER_SAN}\n"))

echo Remove Passphrase from Server Key:
openssl rsa -passin pass:1111 -in server.key -out server.key

# --------------------------------------------------------

echo Generate client key
openssl genrsa -passout pass:1111 -des3 -out client.key 4096

echo Generate client signing request:
openssl req -passin pass:1111 -new -key client.key -out client.csr -subj "/C=CA/ST=ON/O=Aion Foundation"

echo Self-signed client certificate:
# Generates client.crt which is the clientCertChainFile for the client (need for mutual TLS only)
openssl x509 -passin pass:1111 -req -days 365 -in client.csr -CA ca.crt -CAkey ca.key -set_serial 02 -out client.crt -extensions SAN \
-extfile <(cat /etc/ssl/openssl.cnf <(printf "\n[SAN]\nsubjectAltName = @san_names\n[san_names]\n${CLIENT_SAN}\n"))

echo Remove passphrase from client key:
openssl rsa -passin pass:1111 -in client.key -out client.key

# --------------------------------------------------------

echo Converting the private keys to X.509:
# Generates client.pem which is the clientPrivateKeyFile for the Client (needed for mutual TLS only)
openssl pkcs8 -topk8 -nocrypt -in client.key -out client.pem
# Generates server.pem which is the privateKeyFile for the Server
openssl pkcs8 -topk8 -nocrypt -in server.key -out server.pem

# --------------------------------------------------------

DEST="./certs_$(date +%Y.%m.%d--%H:%M:%S)";
mkdir $DEST;

mv ca.* $DEST
mv client.* $DEST
mv server.* $DEST