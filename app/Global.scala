import play.api.GlobalSettings
import play.api.Application
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Await
import scala.concurrent.duration._

object Global extends GlobalSettings {
  override def onStart(app: Application): Unit = {
    val ready = board.System.start()
    
    Await.ready(ready, 10 minutes)
  }
}