package sbtuglify

import java.io.{Reader, InputStreamReader}
import javax.script.ScriptEngineManager

import sbt._

class Compressor(args: Array[String]) {

  private def reader(path: String): Reader = new InputStreamReader(getClass.getClassLoader.getResourceAsStream(path))
  
  def compile(sources: List[File], externs: List[File], target: File, log: Logger): Unit = {
    try {
      val manager = new ScriptEngineManager()
      val engine = manager.getEngineByName("nashorn")
      
      engine.put("uglify_args", args ++ externs ++ sources)
      engine.put("uglify_no_output", true)
      
      engine.eval(reader("javascript/parse-js.js"))
      engine.eval(reader("javascript/process.js"))
      engine.eval(reader("javascript/adapter/sys.js"))
      engine.eval(reader("javascript/adapter/JSON.js"))
      engine.eval(reader("javascript/adapter/Array.js"))
      engine.eval(reader("javascript/uglifyjs.js"))

      IO.createDirectory(file(target.getParent))
      IO.write(target, engine.eval("uglify();").asInstanceOf[String])
    } catch {
      case e: Exception => 
        log.error(e.toString)
        log.error(e.getStackTrace.mkString("\n\t"))
    }
  }
}
