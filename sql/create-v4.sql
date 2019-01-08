create database bridgev4;

use bridgev4;

# Ethereum \/ \/ \/ \/ \/ \/ \/ 

create table eth_finalized_bundle (
  bundle_id bigint(64) unsigned primary key,
  bundle_hash char(64) not null,
  eth_block_number bigint(64) unsigned not null, 
  eth_block_hash char(64) not null,
  index_in_eth_block int unsigned not null,
  transfers text not null, # text can store maximum bytes 65,535; use mediumtext or longtext if neccesary
  updated timestamp default now() on update now()
);
#partition by range columns(bundle_id)(
#  partition p0 values less than (1000000),
#  partition p1 values less than (MAXVALUE)
#);

# this table is mainly for auditing and the UI
# transfers are denormalized, since for bridge application, we never query by anything other than bundle_id
create table eth_transfer (
  eth_tx_hash char(64) primary key,
  bundle_id bigint(64) unsigned not null,
  bundle_hash char(64) not null,
  eth_address char(64) not null,
  aion_address char(64) not null,
  aion_transfer_amount char(32) not null,
  updated timestamp default now() on update now()
);

create table status_eth_finalized_block (
  integrity_keeper enum('status') primary key,
  eth_block_number bigint(64) unsigned not null, 
  eth_block_hash char(64) not null,
  updated timestamp default now() on update now()
);

create table status_eth_finalized_bundle (
  integrity_keeper enum('status') primary key,  
  bundle_id bigint(64) unsigned not null,
  bundle_hash char(64) not null,
  updated timestamp default now() on update now()
);

# Aion \/ \/ \/ \/ \/ \/ \/ 

create table aion_finalized_bundle (
  bundle_id bigint(64) unsigned primary key,
  bundle_hash char(64) not null,
  aion_tx_hash char(64) not null,
  aion_block_number bigint(64) unsigned not null,
  aion_block_hash char(64) not null,
  updated timestamp default now() on update now()
);
#partition by range columns(bundle_id) (
#  partition p0 values less than (1000000),
#  partition p1 values less than (MAXVALUE));

create table status_aion_finalized_bundle (
  integrity_keeper enum('status') primary key,
  bundle_id bigint(64) unsigned not null,
  bundle_hash char(64) not null,
  updated timestamp default now() on update now()
);

create table status_aion_finalized_block (
  integrity_keeper enum('status') primary key,
  aion_block_number bigint(64) unsigned not null, 
  aion_block_hash char(64) not null,
  updated timestamp default now() on update now()
);

# for UI only: to get a 'ticker' of blocks moving on aion-side
create table status_aion_latest_block (
  integrity_keeper enum('status') primary key,
  aion_block_number bigint(64) unsigned not null,
  updated timestamp default now() on update now()
);

create table status_aion_balance (
  integrity_keeper enum('bridge', 'relayer') primary key,
  aion_balance char(64) not null,
  aion_block_number bigint(64) unsigned not null,
  updated timestamp default now() on update now()
);


