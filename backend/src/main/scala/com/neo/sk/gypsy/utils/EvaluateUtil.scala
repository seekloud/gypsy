package com.neo.sk.gypsy.utils

import java.io._

import org.slf4j.LoggerFactory

import scala.collection.mutable
/**
  * User: sky
  * Date: 2018/6/12
  * Time: 13:00
  * 本部分为评估函数实现
  */
object EvaluateUtil {
  private val log = LoggerFactory.getLogger(this.getClass)

  def getFakeTopCsv(filePath:String,flag:Boolean,resultMap:mutable.HashMap[Int,(Int,Int,Boolean)]) = {
    val file = new File(filePath)
    if (file.isFile && file.exists) {
      try {
        val in = new FileInputStream(filePath)
        val inReader = new InputStreamReader(in, "UTF-8")
        val bufferedReader = new BufferedReader(inReader)
        bufferedReader.lines().skip(1).forEach { line =>
          val target = line.split(",")
          if(flag){
            resultMap.put(target(0).toInt,(target(1).toInt,0,false))
          }else{
            resultMap.get(target(0).toInt).foreach{ r=>
              resultMap.update(target(0).toInt,(r._1,target(1).toInt,r._1==target(1).toInt))
            }
          }
        }
      } catch {
        case e: Exception =>
          resultMap.clear()
          println("get exception:" + e.getStackTrace)
      }
    }else{
      println(s"file--$filePath isn't exists.")
    }
  }

  def MicroP(id:Long,result_dir:String, evaluate_dir:String)={
    val resultMap:mutable.HashMap[Int,(Int,Int,Boolean)]=mutable.HashMap()  //(id,(evaluate,result))
    val countMap:mutable.HashMap[Int,(Int,Int)]=mutable.HashMap() //(id,(TP,FP))
    val t1=System.currentTimeMillis()
    getFakeTopCsv(AppSettings.storeFilePath+"/result/"+evaluate_dir,true,resultMap)
    getFakeTopCsv(AppSettings.storeFilePath+"/testResult/"+result_dir,false,resultMap)
    val size=resultMap.groupBy(_._2._1).size
    for(i <-0 until size){
      val tp=resultMap.count(r => r._2._1 == i && r._2._3)
      val tpFp=resultMap.count(r => r._2._2 == i)
      countMap.put(i,(tp,tpFp-tp))
    }
    val eTp=countMap.map(_._2._1).sum.toFloat
    val eFp=countMap.map(_._2._2).sum.toFloat
    if(resultMap.nonEmpty){
      evaluateActor ! FinishEvaluate(id,eTp/(eFp+eTp))
    }else{
      evaluateActor ! FinishEvaluate(id,0)
    }
    val t2=System.currentTimeMillis()
    log.info(s"MicroP cost ${t2-t1}")
  }


  def MacroF1(id:Long,result_dir:String, evaluate_dir:String)={
    val resultMap:mutable.HashMap[Int,(Int,Int,Boolean)]=mutable.HashMap()  //(id,(正确-evaluate,预测-result))
    val countMap:mutable.HashMap[Int,(Int,Int,Int,Float)]=mutable.HashMap() //(id,(TP,TP+FP,TP+FN,F1))
    val t1=System.currentTimeMillis()
    getFakeTopCsv(AppSettings.storeFilePath+"/result/"+evaluate_dir,true,resultMap)
    getFakeTopCsv(AppSettings.storeFilePath+"/testResult/"+result_dir,false,resultMap)
    val size=resultMap.groupBy(_._2._1).size
    for(i <-0 until size){
      val tp=resultMap.count(r => r._2._1 == i && r._2._3)
      val tpFp=resultMap.count(r => r._2._2 == i)
      val tpFn=resultMap.count(r=>r._2._1==i)
      countMap.put(i,(tp,tpFp,tpFn,0.0f))
    }
    countMap.foreach{r=>
      val P=r._2._1.toFloat/r._2._2.toFloat
      val R=r._2._1.toFloat/r._2._3.toFloat
      val F1=2*P*R/(P+R)
      countMap.update(r._1,(r._2._1,r._2._2,r._2._3,F1))
    }
    if(resultMap.nonEmpty){
      val grade= countMap.map(_._2._4).sum /countMap.size
      evaluateActor ! FinishEvaluate(id,grade)
      log.info(s"eee---$grade")
    }else{
      evaluateActor ! FinishEvaluate(id,0)
    }

    val t2=System.currentTimeMillis()
    log.info(s"MacroF1 cost ${t2-t1}")
  }


}
