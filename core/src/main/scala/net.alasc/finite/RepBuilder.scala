package net.alasc.finite

import scala.reflect.ClassTag

import spire.algebra.{Group, PartialOrder}
import spire.algebra.lattice.{BoundedJoinSemilattice, Lattice}
import spire.util.Opt

trait BuiltRep[G] extends Rep[G] {

  type B <: RepBuilder[G] with Singleton

  implicit val builder: B

}

object BuiltRep {

  type In[B0 <: RepBuilder[G] with Singleton, G] = BuiltRep[G] { type B = B0 }

}

/** Describes a family of permutation representations of a group G. 
  * Depending on the particular subgroup H of G, the representation 
  * can differ: thus, one obtains a representation valid for a set of generators
  * by calling `build` on those generators.
  *
  * Given representations a and b, one defines a joined representation c such that if 
  * A and B are subgroups of G for which a and b are valid, the representation c is
  * valid for the union of A and B. This is described using a join-semilattice.
  * 
  * Methods are generic in their arguments (i.e. they accept a `BuiltRep[G]`), 
  * and return a representation specific to the current class.
  */ 
abstract class RepBuilder[G] {

  type R <: BuiltRep.In[this.type, G]

  def classTagR: ClassTag[R]

  def build(generators: Iterable[G]): R

  object CastIn {

    def unapply(r: Rep[G]): Opt[R] = {
      val CT = classTagR
      r match {
        case CT(typed) if typed.builder == this => Opt(typed)
        case _ => Opt.empty[R]
      }
    }

  }

  def get(r: Rep[G], generators: Iterable[G]): R = r match {
    case CastIn(cast) => cast
    case _ => build(generators)
  }

  implicit def lattice: Lattice[R] with BoundedJoinSemilattice[R]
  implicit def partialOrder: PartialOrder[R]

  /** Returns a representation that is compatible with the union of subgroups
    * generated by `lhsGenerators` and `rhsGenerators`. 
    * Representations `lhs` and `rhs` are provided, and will be used in the join
    *  provided they are of the type of the representations described by this object.
    */
  def genJoin(lhs: Rep[G], lhsGenerators: Iterable[G],
    rhs: Rep[G], rhsGenerators: Iterable[G]): R = (lhs, rhs) match {
    case (CastIn(lhs1), CastIn(rhs1)) => lattice.join(lhs1, rhs1)
    case (CastIn(lhs1), _) if rhsGenerators.forall(lhs1.represents(_)) => lhs1
    case (_, CastIn(rhs1)) if lhsGenerators.forall(rhs1.represents(_)) => rhs1
    case _ => lattice.join(get(lhs, lhsGenerators), get(rhs, rhsGenerators))
  }

}
