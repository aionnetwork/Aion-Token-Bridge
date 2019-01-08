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

import org.aion.bridge.chain.aion.types.AionReceipt;
import org.aion.bridge.chain.base.types.Word32;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;

public class StatefulBundle implements Comparable<StatefulBundle>{

    public enum State {
        BUNDLED, // Transfers extracted from source blocks and split up into bundles
        STORED, // Bundle DB ID assigned and stored in the DB
        SIGNED, // Signed by signatories
        SUBMITTED, // Submitted to destination daemon
        SEALED, // Sealed in a destination block
        FINALIZED // Finalized in the destination chain
    }
    private State state;

    private PersistentBundle bundle;
    private Set<Signature> signatures;
    private AionSubmittedTx submitted;
    private AionReceipt aionTxReceipt;

    public StatefulBundle(PersistentBundle bundle) {
        this.bundle = bundle;
        this.state = State.BUNDLED;
    }

    public void setStored() {
        if(this.state != State.BUNDLED)
            throw new IllegalStateException("bundle state should be BUNDLED");

        this.state = State.STORED;
    }

    public void setSigned(Set<Signature> signatures) {
        if (this.state != State.STORED)
            throw new IllegalStateException("bundle state should be STORED");

        this.signatures = signatures;
        this.state = State.SIGNED;
    }

    public void setSubmitted(AionSubmittedTx submitted) {
        if (this.state != State.SIGNED)
            throw new IllegalStateException("bundle state should be SIGNED");

        this.submitted = submitted;
        this.state = State.SUBMITTED;
    }

    public void setSealed(AionReceipt aionTxReceipt) {
        if(this.state != State.SUBMITTED)
            throw new IllegalStateException("bundle state should be SUBMITTED");

        this.aionTxReceipt = aionTxReceipt;
        this.state = State.SEALED;
    }

    public void setFinalized() {
        if(this.state != State.SEALED)
            throw new IllegalStateException("bundle state should be SEALED");

        this.state = State.FINALIZED;
    }

    // Reset Aion transaction receipt to replace a re-orged transaction
    public void resetSealed(AionReceipt aionTxReceipt) {
        if(this.state != State.SEALED)
            throw new IllegalStateException("bundle state should be SEALED");

        this.aionTxReceipt = aionTxReceipt;
    }

    public State getState() { return state; }
    public PersistentBundle getPersistentBundle() { return bundle; }
    public Bundle getBundle() { return bundle.getBundle(); }
    public long getBundleId() { return bundle.getBundleId(); }
    public Set<Signature> getSignatures() { return signatures; }
    public AionSubmittedTx getTxSubmitted() { return submitted; }
    public AionReceipt getAionReceipt() { return aionTxReceipt; }

    // implement the bundle methods here since a stateful bundle sometimes is used like it extends bundle
    public long getEthBlockNumber() { return bundle.getBundle().getEthBlockNumber(); }
    public Word32 getEthBlockHash() { return bundle.getBundle().getEthBlockHash(); }
    public int getIndexInEthBlock() { return bundle.getBundle().getIndexInEthBlock(); }
    public List<Transfer> getTransfers() { return bundle.getBundle().getTransfers(); }
    public Word32 getBundleHash() { return bundle.getBundle().getBundleHash(); }

    @Override
    public int compareTo(@Nonnull StatefulBundle o) {
        return getBundle().compareTo(o.getBundle());
    }

    public String getErrorString(){
        StringBuilder sb = new StringBuilder();
        sb.append("BundleId: ");
        sb.append(this.getBundleId());
        sb.append("\n");
        sb.append("BundleHash: ");
        sb.append(this.getBundleHash());
        sb.append("\n");
        sb.append("EthBN: ");
        sb.append(this.getEthBlockNumber());
        sb.append("\n");
        sb.append("EthBlockHash: ");
        sb.append(getEthBlockHash());
        sb.append("\n");
        sb.append("Idx: ");
        sb.append(getIndexInEthBlock());
        sb.append("\n");
        sb.append("TransferSize: ");
        sb.append(getTransfers().size());
        sb.append("\n");
        if(getState() != State.SIGNED && getState() != State.STORED) {
            sb.append("AionTxHash: ");
            sb.append(getTxSubmitted().getAionTxHash());
            sb.append("\n");
        }
        return sb.toString();
    }
}
