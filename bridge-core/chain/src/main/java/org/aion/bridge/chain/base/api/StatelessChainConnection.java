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

package org.aion.bridge.chain.base.api;

import org.aion.bridge.chain.base.types.*;

import java.math.BigInteger;

/**
 * Right now we only use stateless clients (http api's) for connecting to blockchains.
 * <p>
 * In the future, if we add stateful clients, two alternatives exist to change this class:
 * 1. Create a parent class ChainConnection and a sibling interface
 * 2. Change this class to have a reconnect() function.
 * (all dependencies of this class would have to be updated at that point)
 * <p>
 * RATIONALE: Stateful clients vary wildly when it comes to reconnection & disconnection policies, and
 * therefore, no point in trying to anticipate some unknown stateful client's behaviour in our implementation.
 * <p>
 * Explicitly named "Stateless.." because how this api is consumed depends on the statelessness assumption.
 */
public interface StatelessChainConnection<B extends Block, R extends Receipt<L>, L extends Log, A extends Address>
        extends StatelessChainConnectionBase<B, R, L, A> {

    BigInteger getNonce(A address)
            throws IncompleteApiCallException, MalformedApiResponseException, QuorumNotAvailableException, InterruptedException;

    Word32 sendRawTransaction(ImmutableBytes rawTransaction)
            throws IncompleteApiCallException, MalformedApiResponseException, QuorumNotAvailableException, InterruptedException;

    BigInteger getGasPrice()
            throws IncompleteApiCallException, MalformedApiResponseException, QuorumNotAvailableException, InterruptedException;

    String contractCall(A address, ImmutableBytes abi, String blockId)
            throws IncompleteApiCallException, MalformedApiResponseException, QuorumNotAvailableException, InterruptedException;

    String contractCall(A address, ImmutableBytes abi)
            throws IncompleteApiCallException, MalformedApiResponseException, QuorumNotAvailableException, InterruptedException;

    BigInteger getBalance(A address, String blockId)
            throws IncompleteApiCallException, MalformedApiResponseException, QuorumNotAvailableException, InterruptedException;

    Integer peerCount() throws IncompleteApiCallException, MalformedApiResponseException, QuorumNotAvailableException, InterruptedException;

    void evictConnections();
}
