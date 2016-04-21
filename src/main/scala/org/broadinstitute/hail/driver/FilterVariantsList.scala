package org.broadinstitute.hail.driver

import org.broadinstitute.hail.Utils._
import org.broadinstitute.hail.annotations._
import org.broadinstitute.hail.methods._
import org.broadinstitute.hail.variant._
import org.kohsuke.args4j.{Option => Args4jOption}


object FilterVariantsList extends Command {

  class Options extends BaseOptions {
    @Args4jOption(required = false, name = "-i", aliases = Array("--input"),
      usage = "Path to variant list file")
    var input: String = _

    @Args4jOption(required = false, name = "--keep", usage = "Keep variants matching condition")
    var keep: Boolean = false

    @Args4jOption(required = false, name = "--remove", usage = "Remove variants matching condition")
    var remove: Boolean = false
  }

  def newOptions = new Options

  def name = "filtervariants list"

  def description = "Filter variants in current dataset with a variant list"

  override def supportsMultiallelic = true

  def run(state: State, options: Options): State = {
    val vds = state.vds

    if ((options.keep && options.remove)
      || (!options.keep && !options.remove))
      fatal("one `--keep' or `--remove' required, but not both")

    val keep = options.keep

    val variants = readLines(options.input, state.hadoopConf)(_.map(_.transform { line =>
      val fields = line.value.split(":")
      if (fields.length != 4)
        fatal("invalid variant")
      val ref = fields(2)
      Variant(fields(0),
        fields(1).toInt,
        ref,
        fields(3).split(",").map(alt => AltAllele(ref, alt)))
    }).toSet)

    val variantsBc = state.sc.broadcast(variants)

    val p = (v: Variant, _: Annotation) => Filter.keepThis(variantsBc.value.contains(v), keep)

    state.copy(vds = vds.filterVariants(p))
  }
}
