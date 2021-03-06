package eit_group

import org.apache.spark.sql.DataFrame
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.feature.{OneHotEncoderEstimator, VectorAssembler}
import org.apache.spark.ml.regression.{LinearRegression, LinearRegressionModel}
import org.apache.spark.ml.feature.StringIndexer

object SimpleModelObject extends App {
  class SimpleModel(name: String) {
    def train(training: DataFrame): PipelineModel = {
      val monthIndexer = new StringIndexer().setInputCol("Month").setOutputCol("MonthIndex")
      val companyIndexer = new StringIndexer().setInputCol("UniqueCarrier").setOutputCol("UniqueCarrierIndex")

      val encoder = new OneHotEncoderEstimator()
        .setInputCols(Array(monthIndexer.getOutputCol,companyIndexer.getOutputCol))
        .setOutputCols(Array("MonthVec","CompanyVec"))

      val assembler = new VectorAssembler()
        .setInputCols(Array("DepDelay","TaxiOut","MonthVec","CompanyVec", "NightFlight"))
        .setOutputCol("features")

      val lr = new LinearRegression()
        .setFeaturesCol("features")
        .setLabelCol("ArrDelay")
        .setMaxIter(10)
        .setElasticNetParam(0.5)

      val pipeline = new Pipeline()
        .setStages(Array(monthIndexer,companyIndexer,encoder,assembler, lr))

      val lrModel = pipeline.fit(training)
      println(s"Coefficients: ${lrModel.stages(4).asInstanceOf[LinearRegressionModel].coefficients}")
      println(s"Intercept: ${lrModel.stages(4).asInstanceOf[LinearRegressionModel].intercept}")
      val trainingSummary = lrModel.stages(4).asInstanceOf[LinearRegressionModel].summary
      println(s"numIterations: ${trainingSummary.totalIterations}")
      println(s"objectiveHistory: ${trainingSummary.objectiveHistory.toList}")
      println(s"RMSE: ${trainingSummary.rootMeanSquaredError}")
      println(s"r2: ${trainingSummary.r2}")
      lrModel
    }

    def evaluate(test: DataFrame, pipeline: PipelineModel): DataFrame = {
      val predictions = pipeline.transform(test)
      predictions.show(truncate=false)

      val evaluator = new RegressionEvaluator()
        .setMetricName("rmse")
        .setLabelCol("ArrDelay")
        .setPredictionCol("prediction")
      val rmse = evaluator.evaluate(predictions)

      println(s"$name: Root-mean-square error on test data set = $rmse")
      predictions
    }
  }
}



