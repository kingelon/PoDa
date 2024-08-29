package com.example

import org.apache.spark.sql.SparkSession
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.client.Scan
import org.apache.hadoop.hbase.mapreduce.TableInputFormat
import org.apache.spark.rdd.RDD
import org.apache.hadoop.hbase.io.ImmutableBytesWritable
import org.apache.hadoop.hbase.client.Result
import java.util.Properties
import scala.io.Source

object HBaseRowCount {
  def main(args: Array[String]): Unit = {
    if (args.length != 1) {
      println("Usage: HBaseRowCount <config-file-path>")
      System.exit(1)
    }

    val configFilePath = args(0)

    // Load configuration file
    val config = new Properties()
    config.load(Source.fromFile(configFilePath).bufferedReader())

    val hbaseTableName = config.getProperty("hbase.table.name")
    println(s"Counting rows in HBase table: $hbaseTableName")

    // Create Spark session
    val spark = SparkSession.builder
      .appName("HBaseRowCount")
      .getOrCreate()

    // Set up HBase configuration
    val conf = HBaseConfiguration.create()
    conf.set(TableInputFormat.INPUT_TABLE, hbaseTableName)

    // Create an RDD to scan the HBase table
    val hBaseRDD: RDD[(ImmutableBytesWritable, Result)] = spark.sparkContext.newAPIHadoopRDD(
      conf, classOf[TableInputFormat],
      classOf[ImmutableBytesWritable],
      classOf[Result]
    )

    // Count the number of rows in the HBase table
    val rowCount = hBaseRDD.count()
    println(s"Total number of rows in HBase table '$hbaseTableName': $rowCount")

    // Stop the Spark session
    spark.stop()
  }
}
