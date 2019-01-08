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

package org.aion.bridge.datastore;

@SuppressWarnings("WeakerAccess")
public class MySqlQuery {

    public static final String INTEGRITY_KEEPER = "status";

    // eth-side writes ---------------------------------------------------------------

    public static final String INSERT_ETH_FINALIZED_BUNDLE =
            "insert into eth_finalized_bundle " +
                    "(bundle_id, bundle_hash, eth_block_number, eth_block_hash, index_in_eth_block, transfers) " +
                    "values (?, ?, ?, ?, ?, ?);";

    public static final String INSERT_ETH_TRANSFER =
            "insert into eth_transfer " +
                    "(eth_tx_hash, bundle_id, bundle_hash, eth_address, aion_address, aion_transfer_amount) " +
                    "values (?, ?, ?, ?, ?, ?);";

    public static final String UPDATE_ETH_FINALIZED_BLOCK =
            "replace into status_eth_finalized_block " +
                    "(integrity_keeper, eth_block_number, eth_block_hash) " +
                    "values ('"+INTEGRITY_KEEPER+"', ?, ?);";

    public static final String UPDATE_ETH_FINALIZED_BUNDLE_ID =
            "replace into status_eth_finalized_bundle " +
                    "(integrity_keeper, bundle_id, bundle_hash) " +
                    "values ('"+INTEGRITY_KEEPER+"', ?, ?);";

    // aion-side writes ---------------------------------------------------------------

    public static final String INSERT_AION_FINALIZED_BUNDLE =
            "insert into aion_finalized_bundle " +
                    "(bundle_id, bundle_hash, aion_tx_hash, aion_block_number, aion_block_hash) " +
                    "values (?, ?, ?, ?, ?);";

    public static final String UPDATE_AION_FINALIZED_BUNDLE_ID =
            "replace into status_aion_finalized_bundle " +
                    "(integrity_keeper, bundle_id, bundle_hash) " +
                    "values ('"+INTEGRITY_KEEPER+"', ?, ?);";

    public static final String UPDATE_AION_FINALIZED_BLOCK =
            "replace into status_aion_finalized_block " +
                    "(integrity_keeper, aion_block_number, aion_block_hash) " +
                    "values ('"+INTEGRITY_KEEPER+"', ?, ?);";

    // notification-related writes ------------------------------------------------------

    public static final String UPDATE_AION_LATEST_BLOCK_NUMBER =
            "replace into status_aion_latest_block " +
                    "(integrity_keeper, aion_block_number) " +
                    "values ('"+INTEGRITY_KEEPER+"', ?);";

    public static final String UPDATE_AION_CONTRACT_BALANCE =
            "replace into status_aion_balance " +
                    "(integrity_keeper, aion_balance, aion_block_number) " +
                    "values ('bridge', ?, ?);";

    public static final String UPDATE_AION_RELAYER_BALANCE =
            "replace into status_aion_balance " +
                    "(integrity_keeper, aion_balance, aion_block_number) " +
                    "values ('relayer', ?, ?);";

    // reads ----------------------------------------------------------------------------

    public static final String GET_STATUS_ETH_FINALIZED_BUNDLE_ID =
            "select bundle_id from status_eth_finalized_bundle where integrity_keeper = '"+INTEGRITY_KEEPER+"';";

    public static final String GET_STATUS_AION_FINALIZED_BUNDLE_ID =
            "select bundle_id from status_aion_finalized_bundle where integrity_keeper = '"+INTEGRITY_KEEPER+"';";

    public static final String GET_STATUS_AION_LATEST_BLOCK_NUMBER =
            "select aion_block_number from status_aion_latest_block where integrity_keeper = '"+INTEGRITY_KEEPER+"';";

    public static final String GET_STATUS_AION_CONTRACT_BALANCE =
            "select aion_balance from status_aion_contract_balance where integrity_keeper = '"+INTEGRITY_KEEPER+"';";

    public static final String GET_STATUS_ETH_FINALIZED_BLOCK =
            "select eth_block_number, eth_block_hash from status_eth_finalized_block where integrity_keeper = '"+INTEGRITY_KEEPER+"';";

    public static final String GET_STATUS_AION_FINALIZED_BLOCK =
            "select aion_block_number, aion_block_hash from status_aion_finalized_block where integrity_keeper = '"+INTEGRITY_KEEPER+"';";

    public static final String GET_STATUS_ETH_FINALIZED_BUNDLE_FULL =
            "select bundle_id, bundle_hash, eth_block_number, eth_block_hash, index_in_eth_block, transfers from " +
                    "eth_finalized_bundle where bundle_id = " +
                    "(select bundle_id from status_eth_finalized_bundle where integrity_keeper = '"+INTEGRITY_KEEPER+"');";

    public static final String GET_ETH_FINALIZED_BUNDLE_RANGE =
            "select bundle_id, bundle_hash, eth_block_number, eth_block_hash, index_in_eth_block, transfers from " +
                    "eth_finalized_bundle where bundle_id between ? and ?;";

    public static final String GET_ETH_BUNDLE_CREATION_TIMESTAMP =
            "select updated from eth_finalized_bundle where bundle_id = ?";

    public static final String GET_AION_FINALIZED_BUNDLE_MAPPING =
            "select bundle_id, bundle_hash, aion_tx_hash, aion_block_number, aion_block_hash from " +
                    "aion_finalized_bundle where bundle_id between ? and ?;";

    public static final String GET_TRANSFER_VALUE_IN_BLOCK_RANGE =
            "SELECT aion_transfer_amount FROM eth_transfer WHERE bundle_id BETWEEN ? AND ?;";

    public static final String GET_GENERATED_BUNDLE_IDS =
            "SELECT bundle_id FROM eth_finalized_bundle";
}
