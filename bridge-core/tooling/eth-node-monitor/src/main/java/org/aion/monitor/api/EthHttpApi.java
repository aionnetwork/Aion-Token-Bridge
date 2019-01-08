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

package org.aion.monitor.api;


import org.aion.bridge.chain.base.rpc.dto.GetBlockByNumber;
import org.aion.bridge.chain.base.rpc.dto.GetBlockNumber;
import org.aion.bridge.chain.base.types.Word32;
import org.aion.bridge.chain.base.utility.ByteUtils;
import org.aion.bridge.chain.eth.types.EthBlock;
import org.aion.bridge.chain.eth.types.KeccakBloom;
import org.codehaus.commons.nullanalysis.NotNull;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;

public class EthHttpApi implements EthApi {
    private final HttpProvider provider;
    private final long timeoutSeconds;
    private String apiKey;
    private String url;
    private String name;
    private int id;

    public EthHttpApi(@NotNull final String url, String name, String apiKey, long timeoutSeconds) {
        this.url = url;
        this.name = name;
        this.provider = new HttpProvider(url);
        this.timeoutSeconds = timeoutSeconds;
        this.apiKey = apiKey;
        this.id = id;
    }

    public String getUrl() {
        return url;
    }
    public String getName() {
        return name;
    }
    public Integer getId() {
        return id;
    }


    @SuppressWarnings("unchecked")
    public Long getLatestBLockNumber() {
        Long blockNumber = null;
        Map<String, String> params = new LinkedHashMap<>();
        params.put("module", "proxy");
        params.put("action", "eth_blockNumber");
        params.put("apikey", apiKey);

        try {
            BigInteger num = (BigInteger) this.provider.send(params, GetBlockNumber.Response.class)
                    .thenApply((obj) -> ((GetBlockNumber.Response) obj).getNumber())
                    .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .get();
            blockNumber = num.longValueExact();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return blockNumber;
    }

    @SuppressWarnings("unchecked")
    public Optional<EthBlock> getBLock(Long blockNumber) {
        if (blockNumber == null)
            return Optional.empty();

        Optional<EthBlock> block = Optional.empty();

        Map<String, String> params = new LinkedHashMap<>();
        params.put("module", "proxy");
        params.put("action", "eth_getBlockByNumber");
        params.put("tag", "0x" + ByteUtils.binToHex(BigInteger.valueOf(blockNumber).toByteArray()).replaceFirst("^0*", ""));
        params.put("boolean", "false");
        params.put("apikey", apiKey);
        try {
            GetBlockByNumber.BlockJson blockJson = (GetBlockByNumber.BlockJson) this.provider.send(params, GetBlockByNumber.Response.class)
                    .thenApply(obj -> ((GetBlockByNumber.Response) obj).getResult())
                    .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .get();

            if (blockJson != null)
                block = Optional.of(new EthBlock(
                        blockJson.getBlockNumber().toBigInteger().longValueExact(),
                        new Word32(blockJson.getHash()),
                        new Word32(blockJson.getParentHash()),
                        new KeccakBloom(blockJson.getLogsBloom()),
                        blockJson.getTotalDifficulty().toBigInteger(),
                        blockJson.getTimestamp().toBigInteger().longValueExact(),
                        blockJson.getTransactionHashes().stream().map(Word32::new).collect(toList())));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return block;
    }

    public Integer getPeerCount() {
        throw new UnsupportedOperationException();
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

}


