package com.example.hbaseapi.service;

import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.ColumnPrefixFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class HBaseService {

    @Autowired
    private Connection connection;

    public String getLatestValueAndTimestamp(String tableName, String id) throws IOException {
        Table table = connection.getTable(TableName.valueOf(tableName));
        try {
            Get get = new Get(Bytes.toBytes(id));
            get.addColumn(Bytes.toBytes("cf1"), Bytes.toBytes("latest_value"));
            get.addColumn(Bytes.toBytes("cf1"), Bytes.toBytes("latest_timestamp"));

            Result result = table.get(get);
            String latestValue = Bytes.toString(result.getValue(Bytes.toBytes("cf1"), Bytes.toBytes("latest_value")));
            long latestTimestamp = Bytes.toLong(result.getValue(Bytes.toBytes("cf1"), Bytes.toBytes("latest_timestamp")));

            return "Latest value: " + latestValue + ", Latest timestamp: " + latestTimestamp;
        } finally {
            table.close();
        }
    }

    public List<String> getHistoricalDataInRange(String tableName, String id, long startTs, long endTs) throws IOException {
        Table table = connection.getTable(TableName.valueOf(tableName));
        List<String> historicalData = new ArrayList<>();
        try {
            Scan scan = new Scan();
            scan.setRowPrefixFilter(Bytes.toBytes(id));
            scan.setFilter(new ColumnPrefixFilter(Bytes.toBytes("value_")));

            ResultScanner scanner = table.getScanner(scan);
            for (Result result : scanner) {
                result.rawCells().forEach(cell -> {
                    String columnName = Bytes.toString(cell.getQualifierArray(), cell.getQualifierOffset(), cell.getQualifierLength());
                    if (columnName.startsWith("value_")) {
                        long timestamp = Long.parseLong(columnName.substring("value_".length()));
                        if (timestamp >= startTs && timestamp <= endTs) {
                            String value = Bytes.toString(cell.getValueArray(), cell.getValueOffset(), cell.getValueLength());
                            historicalData.add("Value at " + timestamp + ": " + value);
                        }
                    }
                });
            }
        } finally {
            table.close();
        }
        return historicalData;
    }
}
