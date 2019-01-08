/*
 * This code is licensed under the MIT License
 *
 * Copyright (c) 2019 Aion Foundation https://aion.network/
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package main

import (
	pb "enclave/proto"
	"enclave/signer"
	"encoding/hex"
	"fmt"
	"log"
	"net"
	"os"
	"syscall"

	"github.com/zondax/ledger-goclient"
	"golang.org/x/crypto/ssh/terminal"
	"golang.org/x/net/context"
	"google.golang.org/grpc"
	"google.golang.org/grpc/reflection"
)

var mode int
var ledger *ledger_goclient.Ledger

//sodium Mode = 0
//ledger Mode = 1

type EnclaveService struct{}

func (s *EnclaveService) Sign(ctx context.Context, in *pb.SignRequest) (*pb.SignedResponse, error) {
	var err error
	var res []byte
	if mode == 0 {
		res, err = (libSodiumSigner).Sign(in.Data)
	} else {
		res, err = (ledgerSigner).Sign(in.Data, ledger)
	}
	if err != nil {
		log.Println("error occurred while signing data: %v\n", err)
		return &pb.SignedResponse{SignedData: []byte{}}, err
	}
	log.Println("Sign success.")
	return &pb.SignedResponse{SignedData: res}, nil
}

var libSodiumSigner = &Signer.LibSodiumSigner{}
var ledgerSigner = &Signer.LedgerSigner{}

func main() {
	args := os.Args[1:]
	lis, err := net.Listen("tcp", ":"+args[0])
	if err != nil {
		log.Fatalf("failed to listen: %v", err)
	}

	switch args[1] {
	case "sodium":
		fmt.Println("Input the private key: ")
		sk, err := terminal.ReadPassword(int(syscall.Stdin))
		if err != nil {
			log.Fatalf("error occurred while reading the key: %v\n", err)
			os.Exit(1)
		}
		log.Println("Private Key ok.")
		if len(sk) >= 2 && string(sk[:2]) == "0x" {
			sk = sk[2:]
		}
		sk_decoded, err := hex.DecodeString(string(sk))
		if err != nil {
			log.Fatalf("error occurred while decoding the key: %v\n", err)
			os.Exit(1)
		}
		mode = 0
		(*libSodiumSigner).Sk = sk_decoded

	case "ledger":
		ledger = (ledgerSigner).Get_Ledger()
		if ledger != nil {
			fmt.Println("connection to ledger ok.")
			mode = 1
		} else {
			log.Fatalf("error occurred while detecting ledger: %v\n", err)
			os.Exit(1)
		}

	default:
		fmt.Println("Selected mode not recognized.")
		fmt.Println("Supported options are '<port> sodium' and '<port> ledger'.")
		os.Exit(1)
	}

	s := grpc.NewServer()
	pb.RegisterEnclaveServiceServer(s, &EnclaveService{})
	reflection.Register(s)
	if err := s.Serve(lis); err != nil {
		log.Fatalf("failed to serve: %v", err)
	}

}
