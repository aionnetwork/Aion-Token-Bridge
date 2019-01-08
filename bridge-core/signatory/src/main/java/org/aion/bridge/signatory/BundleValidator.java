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

package org.aion.bridge.signatory;

import org.aion.bridge.chain.base.BlockProcessorMissingReceiptsException;
import org.aion.bridge.chain.base.api.IncompleteApiCallException;
import org.aion.bridge.chain.base.api.MalformedApiResponseException;
import org.aion.bridge.chain.base.api.QuorumNotAvailableException;
import org.aion.bridge.chain.base.api.StatelessChainConnection;
import org.aion.bridge.chain.base.types.BlockWithReceipts;
import org.aion.bridge.chain.base.types.Word32;
import org.aion.bridge.chain.bridge.Bundle;
import org.aion.bridge.chain.bridge.EthBundle;
import org.aion.bridge.chain.bridge.EthBundlingPolicy;
import org.aion.bridge.chain.eth.types.EthAddress;
import org.aion.bridge.chain.eth.types.EthBlock;
import org.aion.bridge.chain.eth.types.EthLog;
import org.aion.bridge.chain.eth.types.EthReceipt;
import org.aion.bridge.chain.log.LoggingSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class BundleValidator<T extends StatelessChainConnection<EthBlock, EthReceipt, EthLog, EthAddress>> {
    
    public enum Error {
        INVALID_BLOCK_HEIGHT,
        NON_MAINCHAIN_BLOCK,
        API_ERROR_MISSING_RECEIPTS,
        BUNDLE_INDEX_NOT_EXISTS,
        BUNDLE_HASH_NOT_MATCHES,
        INVALID_JSON_RESPONSE
    }

    private T api;
    private EthBundlingPolicy ethBundlingPolicy;
    private static final Logger log = LoggerFactory.getLogger(BundleValidator.class);

    public BundleValidator(T api, EthBundlingPolicy ethBundlingPolicy) {
        this.api = api;
        this.ethBundlingPolicy = ethBundlingPolicy;
        LoggingSetup.setupLogging();
    }

    /**
     * @implNote We do a query for the block number and then check for blockHash equality since
     * both Ethereum and Aion API's contract says that when I search by blockNumber, the client will return what it
     * believes to be the mainchain block. After retrieving blockByNumber, we check the blockHash to see if
     * this was the block requested by the user.
     *
     */
    public boolean validate(long blockNumber, Word32 blockHash, int bundleIndex, Word32 bundleHash) throws BundleValidatorException {

        if (blockNumber < 0 || bundleIndex < 0 || blockHash == null || bundleHash == null)
            throw new IllegalArgumentException("blockNumber < 0 or indexInSourceChainBlock < 0");

        Optional<EthBlock> block = null;
        try {
            block = api.getBlock(blockNumber);
        } catch (IncompleteApiCallException | MalformedApiResponseException | QuorumNotAvailableException | InterruptedException e) {
            throw new BundleValidatorException(Error.INVALID_JSON_RESPONSE, "getBlock call for block number [" + blockNumber + "] contains an error. " +  e.getCause());
        }
        if (!block.isPresent())
            throw new BundleValidatorException(Error.INVALID_BLOCK_HEIGHT, "Block number [" + blockNumber + "] not found by Eth client.");


        // check this is the block we are looking for
        if (!block.get().getHash().equals(blockHash)) {
            log.debug("Error: {}, Block number {} not found on ethChain for requested hash {}", Error.NON_MAINCHAIN_BLOCK, blockNumber, block.get().getHash().toStringWithPrefix());
            throw new BundleValidatorException(Error.NON_MAINCHAIN_BLOCK, "Block number [" + blockNumber + "] with requested hash not found on Eth mainchain.");
        }

        // get the receipts for the block
        List<BlockWithReceipts<EthBlock, EthReceipt, EthLog>> blockWithReceipts = null;
        try {
            blockWithReceipts = api.getReceiptsForBlocks(List.of(block.get()));
        } catch (IncompleteApiCallException | MalformedApiResponseException | QuorumNotAvailableException | InterruptedException e) {
            throw new BundleValidatorException(Error.INVALID_JSON_RESPONSE, "getReceiptsForBlocks call for block number [" + blockNumber + "] contains an error. " +  e.getCause());
        }

        // we should only get back one block.
        if (blockWithReceipts.size() != 1)
            throw new IllegalStateException();

        // get the Bundles for each block
        List<Bundle> bundles;
        try {
            List<EthBundle> ethBundles = ethBundlingPolicy.fromUnfilteredBlock(block.get(), blockWithReceipts.get(0).getReceipts());
            bundles = ethBundles.stream().map(EthBundle::getBundle).collect(toList());
        } catch (BlockProcessorMissingReceiptsException e) {
            log.debug("Error: {}, Api connection returned bad data: {}", Error.API_ERROR_MISSING_RECEIPTS, e.getMessage());
            throw new BundleValidatorException(Error.API_ERROR_MISSING_RECEIPTS, "Api connection returned bad data for block number [" + blockNumber + "], bundle index [" + bundleIndex + "]" + e.getMessage());
        }

        if (bundleIndex > bundles.size() - 1) {
            log.error(LoggingSetup.SMTP_MARKER, "Error: {}, Bundle index {} does not exist in block {}", Error.BUNDLE_INDEX_NOT_EXISTS, bundleIndex, block.get().getHash().toStringWithPrefix());
            throw new BundleValidatorException(Error.BUNDLE_INDEX_NOT_EXISTS, "Requested indexInSourceChainBlock[" + bundleIndex + "] does not exist in block [" + blockNumber + "]");
        }

        // at this point, we should be able to find the bundleIndex in the bundles array
        // this should not throw an indexOutOfBounds, since we've already made sure index is in range
        Bundle targetBundle = bundles.get(bundleIndex);

        if (!targetBundle.getBundleHash().equals(bundleHash)) {
            log.error(LoggingSetup.SMTP_MARKER, "Error: {}, Target bundle hash {} does not equal calculated bundleHash {}", Error.BUNDLE_HASH_NOT_MATCHES, targetBundle.getBundleHash().toStringWithPrefix(), bundleHash.toStringWithPrefix());
            throw new BundleValidatorException(Error.BUNDLE_HASH_NOT_MATCHES, "Target bundle hash != requested bundle hash. block number [" + blockNumber + "], bundle index [" + bundleIndex + "]");
        }

        return true;
    }
}
