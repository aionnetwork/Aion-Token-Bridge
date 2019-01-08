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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

import static org.apache.commons.lang3.ObjectUtils.allNotNull;

public class DbConnectionManager {
    private String host;
    private String port;
    private String database;
    private String user;
    private String password;
    HikariDataSource dataSource;

    private DbConnectionManager(Builder builder) throws ClassNotFoundException {
        this.host = builder.host;
        this.port = builder.port;
        this.database = builder.database;
        this.user = builder.user;
        this.password = builder.password;
        Class.forName("com.mysql.cj.jdbc.Driver");

        HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(50);
        config.setMinimumIdle(10);
        config.setAutoCommit(false);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
        config.addDataSourceProperty("user", user);
        config.addDataSourceProperty("password", password);
        config.addDataSourceProperty("serverTimezone", "EST5EDT");
        config.addDataSourceProperty("useSSL", "false");

        dataSource = new HikariDataSource(config);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void closeAllConnections() throws SQLException {
        dataSource.close();
    }

    public static class Builder {

        // Required parameters
        String host;
        String port;
        String database;
        String user;
        String password;

        public Builder setHost(String x) {host = x; return this;}
        public Builder setPort(String x) {port = x; return this;}
        public Builder setDatabase(String x) {database = x; return this;}
        public Builder setUser(String x) {user = x; return this;}
        public Builder setPassword(String x) {password = x; return this;}

        public DbConnectionManager build() throws ClassNotFoundException {
            if(allNotNull(host, port, database, user, password) && validate())
                return new DbConnectionManager(this);
            else
                throw new IllegalStateException();
        }

        private boolean validate() {
            Integer intPort = Integer.parseInt(this.port);

            if(intPort < 0 || intPort > 65535)
                return false;

            return true;
        }

    }
}
