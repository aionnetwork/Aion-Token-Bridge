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

package org.aion.bridge.chain.base;

import org.aion.bridge.chain.base.types.*;

import java.util.*;

import static java.util.stream.Collectors.toList;

public class BlockProcessor {

    /**
     * Filter at block-level
     */
    public static <B extends Block, F extends EventFilter> boolean filterBlock(B block, F filter) {
        return filter.matches(block.getLogsBloom());
    }

    /**
     * Filter at receipts-level
     *
     * Responsibilities:
     *
     * 1. Throw if receipt not found for transaction hash requested
     * 2. Iterate over all receipts to filter out transactions NOT matching our contract filter
     * 3. Return a list of receipts matching filter
     */
    public static <B extends Block, R extends Receipt<L>, L extends Log, F extends EventFilter> List<R>
            filterReceipts(B block, List<R> receipts, F filter) throws BlockProcessorMissingReceiptsException {

        // filter at receipts-level
        Set<Word32> requestedTxHashes = new HashSet<>(block.getTransactionHashes());

        List<R> filteredReceipts = new ArrayList<>();
        for (R r : receipts) {

            // receipt should have ALL transactions we requested
            if (!requestedTxHashes.remove(r.getTransactionHash()))
                throw new BlockProcessorMissingReceiptsException("Provided receipt that is missing in the transaction hashes reported by the block");

            if (!filter.matches(r.getLogsBloom())) continue;

            List<L> filteredLogs = r.getEventLogs().stream().filter(log -> {
                if (!log.getAddress().equals(filter.getContractAddress()))
                    return false;

                return log.getTopics().get(0).equals(filter.getEventHash());
            }).collect(toList());

            if (!filteredLogs.isEmpty()) {
                filteredReceipts.add(r);
            }
        }

        // receipt should have ALL transactions we requested
        if (!requestedTxHashes.isEmpty())
            throw new BlockProcessorMissingReceiptsException("Provided receipts list missing some transactions, as reported by the block");

        return filteredReceipts;
    }
}
