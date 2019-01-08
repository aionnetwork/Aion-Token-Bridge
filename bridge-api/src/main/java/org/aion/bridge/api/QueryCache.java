/*
 *
 *   This code is licensed under the MIT License
 *
 *   Copyright (c) 2019 Aion Foundation https://aion.network/
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in all
 *   copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *   SOFTWARE.
 *
 */

package org.aion.bridge.api;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class QueryCache {

    public final static String STATUS = "status";
    public final static long STATUS_CACHE_TIMEOUT = 5;

    public final static String AION_LATEST = "aion_latest";
    public final static long AION_LATEST_CACHE_TIMEOUT = 5;

    public final static String TRANSACTION = "transaction";
    public final static long TRANSACTION_CACHE_TIMEOUT = 5;

    public final static String BALANCE = "balance";
    public final static long BALANCE_CACHE_TIMEOUT = 5;

    public final static String BATCH = "batch";
    public final static long BATCH_CACHE_TIMEOUT = 5;

    public final static TimeUnit TIMEUNIT = TimeUnit.SECONDS;

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cm = new SimpleCacheManager();

        // TODO: hook up stats recording to configuration file
        boolean statsEnabled = false;

        //noinspection ConstantConditions
        cm.setCaches(Arrays.asList(
                buildExpireAfterWriteCache(STATUS, STATUS_CACHE_TIMEOUT, TIMEUNIT, 10, statsEnabled),
                buildExpireAfterWriteCache(TRANSACTION, TRANSACTION_CACHE_TIMEOUT, TIMEUNIT, 5000, statsEnabled),
                buildExpireAfterWriteCache(BALANCE, BALANCE_CACHE_TIMEOUT, TIMEUNIT, 5000, statsEnabled),
                buildExpireAfterWriteCache(BATCH, BATCH_CACHE_TIMEOUT, TIMEUNIT, 5000, statsEnabled),
                buildExpireAfterWriteCache(AION_LATEST, AION_LATEST_CACHE_TIMEOUT, TIMEUNIT, 5000, statsEnabled)
        ));
        return cm;
    }

    @SuppressWarnings("SameParameterValue")
    private Cache buildExpireAfterWriteCache(String name, long duration, TimeUnit timeUnit, long maxSize, boolean statsEnabled) {
        Caffeine<Object, Object> cc = Caffeine.newBuilder()
                .expireAfterWrite(duration, timeUnit)
                .maximumSize(maxSize);

        if(statsEnabled) cc.recordStats();

        return new CaffeineCache(name, cc.build());
    }
}
