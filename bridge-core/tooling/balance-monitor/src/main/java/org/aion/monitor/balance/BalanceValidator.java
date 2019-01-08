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

package org.aion.monitor.balance;

import org.aion.bridge.chain.aion.types.AionAddress;
import org.aion.bridge.chain.log.LoggingSetup;
import org.aion.bridge.datastore.BridgeBalanceState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class BalanceValidator extends Thread {

    private static final Logger log = LoggerFactory.getLogger(BalanceValidator.class);

    private volatile boolean shutdown = false;
    private BridgeBalanceState balanceState;
    private AionAddress accountAddress;
    private Double minimumBalance;
    private final long pollInterval;
    private final TimeUnit pollIntervalTimeUnit;
    private boolean notificationSent = false;

    public BalanceValidator(@Nonnull BridgeBalanceState balanceState,
                            @Nonnull AionAddress accountAddress,
                            @Nonnull Double minimumBalance,
                            @Nonnull long pollInterval,
                            @Nonnull TimeUnit timeUnit) {
        this.balanceState = balanceState;
        this.accountAddress = accountAddress;
        this.minimumBalance = minimumBalance;
        this.pollInterval = pollInterval;
        this.pollIntervalTimeUnit = timeUnit;
    }

    //consecutive errors handled in BridgeBalanceState
    @Override
    public void run() {

        Double balance;
        while (!shutdown) {
            Optional<BigInteger> b = balanceState.getBalance(accountAddress);
            if (b.isPresent()) {
                balance = b.get().doubleValue() / (BigInteger.TEN.pow(18).doubleValue());
                if (balance < minimumBalance) {
                    log.trace("balance low for" , accountAddress.toString());
                    if (!notificationSent) {
                        LoggingSetup.setupLogging();
                        log.error(LoggingSetup.SMTP_MARKER, "Balance low for {}: {} Aion", accountAddress.toStringWithPrefix(), balance);
                        notificationSent = true;
                    }
                } else {
                    notificationSent = false;
                }
            }
            try {
                pollIntervalTimeUnit.sleep(pollInterval);
            } catch (InterruptedException e) {
                log.info("Interrupted via InterruptedException");
                throw new RuntimeException(e.getCause());
            }
        }
    }

    public void shutdown() {
        this.shutdown = true;
        log.debug("Shutdown signal received");
    }
}
