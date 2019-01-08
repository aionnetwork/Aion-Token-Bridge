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

import java.util.List;
import java.util.Optional;


public interface StatelessChainConnectionBase<B extends Block, R extends Receipt<L>, L extends Log, A extends Address> {

    Long getBlockNumber()
            throws IncompleteApiCallException, MalformedApiResponseException, QuorumNotAvailableException, InterruptedException;

    /**
     * returns Optional.empty() if block cannot be found
     */
    Optional<B> getBlock(long blockNumber)
            throws IncompleteApiCallException, MalformedApiResponseException, QuorumNotAvailableException, InterruptedException;

    /**
     * returns Optional.empty() if block cannot be found
     */
    Optional<R> getReceipt(Word32 transactionHash)
            throws IncompleteApiCallException, MalformedApiResponseException, QuorumNotAvailableException, InterruptedException;

    /**
     * returns a closed range: ie. start and end inclusive, ie. result.length = end - start + 1; no nulls
     * throws MalformedApiResponseException if full data requested not found.
     */
    List<B> getBlocksRangeClosed(long start, long end)
            throws IncompleteApiCallException, MalformedApiResponseException, QuorumNotAvailableException, InterruptedException;

    /**
     * throws MalformedApiResponseException if full data requested not found.
     * returns a result sorted in ascending order by block number
     */
    List<BlockWithReceipts<B, R, L>> getReceiptsForBlocks(List<B> blocks)
            throws IncompleteApiCallException, MalformedApiResponseException, QuorumNotAvailableException, InterruptedException;

}
