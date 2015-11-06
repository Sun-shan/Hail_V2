package org.broadinstitute.hail.driver

import org.broadinstitute.hail.Utils._
import org.broadinstitute.hail.methods._
import org.broadinstitute.hail.variant._
import org.kohsuke.args4j.{Option => Args4jOption}

object FilterVariants extends Command {

  class Options extends BaseOptions {
    @Args4jOption(required = false, name = "--keep", usage = "Keep variants matching condition")
    var keep: Boolean = false

    @Args4jOption(required = false, name = "--remove", usage = "Remove variants matching condition")
    var remove: Boolean = false

    @Args4jOption(required = true, name = "-c", aliases = Array("--condition"),
      usage = "Filter condition: expression or .interval_list file")
    var condition: String = _
  }

  def newOptions = new Options

  def name = "filtervariants"

  def description = "Filter variants in current dataset"

  def run(state: State, options: Options): State = {
    val vds = state.vds

    if (!options.keep && !options.remove)
      fatal(name + ": one of `--keep' or `--remove' required")

    val cond = options.condition
    val p: (Variant) => Boolean = cond match {
      case f if f.endsWith(".interval_list") =>
        val ilist = IntervalList.read(options.condition)
        (v: Variant) => ilist.contains(v.contig, v.start)
      case c: String =>
        try {
          val cf = new ConditionPredicate[Variant]("v", c)
          cf.compile(true)
          cf.apply
        } catch {
          case e: scala.tools.reflect.ToolBoxError =>
            /* e.message looks like:
               reflective compilation has failed:

               ';' expected but '.' found. */
            fatal("parse error in condition: " + e.message.split("\n").last)
        }
    }

    val newVDS = vds.filterVariants(if (options.keep)
      p
    else
      (v: Variant) => !p(v))

    state.copy(vds = newVDS)
  }
}

