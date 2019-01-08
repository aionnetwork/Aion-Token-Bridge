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

package org.aion.bridge.chain.base.oracle;

import org.aion.bridge.chain.base.types.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultset returned by the ChainOracle
 *
 * Nomenclature:
 *
 * EmptyBlocks = blocks without any transactions-of-interest
 * BridgeBlocks = set of block + receipts for only transactions-of-interest
 *
 * all blocks
 */
public class ChainOracleResultset<B extends Block, R extends Receipt<L>, L extends Log> {
    private enum STATE {
        OPEN,
        FINALIZED
    }

    private STATE state;
    private List<BlockWithReceipts<B, R, L>> allBlocks;

    public ChainOracleResultset() {
        allBlocks = new ArrayList<>();
        state = STATE.OPEN;
    }

    public boolean isOpen() {
        return state == STATE.OPEN;
    }

    public void appendEmptyBlock(B block) {
        if (state != STATE.OPEN) throw new IllegalStateException();
        allBlocks.add(new BlockWithReceipts<>(block));
    }

    public void appendFilledBlock(BlockWithReceipts<B, R, L> filledBlock) {
        if (state != STATE.OPEN) throw new IllegalStateException();
        allBlocks.add(filledBlock);
    }

    public void finalizeResultset() {
        // sort has n*lg(n) performance
        this.allBlocks.sort(BlockWithReceipts::compareByBlockNumber);
        this.state = STATE.FINALIZED;
    }

    public List<BlockWithReceipts<B, R, L>> getAllBlocks() {
        return allBlocks;
    }
}
