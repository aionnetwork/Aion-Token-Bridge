DROP DATABASE IF EXISTS ethMonitor;

CREATE DATABASE ethMonitor;

USE ethMonitor;

CREATE TABLE eth_nodes (
  id  INT         NOT NULL AUTO_INCREMENT,
  url VARCHAR(200) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE node_variance_data (
  id             BIGINT(64) NOT NULL AUTO_INCREMENT,
  height_diff    INT(8)     NULL     DEFAULT 0,
  infura_diff    INT(8)     NULL     DEFAULT 0,
  etherscan_diff INT(8)     NULL     DEFAULT 0,
  block_num      BIGINT(64) NOT NULL DEFAULT 0,
  peer_count     INT(8)     NULL     DEFAULT 0,
  timestamp      TIMESTAMP  NULL     DEFAULT CURRENT_TIMESTAMP,
  node_id        INT        NOT NULL,
  PRIMARY KEY (id)
  #   INDEX fk_node_variance_data_1_idx (node_id ASC),
  #   CONSTRAINT fk_node_variance_data_1
  #   FOREIGN KEY (node_id)
  #   REFERENCES eth_nodes (id)
  #     ON DELETE NO ACTION
  #     ON UPDATE NO ACTION
)
  PARTITION BY RANGE COLUMNS (id) (
  PARTITION p0 VALUES LESS THAN (1000000),
  PARTITION p1 VALUES LESS THAN (2000000),
  PARTITION p2 VALUES LESS THAN ( MAXVALUE )
  );


