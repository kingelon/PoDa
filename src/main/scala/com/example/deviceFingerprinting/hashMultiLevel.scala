object MultiLevelDeviceFingerprinting {

  def main(args: Array[String]): Unit = {
    // Initialize Spark Session
    val spark = SparkSession.builder()
      .appName("Multi-Level Device Fingerprinting with Spark")
      .getOrCreate()

    // Load data from HBase
    val hbaseTable = "clickstream_data"
    val hbaseNamespace = "default"
    val clickstreamDF = spark.read
      .format("hbase")
      .option("hbase.table", s"$hbaseNamespace:$hbaseTable")
      .load()

    // Hashing UDF
    val hashUdf = udf((str: String) => {
      MessageDigest.getInstance("SHA-256")
        .digest(str.getBytes("UTF-8"))
        .map("%02x".format(_)).mkString
    })

    // Generate primary hash for device details
    val primaryHashDF = clickstreamDF.withColumn(
      "primaryHash",
      hashUdf(
        concat_ws(
          "|",
          col("userAgent"),
          col("deviceClass"),
          col("operatingSystemName"),
          col("operatingSystemVersion"),
          col("screenResolution")
        )
      )
    )

    // Generate geographical hash
    val geoHashDF = primaryHashDF.withColumn(
      "geoHash",
      hashUdf(
        concat_ws(
          "|",
          col("geoLocation.country"),
          col("geoLocation.region"),
          col("geoLocation.continent")
        )
      )
    )

    // Generate application context hash
    val appContextHashDF = geoHashDF.withColumn(
      "appContextHash",
      hashUdf(
        concat_ws(
          "|",
          col("datapointAppID"),
          col("datapointDataSource")
        )
      )
    )

    // Save results back to HBase
    appContextHashDF.write
      .format("hbase")
      .option("hbase.table", s"$hbaseNamespace:$hbaseTable")
      .save()
  }
}
