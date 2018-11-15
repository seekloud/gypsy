package com.neo.sk.gypsy.common

import javafx.scene.Scene
import javafx.stage.Stage
/**
  * @author zhaoyin
  * @date 2018/10/29  5:08 PM
  */
class StageContext(stage: Stage) {

  def showScene(scene: Scene,title: String = "Gypsy") = {
    stage.setScene(scene)
    stage.setTitle(title)
    stage.sizeToScene()
    stage.show()
  }

}
