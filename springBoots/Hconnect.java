package com.example.hbaseapi;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SimpleSpringBootTest {

    public static void main(String[] args) {
        // Start Spring Boot application
        SpringApplication.run(SimpleSpringBootTest.class, args);

        // Test HBase connection immediately after starting Spring Boot
        try {
            Configuration config = HBaseConfiguration.create();
            System.out.println("Attempting to establish HBase connection...");
            Connection connection = ConnectionFactory.createConnection(config);

            if (connection != null && !connection.isClosed()) {
                System.out.println("HBase connection established successfully!");
            } else {
                System.err.println("Failed to establish HBase connection.");
            }

            // Clean up connection
            connection.close();
        } catch (Exception e) {
            System.err.println("Error while establishing HBase connection: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
