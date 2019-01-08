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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class Model {

    private Logger log = LoggerFactory.getLogger("bridge_api");

    @Autowired
    JdbcTemplate sql;

    @Autowired
    private ObjectMapper jackson;

    @Cacheable(QueryCache.TRANSACTION)
    public Optional<DtoEthTransactionState> getEthTransactionState(String hash) throws JsonProcessingException {
        // make sql query
        long time = System.currentTimeMillis();
        List<DtoEthTransactionState> r = sql.query(DtoEthTransactionState.QUERY, new Object[]{hash},
                new DtoEthTransactionState.JdbcRowMapper());
        time = System.currentTimeMillis() - time;
        log.debug("[SQL] Transaction State [{}]: {} ms ", StringUtils.right(hash, 10), time);

        // produce response
        DtoEthTransactionState response;

        if (r.size() == 0)
            response = DtoEthTransactionState.NOT_FOUND;
        else if (r.size() == 1)
            response = r.get(0);
        else {
            // integrity check
            log.error("Transaction State [{}]: Database integrity error; {}", hash, jackson.writeValueAsString(r));
            return Optional.empty();
        }

        return Optional.of(response);
    }

    @Cacheable(QueryCache.STATUS)
    public Optional<DtoFinalizationStatus> getFinalizationStatus() throws JsonProcessingException {
        long time = System.currentTimeMillis();
        // make sql query
        List<DtoFinalizationStatus> r = sql.query(DtoFinalizationStatus.QUERY,
                new DtoFinalizationStatus.JdbcRowMapper());
        time = System.currentTimeMillis() - time;
        log.debug("[SQL] Finalization Status: {} ms ",  time);

        // produce response
        DtoFinalizationStatus response;

        // no records populated in db
        if (r.size() == 0)
            response = DtoFinalizationStatus.EMPTY;
        else if (r.size() == 1)
            response = r.get(0);
        else {
            // integrity check
            log.error("Finalization Status: Database integrity error; {}", jackson.writeValueAsString(r));
            return Optional.empty();
        }

        return Optional.of(response);
    }

    @Cacheable(QueryCache.AION_LATEST)
    public Optional<DtoAionLatestBlock> getAionLatestBlock() throws JsonProcessingException {
        long time = System.currentTimeMillis();
        // make sql query
        List<DtoAionLatestBlock> r = sql.query(DtoAionLatestBlock.QUERY,
                new DtoAionLatestBlock.JdbcRowMapper());
        time = System.currentTimeMillis() - time;
        log.debug("[SQL] Aion Latest: {} ms ",  time);

        // produce response
        DtoAionLatestBlock response;

        // no records populated in db
        if (r.size() == 0)
            response = DtoAionLatestBlock.EMPTY;
        else if (r.size() == 1)
            response = r.get(0);
        else {
            // integrity check
            log.error("Aion Latest: Database integrity error; {}", jackson.writeValueAsString(r));
            return Optional.empty();
        }

        return Optional.of(response);
    }

    @Cacheable(QueryCache.BALANCE)
    public Optional<DtoBridgeBalanceStatus> getBalance() throws JsonProcessingException {
        long time = System.currentTimeMillis();
        // make sql query
        List<DtoBridgeBalanceStatus> r = sql.query(DtoBridgeBalanceStatus.QUERY,
                new DtoBridgeBalanceStatus.JdbcRowMapper());
        time = System.currentTimeMillis() - time;
        log.debug("[SQL] Balance: {} ms ",  time);

        // produce response
        DtoBridgeBalanceStatus response;

        // no records populated in db
        if (r.size() == 0)
            response = DtoBridgeBalanceStatus.EMPTY;
        else if (r.size() == 1)
            response = r.get(0);
        else {
            // integrity check
            log.error("Balance: Database integrity error; {}", jackson.writeValueAsString(r));
            return Optional.empty();
        }

        return Optional.of(response);
    }
}
