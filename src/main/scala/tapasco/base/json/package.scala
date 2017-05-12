package de.tu_darmstadt.cs.esa.tapasco.base
import  de.tu_darmstadt.cs.esa.tapasco.Implicits._
import  de.tu_darmstadt.cs.esa.tapasco.json._
import  de.tu_darmstadt.cs.esa.tapasco.jobs._
import  de.tu_darmstadt.cs.esa.tapasco.jobs.json._
import  play.api.libs.json._
import  play.api.libs.json.Reads._
import  play.api.libs.json.Writes._
import  play.api.libs.functional.syntax._
import  java.nio.file._
import  java.time.format.DateTimeFormatter, java.time.LocalDateTime

/**
 * The `json` package contains implicit Reads/Writes/Formats instances to serialize and
 * deserialize basic TPC entities to and from Json format.
 **/
package object json {
  private final val MAX_SLOTS = 128
  private def totalCountOk(c: Seq[Composition.Entry]): Boolean =
    (c map (_.count) fold 0) (_ + _) <= MAX_SLOTS

  /* @{ TargetDesc */
  implicit val targetReads: Reads[TargetDesc] = (
    (JsPath \ "Architecture").read[String] ~
    (JsPath \ "Platform").read[String]
  ) (TargetDesc.apply _)

  implicit val targetWrites: Writes[TargetDesc] = (
    (JsPath \ "Architecture").write[String] ~
    (JsPath \ "Platform").write[String]
  ) (unlift(TargetDesc.unapply _))
  /* TargetDesc @} */

  /* @{ Architecture */
  implicit val reads: Reads[Architecture] = (
    (JsPath \ "DescPath").readNullable[Path].map (_ getOrElse Paths.get("N/A")) ~
    (JsPath \ "Name").read[String] ~
    (JsPath \ "TclLibrary").readNullable[Path].map (_ getOrElse Paths.get("test")) ~
    (JsPath \ "Description").readNullable[String].map (_ getOrElse "") ~
    (JsPath \ "ValueArgTemplate").readNullable[Path].map (_ getOrElse Paths.get("valuearg.directives.template")) ~
    (JsPath \ "ReferenceArgTemplate").readNullable[Path].map (_ getOrElse Paths.get("referencearg.directives.template")) ~
    (JsPath \ "AdditionalSteps").readNullable[Seq[String]].map (_ getOrElse Seq())
  ) (Architecture.apply _)
  implicit val writes: Writes[Architecture] = (
    (JsPath \ "DescPath").write[Path].transform((js: JsObject) => js - "DescPath") ~
    (JsPath \ "Name").write[String] ~
    (JsPath \ "TclLibrary").write[Path] ~
    (JsPath \ "Description").write[String] ~
    (JsPath \ "ValueArgTemplate").write[Path] ~
    (JsPath \ "ReferenceArgTemplate").write[Path] ~
    (JsPath \ "AdditionalSteps").write[Seq[String]]
  ) (unlift(Architecture.unapply _))
  /* Architecture @}*/

  /* @{ Benchmark */
  private val dtf = DateTimeFormatter.ofPattern("yyyy-MM-d kk:mm:ss")

  implicit object FormatsLocalDateTime extends Format[LocalDateTime] {
    def reads(json: JsValue): JsResult[LocalDateTime] = json match {
      case JsString(s) => {
        try { JsSuccess(LocalDateTime.parse(s, dtf)) }
        catch { case e: Exception => JsError(Seq(JsPath() -> Seq(JsonValidationError("validation.error.expected.date")))) }
      }
      case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("validation.error.expected.jsstring"))))
    }
    def writes(ldt: LocalDateTime): JsValue = JsString(dtf.format(ldt))
  }

  implicit val libraryVersionsFormat: Format[LibraryVersions] = (
      (JsPath \ "Platform API").format[String] ~
      (JsPath \ "TPC API").format[String]
    ) (LibraryVersions.apply _, unlift(LibraryVersions.unapply _))

  implicit val hostFormat: Format[Host] = (
      (JsPath \ "Machine").format[String] ~
      (JsPath \ "Node").format[String] ~
      (JsPath \ "Operating System").format[String] ~
      (JsPath \ "Release").format[String] ~
      (JsPath \ "Version").format[String]
    ) (Host.apply _, unlift(Host.unapply _))

  implicit val transferSpeedMeasurementFormat: Format[TransferSpeedMeasurement] = (
      (JsPath \ "Chunk Size").format[Int] ~
      (JsPath \ "Read").format[Double] ~
      (JsPath \ "Write").format[Double] ~
      (JsPath \ "ReadWrite").format[Double]
    ) (TransferSpeedMeasurement.apply _, unlift(TransferSpeedMeasurement.unapply _))

  implicit val benchmarkReads: Reads[Benchmark] = (
      (JsPath \ "DescPath").readNullable[Path].map(_ getOrElse Paths.get("N/A")) ~
      (JsPath \ "Timestamp").read[LocalDateTime] ~
      (JsPath \ "Host").read[Host] ~
      (JsPath \ "Library Versions").read[LibraryVersions] ~
      (JsPath \ "Transfer Speed").read[Seq[TransferSpeedMeasurement]] ~
      (JsPath \ "Job Roundtrip Overhead").read[Double]
    ) (Benchmark. apply _)
  implicit val benchmarkWrites: Writes[Benchmark] = (
      (JsPath \ "DescPath").write[Path].transform((js: JsObject) => js - "DescPath") ~
      (JsPath \ "Timestamp").write[LocalDateTime] ~
      (JsPath \ "Host").write[Host] ~
      (JsPath \ "Library Versions").write[LibraryVersions] ~
      (JsPath \ "Transfer Speed").write[Seq[TransferSpeedMeasurement]] ~
      (JsPath \ "Job Roundtrip Overhead").write[Double]
    ) (unlift(Benchmark.unapply _))
  /* Benchmark @} */

  /* @{ Composition.Entry */
  implicit val compositionEntryReads: Reads[Composition.Entry] = (
    (JsPath \ "Kernel").read[String] (minLength[String](1)) ~
    (JsPath \ "Count").read[Int] (min(1) keepAnd max(MAX_SLOTS))
  ) (Composition.Entry.apply _)

  implicit val compositionEntryWrites: Writes[Composition.Entry] = (
    (JsPath \ "Kernel").write[String] ~
    (JsPath \ "Count").write[Int]
  ) (unlift(Composition.Entry.unapply _))
  /* Composition.Entry @} */

  /* @{ Composition */
  implicit val compositionReads: Reads[Composition] = (
    (JsPath \ "DescPath").readNullable[Path].map(_ getOrElse Paths.get("N/A")) ~
    (JsPath \ "Description").readNullable[String] ~
    (JsPath \ "Composition").read[Seq[Composition.Entry]]
      (/*minLength[Seq[Composition.Entry]](1) keepAnd*/
       verifying[Seq[Composition.Entry]](totalCountOk))
  ) (Composition.apply _)
  implicit val compositionWrites: Writes[Composition] = (
    (JsPath \ "DescPath").write[Path].transform((js: JsObject) => js - "DescPath") ~
    (JsPath \ "Description").writeNullable[String] ~
    (JsPath \ "Composition").write[Seq[Composition.Entry]]
  ) (unlift(Composition.unapply _))
  /* Composition @} */

  /* @{ Core */
  implicit val coreReads: Reads[Core] = (
    (JsPath \ "DescPath").readNullable[Path].map(_ getOrElse Paths.get("N/A")) ~
    (JsPath \ "ZipFile").read[Path] ~
    (JsPath \ "Name").read[String] (minLength[String](1)) ~
    (JsPath \ "Id").read[Kernel.Id] (min(1)) ~
    (JsPath \ "Version").read[String] (minLength[String](1)) ~
    (JsPath \ "Target").read[TargetDesc] ~
    (JsPath \ "Description").readNullable[String] ~
    (JsPath \ "AverageClockCycles").readNullable[Int]
  ) (Core.apply _)
  implicit val coreWrites: Writes[Core] = (
    (JsPath \ "DescPath").write[Path].transform((js: JsObject) => js - "DescPath") ~
    (JsPath \ "ZipFile").write[Path] ~
    (JsPath \ "Name").write[String] ~
    (JsPath \ "Id").write[Int] ~
    (JsPath \ "Version").write[String] ~
    (JsPath \ "Target").write[TargetDesc] ~
    (JsPath \ "Description").writeNullable[String] ~
    (JsPath \ "AverageClockCycles").writeNullable[Int]
  ) (unlift(Core.unapply _))
  /* Core @} */

  /* @{ Features */
  private val readsLEDFeature: Reads[Feature] = (
    (JsPath \ "Feature").read[String] (verifying[String](_ equals "LED")) ~>
    (JsPath \ "Enabled").readNullable[Boolean].map (_ getOrElse true)
  ) .fmap(Feature.LED.apply _)
  private val writesLEDFeature: Writes[Feature.LED] = (
    (JsPath \ "Feature").write[String] ~
    (JsPath \ "Enabled").write[Boolean]
  ) (unlift(Feature.LED.unapply _ andThen (_ map (("LED", _)))))

  private val readsOLEDFeature: Reads[Feature] = (
    (JsPath \ "Feature").read[String] (verifying[String](_ equals "OLED")) ~>
    (JsPath \ "Enabled").readNullable[Boolean].map (_ getOrElse true)
  ) .fmap(Feature.OLED.apply _)
  private val writesOLEDFeature: Writes[Feature.OLED] = (
    (JsPath \ "Feature").write[String] ~
    (JsPath \ "Enabled").write[Boolean]
  ) (unlift(Feature.OLED.unapply _ andThen (_ map (("OLED", _)))))

  private val readsCacheFeature: Reads[Feature] = (
    (JsPath \ "Feature").read[String] (verifying[String](_ equals "Cache")) ~>
    (JsPath \ "Enabled").readNullable[Boolean].map (_ getOrElse true) ~
    (JsPath \ "Size").read[Int] ~
    (JsPath \ "Associativity").read[Int] (verifying[Int](n => n == 2 || n == 4))
  ) (Feature.Cache.apply _)
  private implicit val writesCacheFeature: Writes[Feature.Cache] = (
    (JsPath \ "Feature").write[String] ~
    (JsPath \ "Enabled").write[Boolean] ~
    (JsPath \ "Size").write[Int] ~
    (JsPath \ "Associativity").write[Int]
  ) (unlift(Feature.Cache.unapply _ andThen (_ map ("Cache" +: _))))

  private val readsDebugFeature: Reads[Feature] = (
    (JsPath \ "Feature").read[String] (verifying[String](_ equals "Debug")) ~>
    (JsPath \ "Enabled").readNullable[Boolean].map (_ getOrElse true) ~
    (JsPath \ "Depth").readNullable[Int] ~
    (JsPath \ "Stages").readNullable[Int] ~
    (JsPath \ "Use Defaults").readNullable[Boolean] ~
    (JsPath \ "Nets").readNullable[Seq[String]]
  ) (Feature.Debug.apply _)
  private implicit val writesDebugFeature: Writes[Feature.Debug] = (
    (JsPath \ "Feature").write[String] ~
    (JsPath \ "Enabled").write[Boolean] ~
    (JsPath \ "Depth").writeNullable[Int] ~
    (JsPath \ "Stages").writeNullable[Int] ~
    (JsPath \ "Use Defaults").writeNullable[Boolean] ~
    (JsPath \ "Nets").writeNullable[Seq[String]]
  ) (unlift(Feature.Debug.unapply _ andThen (_ map ("Debug" +: _))))

  implicit val readsFeature: Reads[Feature] =
    readsLEDFeature | readsOLEDFeature | readsCacheFeature | readsDebugFeature

  implicit object writesFeature extends Writes[Feature] {
    def writes(f: Feature): JsValue = f match {
      case f: Feature.LED   => writesLEDFeature.writes(f)
      case f: Feature.OLED  => writesOLEDFeature.writes(f)
      case f: Feature.Cache => writesCacheFeature.writes(f)
      case f: Feature.Debug => writesDebugFeature.writes(f)
    }
  }
  /* Features @} */

  /* @{ Kernel.Argument */
  implicit object kernelPassingConventionFormat extends Format[Kernel.PassingConvention] {
    def reads(json: JsValue): JsResult[Kernel.PassingConvention] = json match {
      case JsString(str) => JsSuccess(Kernel.PassingConvention(str))
      case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("expected.jsstring.for.passing.convention"))))
    }
    def writes(pc: Kernel.PassingConvention): JsValue = JsString(pc.toString)
  }
  implicit val kernelArgumentReads: Reads[Kernel.Argument] = (
    (JsPath \ "Name").read[String] (minLength[String](1)) ~
    (JsPath \ "Passing").readNullable[Kernel.PassingConvention].map (_ getOrElse Kernel.PassingConvention.ByValue)
  ) (Kernel.Argument.apply _)
  implicit val kernelArgumentWrites: Writes[Kernel.Argument] = (
    (JsPath \ "Name").write[String] ~
    (JsPath \ "Passing").write[Kernel.PassingConvention]
  ) (unlift(Kernel.Argument.unapply _))
  /* Kernel.Argument @} */

  /* @{ Kernel */
  implicit val kernelReads: Reads[Kernel] = (
    (JsPath \ "DescPath").readNullable[Path].map (_ getOrElse Paths.get("N/A")) ~
    (JsPath \ "Name").read[String] (verifying[String](_.length > 0)) ~
    (JsPath \ "TopFunction").read[String] (verifying[String](_.length > 0)) ~
    (JsPath \ "Id").read[Kernel.Id] (verifying[Kernel.Id](_ > 0)) ~
    (JsPath \ "Version").read[String] (verifying[String](_.length > 0)) ~
    (JsPath \ "Files").read[Seq[Path]] (verifying[Seq[Path]](_.length > 0)) ~
    (JsPath \ "TestbenchFiles").readNullable[Seq[Path]].map (_ getOrElse Seq()) ~
    (JsPath \ "Description").readNullable[String] ~
    (JsPath \ "CompilerFlags").readNullable[Seq[String]].map (_ getOrElse Seq()) ~
    (JsPath \ "TestbenchCompilerFlags").readNullable[Seq[String]].map (_ getOrElse Seq()) ~
    (JsPath \ "Arguments").read[Seq[Kernel.Argument]] ~
    (JsPath \ "OtherDirectives").readNullable[Path]
  ) (Kernel.apply _)
  implicit val kernelWrites: Writes[Kernel] = (
    (JsPath \ "DescPath").write[Path].transform((js: JsObject) => js - "DescPath") ~
    (JsPath \ "Name").write[String] ~
    (JsPath \ "TopFunction").write[String] ~
    (JsPath \ "Id").write[Int] ~
    (JsPath \ "Version").write[String] ~
    (JsPath \ "Files").write[Seq[Path]] ~
    (JsPath \ "TestbenchFiles").write[Seq[Path]] ~
    (JsPath \ "Description").writeNullable[String] ~
    (JsPath \ "CompilerFlags").write[Seq[String]] ~
    (JsPath \ "TestbenchCompilerFlags").write[Seq[String]] ~
    (JsPath \ "Arguments").write[Seq[Kernel.Argument]] ~
    (JsPath \ "OtherDirectives").writeNullable[Path]
  ) (unlift(Kernel.unapply _))
  /* Kernel @} */

  /* @{ Platform */
  // scalastyle:off magic.number
  implicit def platformReads: Reads[Platform] = (
    (JsPath \ "DescPath").readNullable[Path].map (_ getOrElse Paths.get("N/A")) ~
    (JsPath \ "Name").read[String] (minLength[String](1)) ~
    (JsPath \ "TclLibrary").read[Path] ~
    (JsPath \ "Part").read[String] (minLength[String](1)) ~
    (JsPath \ "BoardPart").read[String] (minLength[String](4)) ~
    (JsPath \ "BoardPreset").read[String] (minLength[String](4)) ~
    (JsPath \ "TargetUtilization").read[Int] (min(5) keepAnd max(100)) ~
    (JsPath \ "SupportedFrequencies").readNullable[Seq[Int]] (minLength[Seq[Int]](1)) .map (_ getOrElse (50 to 450 by 5)) ~
    (JsPath \ "SlotCount").read[Int] (min(1) keepAnd max(255)) ~
    (JsPath \ "Description").readNullable[String] (minLength[String](1)) ~
    (JsPath \ "Harness").readNullable[Path] ~
    (JsPath \ "API").readNullable[Path] ~
    (JsPath \ "TestbenchTemplate").readNullable[Path] ~
    (JsPath \ "Benchmark").readNullable[Path]
  ) (Platform.apply _)
  // scalastyle:on magic.number
  implicit def platformWrites: Writes[Platform] = (
    (JsPath \ "DescPath").write[Path].transform((js: JsObject) => js - "DescPath") ~
    (JsPath \ "Name").write[String] ~
    (JsPath \ "TclLibrary").write[Path] ~
    (JsPath \ "Part").write[String] ~
    (JsPath \ "BoardPart").write[String] ~
    (JsPath \ "BoardPreset").write[String] ~
    (JsPath \ "TargetUtilization").write[Int] ~
    (JsPath \ "SupportedFrequencies").write[Seq[Int]] ~
    (JsPath \ "SlotCount").write[Int] ~
    (JsPath \ "Description").writeNullable[String] ~
    (JsPath \ "Harness").writeNullable[Path] ~
    (JsPath \ "API").writeNullable[Path] ~
    (JsPath \ "TestbenchTemplate").writeNullable[Path] ~
    (JsPath \ "Benchmark").writeNullable[Path]
  ) (unlift(Platform.unapply _))
  /* Platform @} */

  /* @{ Configuration */
  implicit val configurationReads: Reads[Configuration] = (
    (JsPath \ "DescPath").readNullable[Path].map (_ getOrElse Paths.get("N/A")) ~
    (JsPath \ "ArchDir").readNullable[Path].map (_ getOrElse Paths.get("arch")) ~
    (JsPath \ "PlatformDir").readNullable[Path].map (_ getOrElse Paths.get("platform")) ~
    (JsPath \ "KernelDir").readNullable[Path].map (_ getOrElse Paths.get("kernel")) ~
    (JsPath \ "CoreDir").readNullable[Path].map (_ getOrElse Paths.get("core")) ~
    (JsPath \ "CompositionDir").readNullable[Path].map (_ getOrElse Paths.get("bd")) ~
    (JsPath \ "LogFile").readNullable[Path] ~
    (JsPath \ "Slurm").readNullable[Boolean].map (_ getOrElse false) ~
    (JsPath \ "Jobs").read[Seq[Job]]
  ) (ConfigurationImpl.apply _)
  implicit private val configurationWrites: Writes[ConfigurationImpl] = (
    (JsPath \ "DescPath").write[Path].transform((js: JsObject) => js - "DescPath") ~
    (JsPath \ "ArchDir").write[Path] ~
    (JsPath \ "PlatformDir").write[Path] ~
    (JsPath \ "KernelDir").write[Path] ~
    (JsPath \ "CoreDir").write[Path] ~
    (JsPath \ "CompositionDir").write[Path] ~
    (JsPath \ "LogFile").writeNullable[Path] ~
    (JsPath \ "Slurm").write[Boolean] ~
    (JsPath \ "Jobs").write[Seq[Job]]
  ) (unlift(ConfigurationImpl.unapply _))
  implicit object ConfigurationWrites extends Writes[Configuration] {
    def writes(c: Configuration): JsValue = c match {
      case ci: ConfigurationImpl => configurationWrites.writes(ci)
      case _ => throw new Exception("unknown Configuration implementation")
    }
  }
  /* Configuration @} */
}
// vim: foldmarker=@{,@} foldmethod=marker foldlevel=0