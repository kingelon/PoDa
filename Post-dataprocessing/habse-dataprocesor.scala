package com.example.hbase

import org.apache.spark.sql.{SparkSession, DataFrame}
import org.apache.spark.sql.functions._
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.client.{ConnectionFactory, Connection, Table}
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.TableName
import org.apache.spark.sql.types._
import java.security.MessageDigest

object HBaseDataProcessor {

  def main(args: Array[String]): Unit = {
    // Configuration file path
    if (args.length != 1) {
      println("Usage: HBaseDataProcessor <config-file-path>")
      System.exit(1)
    }
    val configFilePath = args(0)

    println("Starting HBase Data Processor...")

    // Step 1: Load Configuration
    val config = new java.util.Properties()
    config.load(scala.io.Source.fromFile(configFilePath).bufferedReader())

    val hbaseNamespace = config.getProperty("hbase.namespace")
    val sourceTableName = config.getProperty("hbase.source.table")
    val targetTableName = config.getProperty("hbase.target.table")

    println(s"Source Table: $hbaseNamespace:$sourceTableName")
    println(s"Target Table: $hbaseNamespace:$targetTableName")

    // Step 2: Initialize Spark Session
    val spark = SparkSession.builder()
      .appName("HBase Data Processor")
      .getOrCreate()

    // Step 3: Read HBase Table as DataFrame
    val sourceDF = readHBaseTable(spark, hbaseNamespace, sourceTableName)
    println("Source Data Preview:")
    sourceDF.show(5)

    // Step 4: Data Transformation - Generate Hashes for Fingerprinting
    val processedDF = sourceDF.withColumn(
      "deviceFingerprint",
      hashUDF(
        concat_ws(",",
          col("userAgent"), col("deviceClass"), col("operatingSystemName"), col("screenResolution")
        )
      )
    )
    println("Processed Data Preview:")
    processedDF.show(5)

    // Step 5: Write Processed Data Back to HBase
    writeHBaseTable(processedDF, hbaseNamespace, targetTableName)

    println("Data processing completed successfully!")
    spark.stop()
  }

  // Function to read from HBase Table
  def readHBaseTable(spark: SparkSession, namespace: String, tableName: String): DataFrame = {
    spark.read.format("org.apache.hadoop.hbase.spark")
      .option("hbase.table", s"$namespace:$tableName")
      .option("hbase.columns.mapping", "rowkey STRING :key, cf:userAgent STRING, cf:deviceClass STRING, cf:operatingSystemName STRING, cf:screenResolution STRING")
      .load()
  }

  // Function to write to HBase Table
  def writeHBaseTable(df: DataFrame, namespace: String, tableName: String): Unit = {
    df.write.format("org.apache.hadoop.hbase.spark")
      .option("hbase.table", s"$namespace:$tableName")
      .option("hbase.columns.mapping", "rowkey STRING :key, cf:deviceFingerprint STRING")
      .save()
  }

  // Hashing UDF
  val hashUDF = udf((input: String) => {
    if (input != null) {
      MessageDigest.getInstance("SHA-256")
        .digest(input.getBytes("UTF-8"))
        .map("%02x".format(_)).mkString
    } else null
  })
}
