package eit_group

import org.apache.spark.sql.DataFrame
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.feature.{OneHotEncoderEstimator, StringIndexer, VectorAssembler}
import org.apache.spark.ml.regression.{LinearRegression, LinearRegressionModel}

object SimpleModelObject extends App {
  class SimpleModel(name: String) {
    def train(training: DataFrame): PipelineModel = {

      val monthIndexer = new StringIndexer().setInputCol("Month").setOutputCol("MonthIndex")
      val dayIndexer = new StringIndexer().setInputCol("DayOfWeek").setOutputCol("DayOfWeekIndex")

      val encoder = new OneHotEncoderEstimator()
        .setInputCols(Array(monthIndexer.getOutputCol,dayIndexer.getOutputCol))
        .setOutputCols(Array("MonthVec", "DayOfWeekVec"))



      val assembler = new VectorAssembler()
        .setInputCols(Array("MonthVec","DepDelay", "NightFlight","DayOfWeekVec","TaxiOut","Distance"))
        .setOutputCol("features")

      val lr = new LinearRegression()
        .setFeaturesCol("features")
        .setLabelCol("ArrDelay")
        .setMaxIter(10)
        .setElasticNetParam(0.8)

      val pipeline = new Pipeline()
        .setStages(Array(monthIndexer,dayIndexer,encoder,assembler, lr))//,dayIndexer

      val lrModel = pipeline.fit(training)//.select("ArrDelay","DepDelay","MonthVec","DayOfWeekVec", "NightFlight"))
      println(s"Coefficients: ${lrModel.stages(4).asInstanceOf[LinearRegressionModel].coefficients}")
      println(s"Intercept: ${lrModel.stages(4).asInstanceOf[LinearRegressionModel].intercept}")
      val trainingSummary = lrModel.stages(4).asInstanceOf[LinearRegressionModel].summary
      println(s"numIterations: ${trainingSummary.totalIterations}")
      println(s"objectiveHistory: ${trainingSummary.objectiveHistory.toList}")
//      trainingSummary.residuals.show()
      println(s"RMSE: ${trainingSummary.rootMeanSquaredError}")
      println(s"r2: ${trainingSummary.r2}")
      lrModel
    }
    def evaluate(test: DataFrame, pipeline: PipelineModel): String = {
      val predictions = pipeline.transform(test)
      predictions.show(truncate=false)

      val evaluator = new RegressionEvaluator()
        .setMetricName("rmse")
        .setLabelCol("ArrDelay")
        .setPredictionCol("prediction")
      val rmse = evaluator.evaluate(predictions)

      println(s"${name}: Root-mean-square error = $rmse")
      rmse.toString()
    }
  }
}



