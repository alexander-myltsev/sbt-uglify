package sbtuglify

import java.io.{Reader, InputStreamReader}
import javax.script.ScriptEngineManager

import sbt.Keys._
import sbt._

object Plugin extends AutoPlugin {
  val UglifyConfig = config("uglify")
  
  object autoImport {
    lazy val pattern = settingKey[String]("File pattern")
    lazy val compress = taskKey[Unit]("Compresses files.")
  }

  import sbtuglify.Plugin.autoImport._

  override def trigger = allRequirements

  override val projectSettings = 
    inConfig(UglifyConfig)(compress <<= (streams, sourceDirectory in UglifyConfig) map { (out, src) =>
      val manager = new ScriptEngineManager()
      val engine = manager.getEngineByName("nashorn")
      
      engine.put("uglify_args", Array("src/main/bar.js"))
      engine.put("uglify_no_output", true)
      
      def reader(path: String): Reader = new InputStreamReader(getClass.getClassLoader.getResourceAsStream(path))
      
      engine.eval(reader("javascript/parse-js.js"))
      engine.eval(reader("javascript/process.js"))
      engine.eval(reader("javascript/adapter/sys.js"))
      engine.eval(reader("javascript/adapter/JSON.js"))
      engine.eval(reader("javascript/adapter/Array.js"))
      engine.eval(reader("javascript/uglifyjs.js"))

      val result = engine.eval("uglify();").asInstanceOf[String]
      out.log.info(result)
    })
}
