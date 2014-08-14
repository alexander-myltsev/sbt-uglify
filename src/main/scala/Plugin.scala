package sbtuglify

import java.nio.charset.Charset

import sbt._

object Plugin extends AutoPlugin {
  import sbt.Keys._

  object autoImport {
    lazy val uglifyCompress = TaskKey[Seq[File]]("uglifyCompress", "Compiles .jsm javascript manifest files")
    lazy val charset = SettingKey[Charset]("charset", "Sets the character encoding used in file IO. Defaults to utf-8")
    lazy val downloadDirectory = SettingKey[File]("download-dir", "Directory to download ManifestUrls to")
    lazy val prettyPrint = SettingKey[Boolean]("pretty-print", "Whether to pretty print JavaScript (default false)")
    lazy val closureOptions = SettingKey[Array[String]]("options", "Compiler options")
    lazy val suffix = SettingKey[String]("suffix", "String to append to output filename (before file extension)")
  }

  import sbtuglify.Plugin.autoImport._

  override def trigger = allRequirements
  
  def closureOptionsSetting: Def.Initialize[Array[String]] =
    (streams, prettyPrint in uglifyCompress) apply {
      (out, prettyPrint) =>
        Array()
    }

  override val projectSettings: Seq[Setting[_]] =
    closureSettingsIn(Compile) ++ closureSettingsIn(Test)

  def closureSettingsIn(conf: Configuration): Seq[Setting[_]] =
    inConfig(conf)(closureSettings0 ++ Seq(
      sourceDirectory in uglifyCompress <<= (sourceDirectory in conf) { _ / "javascript" },
      resourceManaged in uglifyCompress <<= (resourceManaged in conf) { _ / "js" },
      downloadDirectory in uglifyCompress <<= (target in conf) { _ / "closure-downloads" },
      cleanFiles in uglifyCompress <<= (resourceManaged in uglifyCompress, downloadDirectory in uglifyCompress)(_ :: _ :: Nil),
      watchSources <<= (unmanagedSources in uglifyCompress)
    )) ++ Seq(
      cleanFiles <++= (cleanFiles in uglifyCompress in conf),
      watchSources <++= (watchSources in uglifyCompress in conf),
      resourceGenerators in conf <+= uglifyCompress in conf,
      compile in conf <<= (compile in conf).dependsOn(uglifyCompress in conf)
    )

  def closureSettings0: Seq[Setting[_]] = Seq(
    charset in uglifyCompress := Charset.forName("utf-8"),
    prettyPrint := false,
    closureOptions <<= closureOptionsSetting,
    includeFilter in uglifyCompress := "*.js*",
    excludeFilter in uglifyCompress := (".*" - ".") || ("externs") || HiddenFileFilter,
    suffix in uglifyCompress := "",
    unmanagedSources in uglifyCompress <<= closureSourcesTask,
    clean in uglifyCompress <<= closureCleanTask,
    uglifyCompress <<= closureCompilerTask
  )

  private def closureCleanTask =
    (streams, resourceManaged in uglifyCompress) map {
      (out, target) =>
        out.log.info("Cleaning generated JavaScript under " + target)
        IO.delete(target)
    }

  private def closureCompilerTask =
    (streams, sourceDirectory in uglifyCompress, resourceManaged in uglifyCompress,
     includeFilter in uglifyCompress, excludeFilter in uglifyCompress, charset in uglifyCompress,
     downloadDirectory in uglifyCompress, closureOptions in uglifyCompress, suffix in uglifyCompress) map {
      (out, sources, target, include, exclude, charset, downloadDir, options, suffix) => {
        val externs = 
          (sources / "externs").descendantsExcept("*.js", HiddenFileFilter).get.toList
        // compile changed sources
        (for {
          manifest <- sources.descendantsExcept("*.jsm", exclude).get
          outFile <- computeOutFile(sources, manifest, target, suffix)
          if ((manifest newerThan outFile) || 
              (Manifest.files(manifest, downloadDir, charset).exists(_ newerThan outFile)))
        } yield { (manifest, outFile) }) match {
          case Nil =>
            out.log.info("No JavaScript manifest files to compile")
          case xs =>
            out.log.info("Compiling %d jsm files to %s" format(xs.size, target))
            xs map doCompile(downloadDir, charset, out.log, options, externs)
            out.log.info("Compiled %s jsm files" format xs.size)
        }
        compiled(target)
      }
    }

  private def closureSourcesTask =
    (sourceDirectory in uglifyCompress, includeFilter in uglifyCompress, excludeFilter in uglifyCompress) map {
      (sourceDir, incl, excl) =>
         sourceDir.descendantsExcept(incl, excl).get
    }

  private def doCompile(downloadDir: File, charset: Charset, log: Logger, args: Array[String], externs: List[File])(pair: (File, File)) = {
    val (jsm, js) = pair
    log.info(                                                                                 "Compiling %s" format jsm)
    val srcFiles = Manifest.files(jsm, downloadDir, charset)
    val compiler = new Compressor(args)
    compiler.compile(srcFiles, externs, js, log)
  }

  private def compiled(under: File) = (under ** "*.js").get

  private def computeOutFile(sources: File, manifest: File, targetDir: File, suffix: String): Option[File] = {
    val outFile = IO.relativize(sources, manifest).get.replaceAll("""[.]jsm(anifest)?$""", "") + {
      if (suffix.length > 0) "-%s.js".format(suffix)
      else ".js"
    }
    Some(new File(targetDir, outFile))
  }
}
