package net.alasc.blackbox

import scala.annotation.tailrec
import scala.util.Random

import spire.algebra.{Eq, Group}
import spire.syntax.group._

import net.alasc.finite._

class BBGrpBuilder[G](implicit
    val group: Group[G],
    val equ: Eq[G]
  ) extends GrpBuilder[G] {

  def trivial: BBGrp[G] = new BBGrp(Iterable.empty[G], Set(group.id))(this)

  def fromGenerators(generators: Iterable[G]): BBGrp[G] = {
    @tailrec def rec(elements: Set[G]): Set[G] = {
      val newElements = generators
        .flatMap(g1 => elements.map(g2 => g1 |+| g2))
        .filterNot(elements.contains(_))
      if (newElements.isEmpty) elements else rec(elements ++ newElements)
    }
    new BBGrp(generators, rec(generators.toSet))(this)
  }

  def fromGeneratorsAndOrder(generators: Iterable[G], order: BigInt): BBGrp[G] =
    fromGenerators(generators)

  def fromGeneratorsRandomElementsAndOrder(generators: Iterable[G], randomElement: Random => G, order: BigInt): BBGrp[G] =
    fromGenerators(generators)

}
