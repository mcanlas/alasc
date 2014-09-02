package net.alasc.algebra

import scala.{ specialized => spec }
import scala.annotation.tailrec
import scala.collection.immutable.BitSet
import scala.collection.mutable.{ BitSet => MutableBitSet }
import spire.algebra._
import spire.syntax.groupAction._

import net.alasc.math.Cycle
import net.alasc.util._

trait PermutationAction[G] extends GroupAction[Int, G] with Signed[G] {
  /** Returns a bit set of all integers k that are changed by the action of the permutation,
    * i.e. `S = { k | k <|+| g != k }`.
    */
  def support(g: G): BitSet
  /** Returns the maximal element in the support of ` g`, or NNNone if the support is empty. */ 
  def supportMax(g: G): NNOption
  /** Returns the minimal element in the support of `g`, or NNNone if the support is empty. */
  def supportMin(g: G): NNOption
  /** Returns an arbitrary element in the support of `g` or NNNone if support empty. */
  def supportAny(g: G): NNOption = supportMax(g)
  /** Returns an upper bound on the maximal element in the support of any element of `G`. */
  def supportMaxElement: Int

  def images(g: G, n: Int): IndexedSeq[Int] = new IndexedSeq[Int] {
    require(supportMax(g).getOrElse(-1) < n)
    def length = n
    def apply(idx: Int) = actr(idx, g)
  }

  def signum(g: G) = {
    // optimized for dense permutation on non-huge domains
    val toCheck = MutableBitSet.empty ++= support(g)
    var parity = 0
    while (!toCheck.isEmpty) {
      val start = toCheck.head
      @tailrec def rec(k: Int): Unit = {
        toCheck -= k
        parity ^= 1
        val next = actr(k, g)
        if (next != start)
          rec(next)
      }
      rec(start)
    }
    if (parity == 0) 1 else -1
  }

  def to[P](g: G)(implicit evP: Permutation[P]): P =
    evP.fromSupportAndImageFun(support(g), k => actr(k, g))
}

trait FaithfulPermutationAction[G] extends PermutationAction[G]