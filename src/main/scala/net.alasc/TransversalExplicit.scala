package net.alasc

import scala.annotation.tailrec
import scala.util.Random
import scala.collection.immutable.IntMap

/*
## Implementation of `Transversal` using an explicit representation of group elements
*/

case class TransversalExplicit[F <: Finite[F]](beta: Dom, action: PRepr[F], intMap: IntMap[TEntry[F]]) extends Transversal[F] with TransversalImpl[F] {
  import Dom.ZeroBased._

  def builder = TransversalExplicit

  def identity = apply(beta).u

  override def size = intMap.size
  def isDefinedAt(b: Dom) = intMap.isDefinedAt(b)
  def apply(b: Dom) = intMap.apply(b)

  def iterator = intMap.iterator.map { case (k, v) => Dom._0(k) -> v }
  override def keysIterator = intMap.keysIterator.map(Dom._0(_))
  override def valuesIterator = intMap.valuesIterator

  def mapValues[G <: Finite[G]](f: F => G, gPRepr: PRepr[G]): TransversalExplicit[G] =
    TransversalExplicit(beta, gPRepr, IntMap.empty[TEntry[G]] ++ intMap.mapValues( wi => TEntry(f(wi.u), f(wi.uinv))))

  def updated(newGens: Iterable[F], allGens: Iterable[F]): TransversalExplicit[F] = {
    if (newGens.isEmpty)
      return this
    val newGensInv = newGens.map( g => (g, g.inverse) )
    var candidates: IntMap[TEntry[F]] = IntMap.empty[TEntry[F]] ++ (
      for ((s, sinv) <- newGensInv; b <- intMap.keysIterator; img = action(s, b) if !intMap.contains(img) )
      yield (img._0, TEntry(apply(b).u*s, sinv*apply(b).uinv)) )
    var newMap: IntMap[TEntry[F]] = intMap ++ candidates.iterator
    if (newMap.isEmpty)
      return this
    val allGensInv = allGens.map( g => (g, g.inverse) )
    def checkCandidate(candidate: (Int, TEntry[F])): Boolean = {
      val (b, TEntry(u, uinv)) = candidate
      for ((s, sinv) <- allGensInv; img = action(s, b) if !newMap.contains(img)) {
        val el = (img._0, TEntry(u*s, sinv*uinv))
        candidates = candidates + el
        newMap = newMap + el
        return true
      }
      return false
    }
    while (!candidates.isEmpty) {
      val h = candidates.head
      while(checkCandidate(h)) { }
      candidates = candidates - h._1
    }
    TransversalExplicit(beta, action, newMap)
  }

  // implementation of AbstractTrans
  def conjugatedBy(f: F, finv: F): TransversalExplicit[F] = {
    val newMap: IntMap[TEntry[F]] = intMap.map { case (b, TEntry(v,vinv)) => (action(f, b)._0, TEntry(finv*v*f, finv*vinv*f)) } // TODO: bring to class itself
    TransversalExplicit(action(f, beta), action, newMap)
  }
}

object TransversalExplicit extends TransversalBuilder {
  def empty[F <: Finite[F]](beta: Dom, action: PRepr[F]) = TransversalExplicit(beta, action, IntMap(beta._0 -> TEntry(action.identity, action.identity)))
}
