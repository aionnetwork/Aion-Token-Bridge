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

package org.aion.bridge.chain.bridge;

import org.aion.bridge.chain.aion.types.*;
import org.aion.bridge.chain.base.types.Word16;
import org.aion.bridge.chain.base.types.Word32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AionUnbundlingPolicy {

    private static final Logger LOG = LoggerFactory.getLogger("org.aion.bridge.chain");

    private static final int DISTRIBUTED_TOPICS_COUNT = 4;
    private static final int PROCESSED_TOPICS_COUNT = 3;

    private AionEventFilter processedFilter;
    private AionEventFilter distributedFilter;

    public AionUnbundlingPolicy(String aionContractAddress, String processedEventSig, String distributedEventSig) {
        processedFilter = new AionEventFilter(new AionAddress(aionContractAddress), processedEventSig, new BlakeBloom());
        distributedFilter = new AionEventFilter(new AionAddress(aionContractAddress), distributedEventSig, new BlakeBloom());
    }

    /**
     * @implNote assumption: each receipt should have a distributed event and a processed bundle event.
     * if it doesn't, something went wrong!
     */
    public List<AionBundle> fromFilteredReceipt(List<AionReceipt> filtered) {
        List<AionBundle> finalizedBundles = new ArrayList<>();

        for (AionReceipt receipt : filtered) {
           Word32 ethBlockHash = null;
           Word32 bundleHash = null;
           List<Transfer> transfers = new ArrayList<>();

           for (AionLog l : receipt.getEventLogs()) {

               // Check if have exactly 3 topics

               Word32 eventHash = l.getTopics().get(0);

               if (eventHash.equals(processedFilter.getEventHash())) {
                   // Has exactly 3 topics
                   if(l.getTopics().size() != PROCESSED_TOPICS_COUNT)
                       throw new CriticalBridgeTaskException("Incorrect topic size for PROCESSED_FILTER, found: " + l.getTopics().size() + " Expected: " + PROCESSED_TOPICS_COUNT);

                   ethBlockHash = l.getTopics().get(1);
                   bundleHash = l.getTopics().get(2);
               }
               else if (eventHash.equals(distributedFilter.getEventHash())) {
                   // Has exactly 4 topics
                   if(l.getTopics().size() != DISTRIBUTED_TOPICS_COUNT)
                       throw new CriticalBridgeTaskException("Incorrect topic size for PROCESSED_FILTER, found: " + l.getTopics().size() + " Expected: " + DISTRIBUTED_TOPICS_COUNT);

                   Word32 ethTxHash = l.getTopics().get(1);
                   AionAddress aionAddress = new AionAddress(l.getTopics().get(2));
                   Word16 value = new Word16(l.getTopics().get(3));

                   Transfer t = new Transfer(ethTxHash, aionAddress, value);
                   transfers.add(t);
               }
               else {
                   LOG.error("Found an unknown Event Signature: "+eventHash);
               }
           }

           AionBundle b = new AionBundle(bundleHash, receipt, ethBlockHash, transfers);
           finalizedBundles.add(b);
       }

       return finalizedBundles;
    }
}
