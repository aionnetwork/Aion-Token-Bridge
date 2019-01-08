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

package org.aion.monitor.datastore;

import org.aion.bridge.chain.db.PersistenceServiceException;
import org.aion.bridge.datastore.DbConnectionManager;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseOperations {

    private final DbConnectionManager ds;

    public DatabaseOperations(@Nonnull DbConnectionManager ds) {
        this.ds = ds;
    }

    public Integer insertEthNode(String url) throws PersistenceServiceException {
        int id = selectUrl(url);
        if (id < 0) {
            try (Connection c = ds.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(MySqlQuery.INSERT_INTO_ETH_NODE)) {

                    ps.setString(1, url);
                    ps.addBatch();
                    ps.executeBatch();

                    ResultSet rs = c.prepareStatement(MySqlQuery.SELECT_LAST_INSERT_ID).executeQuery();
                    rs.last();

                    id = rs.getInt(1);
                    c.commit();
                } catch (SQLException e) {
                    try {
                        c.rollback();
                    } catch (SQLException f) {
                        System.err.println("Failed to rollback commit on SQLException " + e);
                        throw new PersistenceServiceException(f);
                    }
                    System.err.println("SQLException caught; Commit rolled back successfully " + e);
                    throw new PersistenceServiceException(e);
                }
            } catch (SQLException e) {
                System.err.println("Failed to obtain a datasource connection " + e);
                throw new PersistenceServiceException(e);
            }
        }
        return id;
    }

    public void insertVarianceData(Integer nodeId, Long blockNum, Long height_diff, Long infura_diff, Long etherscan_diff, int peerCount) throws PersistenceServiceException {

            try (Connection c = ds.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(MySqlQuery.INSERT_INTO_NODE_VARIANCE_DATA)) {

                    ps.setLong(1, blockNum);
                    ps.setLong(2, height_diff);
                    ps.setLong(3, infura_diff);
                    ps.setLong(4, etherscan_diff);
                    ps.setInt(5, peerCount);
                    ps.setInt(6, nodeId);

                    ps.addBatch();
                    int[] updates = ps.executeBatch();
                    if(updates.length != 1)
                        throw new IllegalStateException("MySqlDatastore.executeBatchUpdate failed to update row.");

                    c.commit();
                } catch (SQLException e) {
                    try {
                        c.rollback();
                    } catch (SQLException f) {
                        System.err.println("Failed to rollback commit on SQLException " + e);
                        throw new PersistenceServiceException(f);
                    }
                    System.err.println("SQLException caught; Commit rolled back successfully " + e);
                    throw new PersistenceServiceException(e);
                }
            } catch (SQLException e) {
                System.err.println("Failed to obtain a datasource connection " + e);
                throw new PersistenceServiceException(e);
            }
        }


    private int selectUrl(String url) throws PersistenceServiceException {
        try (Connection c = ds.getConnection()) {
            try {
                PreparedStatement ps = c.prepareStatement(MySqlQuery.SELECT_FROM_ETH_NODE);
                ps.setString(1, url);
                ResultSet rs = ps.executeQuery();

                rs.last();
                int size = rs.getRow();

                if (size > 1)
                    throw new IllegalStateException("result set size cannot be > 1");

                int id = -1;
                if (size == 1) id = rs.getInt(1);

                c.commit();
                return id;

            } catch (SQLException e) {
                throw new PersistenceServiceException(e);
            }
        } catch (SQLException e) {
            System.err.println("Failed to obtain a datasource connection " + e);
            throw new PersistenceServiceException(e);
        }
    }
}
