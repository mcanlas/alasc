/*
# Main package `alasc` for computational group theory
*/
package net

import scala.language.implicitConversions

package object alasc {
  object indexSyntax extends IndexSyntax
  object lexicoSyntax extends LexicoSyntax
  object allSyntax extends AllSyntax

  object finiteInstances extends FiniteInstances
  object indexInstances extends IndexInstances
  object permutingInstances extends PermutingInstances
  object allInstances extends AllInstances

  object groupImplicits extends GroupImplicits
  object finiteImplicits extends FiniteImplicits
  object allImplicits extends AllImplicits

  object all extends AllSyntax with AllInstances with AllImplicits

  /* Cartesian product of traversable. */
  def combine[A](xs: Traversable[Traversable[A]]): Seq[Seq[A]] =
    xs.foldLeft(Seq(Seq.empty[A])){
      (x, y) => for (a <- x.view; b <- y) yield a :+ b }

  def combineList[A](xs: Traversable[Traversable[A]]): Seq[List[A]] =
    xs.foldLeft(Seq(List.empty[A])){
      (x, y) => for (a <- x.view; b <- y) yield a :+ b }

  def bind2sub(N: Seq[BigInt], i: BigInt): Seq[BigInt] =
    N.scanLeft((BigInt(0), i)) { case ((rem, quot), n) => (quot % n, quot / n) }.map(_._1).tail

  def bsub2ind(N: Seq[BigInt], I: Seq[BigInt]): BigInt =
    (N zip I).foldLeft((BigInt(0), BigInt(1))) { case ((tot, mul), (n, i)) => (tot + mul * i, mul * n) }._1

  def ind2sub(N: Seq[Int], i: Int): Seq[Int] =
    N.scanLeft((0, i)) { case ((rem, quot), n) => (quot % n, quot / n) }.map(_._1).tail

  def sub2ind(N: Seq[Int], I: Seq[Int]): Int =
    (N zip I).foldLeft((0, 1)) { case ((tot, mul), (n, i)) => (tot + mul * i, mul * n) }._1

  type Predicate[F <: Finite[F]] = (F => Boolean)

  implicit object DomOrdering extends Ordering[Dom] {
    def compare(a: Dom, b: Dom) = a.zeroBased.compare(b.zeroBased)
  }

  implicit class DomIndexing[T](seq: Seq[T]) {
    def apply(index: Dom) = seq(index._0)
  }

  import scala.annotation.elidable
  import scala.annotation.elidable._

  @elidable(ASSERTION)
  def require_(requirement: Boolean) {
    if (!requirement)
      throw new java.lang.AssertionError("assumption failed")
  }
  @elidable(ASSERTION)	
  @inline final def require_(requirement: Boolean, message: => Any) {
    if (!requirement)
      throw new IllegalArgumentException("requirement failed: "+ message)
  }
  implicit def permActionBuilder[P <: Permuting[P]](p: P) = TrivialPRepr(p * p.inverse)
}
