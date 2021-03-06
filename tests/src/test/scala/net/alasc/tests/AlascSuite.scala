package net.alasc.tests

import spire.algebra.Eq
import spire.syntax.EqOps

import org.scalacheck.Shrink
import org.scalatest.exceptions.DiscardedEvaluationException
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FunSuite, Matchers}
import org.typelevel.discipline.scalatest.Discipline

import net.alasc.finite.Grp
import net.alasc.laws.NestedDiscipline

/**
  * An opinionated stack of traits to improve consistency and reduce
  * boilerplate in Alasc tests (inspired by Cats).
  */
trait AlascSuite extends FunSuite with Matchers
  with PropertyChecks
  with Discipline with NestedDiscipline
  with StrictAlascEquality
  with spire.syntax.AllSyntax
  with spire.std.ArrayInstances
  with spire.std.IntInstances
  with spire.std.LongInstances
  with spire.std.StringInstances
  with spire.std.ProductInstances
  with spire.std.SeqInstances1
  with net.alasc.syntax.AllSyntax
  with net.alasc.std.AnyInstances
  with spire.syntax.GroupoidSyntax {

  // disable Eq syntax (by making `eqOps` not implicit), since it collides
  // with scalactic's equality
  override def eqOps[A:Eq](a:A): EqOps[A] = new EqOps[A](a)

  def discardEvaluation(): Nothing = throw new DiscardedEvaluationException

  def noShrink[T] = Shrink[T](_ => Stream.empty)

  implicit def noShrinkGrp[G]: Shrink[Grp[G]] = noShrink[Grp[G]]

}
