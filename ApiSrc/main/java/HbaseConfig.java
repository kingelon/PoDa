package com.example.hbaseapi.config;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class HBaseConfig {

    @Bean
    public Connection hbaseConnection() throws IOException {
        Configuration config = HBaseConfiguration.create();
        return ConnectionFactory.createConnection(config);
    }
}
