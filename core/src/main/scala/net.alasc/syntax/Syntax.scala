package net.alasc.syntax

import spire.algebra.Monoid
import net.alasc.algebra._
import scala.language.implicitConversions

trait CheckSyntax {
  implicit def checkSyntax[A: Check](a: A) = new CheckOps(a)
}

trait MonoidSyntax {
  implicit def monoidSyntax[A: Monoid](as: TraversableOnce[A]) = new MonoidOps(as)
}

trait SequenceSyntax {
  implicit def sequenceSyntax[T, A](t: T)(implicit ev: Sequence[T, A]) = new SequenceOps(t)
}

trait FiniteGroupSyntax {
  implicit def finiteGroupSyntax[A: FiniteGroup](a: A) = new FiniteGroupOps(a)
}

trait PermutationActionSyntax extends FiniteGroupSyntax {
  implicit def permutationActionSyntax[A: PermutationAction](a: A) = new PermutationActionOps(a)
}

trait ShiftablePermutationSyntax extends FiniteGroupSyntax {
  implicit def shiftablePermutationSyntax[A: ShiftablePermutation](a: A) = new ShiftablePermutationOps(a)
}

trait SubgroupSyntax {
  implicit def subgroupSyntax[S, G](s: S)(implicit ev: Subgroup[S, G]) = new SubgroupOps(s)
}

trait PermutationSubgroupSyntax {
  implicit def permutationSubgroupSyntax[S, G](s: S)(implicit ev: Subgroup[S, G], action: FaithfulPermutationAction[G]) =
    new PermutationSubgroupOps[S, G](s)
}

trait GroupoidSyntax {
  implicit def groupoidOps[A <: AnyRef: Groupoid](a: A) = new GroupoidOps(a)
}

trait GroupoidActionSyntax {
  implicit def groupoidActionGroupOps[G <: AnyRef](g: G) = new GroupoidActionGroupOps(g)
  implicit def groupoidActionPointOps[P <: AnyRef](p: P) = new GroupoidActionPointOps(p)
}


trait AllSyntax
    extends CheckSyntax
    with MonoidSyntax
    with SequenceSyntax
    with FiniteGroupSyntax
    with PermutationActionSyntax
    with ShiftablePermutationSyntax
    with SubgroupSyntax
    with PermutationSubgroupSyntax
    with GroupoidSyntax
    with GroupoidActionSyntax
