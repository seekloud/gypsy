import scala.util.Random

val a = new Random(System.currentTimeMillis())
for (i <- 1 until 20){
  println(a.nextInt(5))
}
