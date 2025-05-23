/*
 *
 * Copyright (c) 2014-2020 Embedded Systems and Applications, TU Darmstadt.
 *
 * This file is part of TaPaSCo
 * (see https://github.com/esa-tu-darmstadt/tapasco).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
/**
  * @file Import.scala
  * @brief The Import activity import an IP-XACT IP core in a .zip file
  *        into the currently configured core library for TPC. If no
  *        synthesis report can be found, it will use the EvaluateIP
  *        activity to generate an out-of-context synthesis report to
  *        estimate area utilization and max. operating frequency.
  * @authors J. Korinth, TU Darmstadt (jk@esa.cs.tu-darmstadt.de)
  **/
package tapasco.activity

import java.nio.file._

import tapasco.base._
import tapasco.base.json._
import tapasco.filemgmt.FileAssetManager
import tapasco.util._

/**
  * The Import activity imports an existing IP-XACT core into the cores library
  * of the current TPC configuration. Reports can either be supplied manually,
  * or will be generated by out-of-context synthesis, if not found.
  **/
object Import {
  private implicit final val logger =
    tapasco.Logging.logger(getClass)

  /**
    * Import the given IP-XACT .zip file as Kernel with given id for the given target.
    * If no XML synthesis report is found (%NAME%_export.xml), will perform out-of-contex
    * synthesis and place-and-route for the Core to produce area and Fmax estimates.
    *
    * @param zip           Path to IP-XACT. zip.
    * @param id            Kernel ID.
    * @param t             Target Architecture + Platform combination to import for.
    * @param acc           Average clock cycle count for a job execution on the PE (optional).
    * @param runEvaluation Do not perform out-of-context synthesis for resource estimation (optional).
    * @param cfg           Implicit [[Configuration]].
    **/
  def apply(zip: Path, id: Kernel.Id, t: Target, acc: Option[Long], runEvaluation: Option[Boolean],
            optimization: Int, synthOptions: Option[String] = None)
           (implicit cfg: Configuration): Boolean = {
    // get VLNV from the file
    val vlnv = VLNV.fromZip(zip)
    logger.trace("found VLNV in zip " + zip + ": " + vlnv)
    // extract version and name from VLNV, create Core
    val c = Core(
      descPath = zip.resolveSibling("core.json"),
      _zipPath = zip.getFileName,
      name = vlnv.name,
      id = id,
      version = vlnv.version.toString,
      _target = t,
      Some("imported from %s on %s".format(zip.toAbsolutePath.toString, java.time.LocalDateTime.now().toString)),
      acc)

    // write core.json to output directory (as per config)
    val p = cfg.outputDir(c, t).resolve("ipcore").resolve("core.json")
    importCore(c, t, p, vlnv, runEvaluation, optimization, synthOptions)
  }

  /**
    * Imports the IP-XACT .zip to the default path structure (ipcore/) and performs
    * out-of-context synthesis (if no report from HLS was found and skipEval was not set).
    *
    * @param c              Core description.
    * @param t              Target platform and architecture.
    * @param p              Output path for core description file.
    * @param runEvaluation  Run out-of-context synthesis step (optional).
    * @param cfg            Implicit [[Configuration]].
    **/
  private def importCore(c: Core, t: Target, p: Path, vlnv: VLNV, runEvaluation: Option[Boolean], optimization: Int,
                         synthOptions: Option[String])
                        (implicit cfg: Configuration): Boolean = {
    Files.createDirectories(p.getParent)
    logger.trace("created output directories: {}", p.getParent.toString)

    // link original .zip file, if possible; otherwise copy
    val linkp = cfg.outputDir(c, t).resolve("ipcore").resolve("%s.zip".format(vlnv.name))
    if (!linkp.toFile.equals(c.zipPath.toAbsolutePath.toFile)) {
      Files.createDirectories(linkp.getParent)
      logger.trace("created directories: {}", linkp.getParent.toString)
      if (linkp.toFile.exists) {
        logger.debug("file {} already exists, skipping copy/link step")
      } else {
        logger.trace("creating symbolic link {} -> {}", linkp: Any, c.zipPath.toAbsolutePath)
        try {
          java.nio.file.Files.createSymbolicLink(linkp, c.zipPath.toAbsolutePath)
        }
        catch {
          case ex: java.nio.file.FileSystemException => {
            logger.warn("cannot create link {} -> {}, copying data", linkp: Any, c.zipPath)
            java.nio.file.Files.copy(c.zipPath, linkp, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
          }
        }
      }
    } else {
      logger.debug("{} is the same as {}, no copy/link required", linkp: Any, c.zipPath.toAbsolutePath)
    }

    var result = true
    // if runEvaluation is true, run the Evaluation and store the results with the link
    if(runEvaluation.isDefined && runEvaluation.get) {
      val evalSuccess = evaluateCore(c, t, optimization = optimization, synthOptions = synthOptions)
      result &= evalSuccess
    }

    // write core.json
    logger.debug("writing core description: {}", p.toString)
    Core.to(c.copy(descPath = p, _zipPath = Paths.get("%s.zip".format(vlnv.name))), p)
    result
  }

  /**
    * Searches for an existing synthesis report, otherwise performs out-of-context synthesis and
    * place-and-route to produce area and Fmax estimates and the netlist.
    *
    * @param c            Core description.
    * @param t            Target Architecture + Platform combination.
    * @param optimization Positive integer optimization level.
    * @param synthOptions Optional arguments for synth_design.
    * @param cfg          Implicit [[Configuration]].
    **/
  def evaluateCore(c: Core, t: Target, optimization: Int, synthOptions: Option[String] = None)
                          (implicit cfg: Configuration): Boolean = {
    logger.trace("looking for SynthesisReport ...")
    val period = 1.0
    val report = cfg.outputDir(c, t).resolve("ipcore").resolve("%s_export.xml".format(c.name))
    FileAssetManager.reports.synthReport(c.name, t) map { hls_report =>
      logger.trace("found existing synthesis report: " + hls_report)
      if (!report.equals(hls_report.file)) { // make link if not same
        java.nio.file.Files.createSymbolicLink(report, hls_report.file.toAbsolutePath)
      }
      true
    } getOrElse {
      logger.info("SynthesisReport for {} not found, starting evaluation ...", c.name)
      EvaluateIP(c.zipPath, period, t.pd.part, report, optimization, synthOptions)
    }
  }
}
