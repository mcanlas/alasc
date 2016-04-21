package net.alasc.bsgs

import scala.annotation.tailrec
import scala.collection.mutable
import scala.reflect.ClassTag

import spire.algebra.{Eq, Group}
import spire.syntax.action._
import spire.syntax.group._

abstract class BaseChange {

  /** Change the base in `mutableChain` using the base guide provided.
    * The action is not changed.
    * 
    * Redundant base points are *not* inserted at the end of the chain.
    * 
    * @param mutableChain Mutable chain on which to perform the base change.
    * @param baseGuide    Guide for the new base.
    */
  def changeBase[G:ClassTag:Eq:Group](mutableChain: MutableChain[G], baseGuide: BaseGuide): Unit

  def changeBase[G:ClassTag:Eq:Group](mutableChain: MutableChain[G], base: Seq[Int]): Unit =
  changeBase(mutableChain, BaseGuideSeq(base))

}

final class BaseChangeFromScratch(implicit val schreierSims: SchreierSims) extends BaseChange {

  def changeBase[G:ClassTag:Eq:Group](mutableChain: MutableChain[G], baseGuide: BaseGuide): Unit = {
    implicit def action = mutableChain.start.action
    val oldChain = mutableChain.start.next
    val tempChain = schreierSims.completeChainFromGeneratorsRandomElementsAndOrder(
      oldChain.strongGeneratingSet, oldChain.randomElement(_),
      oldChain.order, baseGuide.fullBase)
    mutableChain.replaceChain(mutableChain.start, tempChain.start)
  }

}

final class BaseChangeSwap(implicit val baseSwap: BaseSwap) extends BaseChange {

  def changeBase[G:ClassTag:Eq:Group](mutableChain: MutableChain[G], guide: BaseGuide): Unit = {
    implicit def action = mutableChain.start.action
    val iter = guide.iterator
    @tailrec def rec(prev: StartOrNode[G],
      lastMutableStartOrNode: MutableStartOrNode[G]): Unit = {
      if (prev.next.isTrivial || !iter.hasNext)
        mutableChain.cutRedundantAfter(prev)
      else prev.next match {
        case IsMutableNode(mutableNode) =>
          val mutablePrev = mutableNode.prev
          val beta = iter.next(mutableNode.beta, Set(mutableNode.beta), mutableNode.isFixed(_))
          if (mutableNode.beta == beta)
            rec(mutableNode, mutablePrev)
          else {
            val newNode = mutableChain.changeBasePointAfter(mutablePrev, beta)
            rec(newNode, mutablePrev)
          }
        case node: Node[G] =>
          val beta = iter.next(node.beta, Set(node.beta), node.isFixed(_))
          if (node.beta == beta)
            rec(node, lastMutableStartOrNode)
          else {
            val mutablePrev = mutableChain.mutableStartOrNode(prev, lastMutableStartOrNode)
            val newNode = mutableChain.changeBasePointAfter(mutablePrev, beta)
            rec(newNode, mutablePrev)
          }
        case term: Term[G] => // finished
      }
    }
    rec(mutableChain.start, mutableChain.start)
  }

}

final class BaseChangeSwapConjugation(implicit val baseSwap: BaseSwap) extends BaseChange {

  def changeBaseConjugation[G:ClassTag:Eq:Group](mutableChain: MutableChain[G], guide: BaseGuide): (G, G) = {
    implicit def action = mutableChain.start.action
    val iter = guide.iterator
    @tailrec def rec(prev: StartOrNode[G], lastMutableStartOrNode: MutableStartOrNode[G], conj: G, conjInv: G): (G, G) = {
      if (prev.next.isTrivial || !iter.hasNext) {
        mutableChain.cutRedundantAfter(prev)
        (conj, conjInv)
      } else prev.next match {
        case IsMutableNode(mutableNode) =>
          val mutablePrev = mutableNode.prev
          val easyPoints = mutable.BitSet.empty
          mutableNode.foreachOrbit { k => easyPoints += (k <|+| conj) }
          val beta = iter.next(mutableNode.beta <|+| conj, easyPoints, k => mutableNode.isFixed(k <|+| conjInv))
          val alpha = beta <|+| conjInv
          if (mutableNode.beta == alpha)
            rec(mutableNode, mutablePrev, conj, conjInv)
          else if (mutableNode.inOrbit(alpha)) {
            val nextConj = mutableNode.u(alpha) |+| conj
            val nextConjInv = conjInv |+| mutableNode.uInv(alpha)
            rec(mutableNode, mutablePrev, nextConj, nextConjInv)
          } else {
            val newNode = mutableChain.changeBasePointAfter(mutablePrev, alpha)
            rec(newNode, mutablePrev, conj, conjInv)
          }
        case node: Node[G] =>
          val easyPoints = mutable.BitSet.empty
          node.foreachOrbit { k => easyPoints += (k <|+| conj) }
          val beta = iter.next(node.beta <|+| conj, easyPoints, k => node.isFixed(k <|+| conjInv))
          val alpha = beta <|+| conjInv
          if (node.beta == alpha)
            rec(node, lastMutableStartOrNode, conj, conjInv)
          else if (node.inOrbit(alpha)) {
            val nextConj = node.u(alpha) |+| conj
            val nextConjInv = conjInv |+| node.uInv(alpha)
            rec(node, lastMutableStartOrNode, nextConj, nextConjInv)
          } else {
            val mutablePrev = mutableChain.mutableStartOrNode(prev, lastMutableStartOrNode)
            val newNode = mutableChain.changeBasePointAfter(mutablePrev, alpha)
            rec(newNode, mutablePrev, conj, conjInv)
          }
        case term: Term[G] => (conj, conjInv)
      }
    }
    rec(mutableChain.start, mutableChain.start, Group[G].id, Group[G].id)
  }

  def changeBase[G:ClassTag:Eq:Group](mutableChain: MutableChain[G], guide: BaseGuide): Unit = {
    val (g, gInv) = changeBaseConjugation(mutableChain, guide)
    mutableChain.conjugate(g, gInv)
  }

}

object BaseChange {

  def fromScratch(implicit schreierSims: SchreierSims): BaseChange = new BaseChangeFromScratch

  def swap(implicit baseSwap: BaseSwap): BaseChange = new BaseChangeSwap

  def swapConjugation(implicit baseSwap: BaseSwap): BaseChange = new BaseChangeSwapConjugation

}