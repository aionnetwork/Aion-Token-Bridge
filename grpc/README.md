### ProtoC

* Install protoc [v3.6.0](https://github.com/google/protobuf/releases/tag/v3.6.0)

### Golang Probuf Generate

* [Install Go](https://golang.org/doc/install#install)
* Make sure [GOPATH is set correnctly](https://golang.org/doc/code.html#GOPATH)
* Install the protoc Go plugin
   ```
   $ go get -u github.com/golang/protobuf/protoc-gen-go
   ```
* Run the script to generate protobuf files
   ```
   $ cd enclave && ./golang-proto.sh
   ```

### Java Probuf Generate

* Use the gradle wrapper provided to generate grpc-probuf generated source files, wrapped in jars
   ```
   $ ./gradlew -p enclave jar
   $ ./gradlew -p signatory jar
   ```
* The jar files are located in the `/lib` folder
   ```
   /enclave/lib/enclave-grpc.jar
   /signatory/lib/signatory-grpc.jar
   ```
* Copy the jar over to the appropriate project