package com.neo.sk.gypsy.common

import javafx.scene.Scene
import javafx.stage.Stage
/**
  * @author zhaoyin
  * 2018/10/29  5:08 PM
  */

object StageContext {
  trait StageListener {
    def onCloseRequest():Unit
  }
}
class StageContext(stage: Stage) {

  import StageContext._

  def getStage: Stage = stage

  def showScene(scene: Scene,title: String = "Gypsy", flag: Boolean) = {
//    stage.setScene(scene)
//    stage.setTitle(title)
//    stage.sizeToScene()
//    stage.show()

    stage.setScene(scene)
    stage.setTitle(title)
    stage.sizeToScene()
    stage.centerOnScreen()
    if(flag){
      stage.setFullScreen(true)
    }
    stage.getWidth
    stage.show()
  }

  var stageListener: StageListener = _

  stage.setOnCloseRequest(_ => stageListener.onCloseRequest())

  def setStageListener(listener: StageListener) = {
    stageListener = listener
  }

  def closeStage()={
    stage.close()
    System.exit(0)
  }

}
