package net.alasc.bsgs

import scala.annotation.tailrec
import scala.reflect.ClassTag
import scala.util.Random

import spire.algebra.{Eq, Group}
import spire.math.SafeLong
import spire.syntax.cfor._
import spire.syntax.group._
import spire.util.Opt

import net.alasc.algebra._

/** Generic element to describe BSGS data. */
sealed trait Elem[G]

sealed trait StartOrNode[G] extends Elem[G] { elem =>

  implicit def action: FaithfulPermutationAction[G]

  /** Next element in this BSGS chain. */
  def next: Chain[G]

}

sealed trait MutableStartOrNode[P] extends StartOrNode[P] { elem =>

  protected[bsgs] def next_= (value: Chain[P]): Unit

}

class Start[P](var next: Chain[P])(implicit val action: FaithfulPermutationAction[P]) extends MutableStartOrNode[P] {
  /** Pretty prints the builder, while doing basic chain consistency checks. */
  override def toString = {
    import scala.collection.mutable.StringBuilder
    var sb = new StringBuilder
    sb ++= "()"
    @tailrec def rec(chain: Chain[P]): Unit = chain match {
      case IsMutableNode(mn) =>
        sb ++= s" <-> ${mn.beta}(${mn.orbitSize})"
        rec(mn.next)
      case node: Node[P] =>
        sb ++= s" -> ${node.beta}(${node.orbitSize})"
        rec(node.next)
      case _: Term[P] =>
        sb ++= " -> ()"
    }
    rec(next)
    sb.mkString
  }
}

/** Base class for elements in a BSGS chain, i.e. nodes or terminal elements, implementing
  * the chain as a single-linked list, with a double-linked list for mutable elements.
  */
sealed trait Chain[P] extends Elem[P] {
  chain =>

  override def toString = nodesIterator.map { node => s"${node.beta}(${node.orbitSize})" }.mkString(" -> ")

  /** Tests whether this element terminates the BSGS chain. */
  def isTerminal: Boolean

  /** Tests whether this node is immutable. */
  def isImmutable: Boolean

  /** Tests whether this node is mutable. */
  def isMutable: Boolean

  /** Tests whether this chain is trivial, i.e. describes the trivial group. */
  def isTrivial: Boolean = ChainRec.isTrivial(chain)

  /** Iterator through the nodes of this chain, including the current one if applicable. */
  def nodesIterator: Iterator[Node[P]] = new Iterator[Node[P]] {
    private var cursor: Chain[P] = chain
    def hasNext = !cursor.isTerminal
    def next = cursor match {
      case node: Node[P] =>
        cursor = node.next
        node
      case _ => Iterator.empty.next
    }
  }

  /** Maps the function `f` is this chain element is a node, or returns the default value. */
  def mapOrElse[A](f: Node[P] => A, default: => A): A = chain match {
    case node: Node[P] => f(node)
    case _ => default
  }

  /** Tests whether if `k` is fixed by the group described by this chain. */
  def isFixed(k: Int): Boolean = ChainRec.isFixed(chain, k)

  /** Returns the strong generating set for the BSGS chain starting from this node.
    * 
    * @note The strong generating set is stored piece by piece by having each
    *       node storing explicitly only the generators appearing at its level.
    */
  def strongGeneratingSet: Iterable[P] = new StrongGeneratingSetIterable[P](chain)

  def elementsIterator(implicit group: Group[P]): Iterator[P]

  def order: SafeLong = ChainRec.order(chain, SafeLong(1))

  def randomElement(rand: Random)(implicit group: Group[P]): P

  def length: Int = ChainRec.length(chain)

  def base: Seq[Int] = ChainRec.base(chain)

  /** Tests whether the base of this chain is in the lexicographic order
    * (i.e. successive elements are increasing). */
  def hasLexicographicBase: Boolean = ChainRec.hasLexicographicBase(chain)

  def baseEquals(baseToCheck: Seq[Int]) = ChainRec.baseEquals(chain, baseToCheck.iterator)

  def basicSift(p: P)(implicit group: Group[P], equ: Eq[P]): (Seq[Int], P) = ChainRec.basicSift(chain, p)

  def siftOther[Q:Permutation](q: Q)(implicit group: Group[P], equ: Eq[P]): Opt[P] = chain match {
    case node: Node[P] =>
      implicit def action = node.action
      ChainRec.siftOther(chain, group.id, q)
    case _: Term[P] => if (q.isId) Opt(group.id) else Opt.empty
  }

  def sifts(p: P)(implicit group: Group[P], equ: Eq[P]): Boolean = ChainRec.sifts(chain, p)

  /** If the current element is a node, returns the next stabilizer group in chain and the current node
    * viewed as a transversal. If the current element is a terminal, creates and returns an empty transversal with
    * base point `beta`.
    */
  def detach(beta: => Int)(implicit group: Group[P]): (Chain[P], Transversal[P]) = chain match {
    case node: Node[P] => (node.next, node)
    case term: Term[P] => (term, Transversal.empty(beta))
  }
}

object Chain {

  implicit def ChainCheck[P:ClassTag:Eq:Group]: Check[Chain[P]] = new ChainCheck[P]

}

/** Node in a BSGS chain.
  * 
  * A `Node` can be in three different states:
  * 
  * - stand-alone if both `next` and `prev` are null and/or unavailable,
  * - in a chain if `next` points to the next node in the chain,
  * - immutable if `next` points to the next node in the chain but `prev` is null and/or unavailable,
  * - mutable if `next` and `prev` point to the next and previous nodes in the chain. Note that if
  *   a node is mutable, then the previous node is mutable as well.
  * 
  * The set of strong generators is represented by storing with each node only the strong generators that stabilize
  * the previous base points, but not the current base point.
  */
trait Node[P] extends Chain[P] with StartOrNode[P] with Transversal[P] {
  node =>
  /** Permutation action for the type `P`. */
  implicit def action: FaithfulPermutationAction[P]

  def isTerminal = false
  def isStandalone: Boolean

  def next: Chain[P]
  def beta: Int

  /** If the base is beta(1) -> ... -> beta(m-1) -> beta(m) current base -> tail.beta,
    * ownGenerator(0 .. numOwnGenerators - 1) contains all the strong generators g that
    *  have beta(i) <|+| g = beta(i) for i < m, and beta(m) <|+| g =!= beta(m).
    */
  def ownGenerator(i: Int): P
  /** Inverses of own generators, order of inverses corresponding to `generatorsArray`. */
  def ownGeneratorInv(i: Int): P
  def nOwnGenerators: Int

  def ownGenerators: IndexedSeq[P] = new IndexedSeq[P] {
    def apply(i: Int) = ownGenerator(i)
    def length = nOwnGenerators
  }

  def elementsIterator(implicit group: Group[P]): Iterator[P] = for {
    rest <- next.elementsIterator
    b <- orbit.iterator
  } yield rest |+| u(b) // TODO optimize

  def randomElement(rand: Random)(implicit group: Group[P]): P = ChainRec.random(node.next, rand, node.randomU(rand))

  def randomOrbit(rand: Random): Int

  def foreachU(f: P => Unit): Unit
  def u(b: Int): P
  def uInv(b: Int): P
  def randomU(rand: Random): P
}

object Node {

  def trivial[P](beta: Int, next: Chain[P] = Term[P])(implicit action: FaithfulPermutationAction[P], group: Group[P], classTag: ClassTag[P]): Node[P] =
    new TrivialNode[P](beta, group.id, next)

  /** Extractor for `Node` from `Elem`. */
  def unapply[P](elem: Elem[P]): Option[Node[P]] = elem match {
    case node: Node[P] => Some(node)
    case _ => None
  }

}

/** Represents the end of a BSGS chain, or, when viewed as a group, the trivial group (). */
class Term[P] extends Chain[P] {

  def isTerminal = true
  def isImmutable = true
  def isMutable = false

  def elementsIterator(implicit group: Group[P]): Iterator[P] = Iterator(group.id)

  def randomElement(rand: Random)(implicit group: Group[P]): P = group.id

}

object Term {

  val instance = new Term[Nothing]
  def apply[P] = instance.asInstanceOf[Term[P]]

}

trait MutableNode[P] extends Node[P] with MutableStartOrNode[P] {

  override def toString = if (isStandalone) s"Node ($beta) orbit $orbit" else super.toString

  def isImmutable = prev eq null
  def isStandalone = (prev eq null) && (next eq null)
  def isMutable = (prev ne null)

  def prev: MutableStartOrNode[P]
  protected[bsgs] def prev_= (value: MutableStartOrNode[P]): Unit

  /** Makes the current node immutable. */
  protected[bsgs] def makeImmutable(): Unit = {
    next.mapOrElse(node => assert(node.isImmutable), ())
    prev = null
  }

  protected[bsgs] def removeRedundantGenerators(): Unit

  /** Adds `newGenerators` (given with their inverses) to this node `ownGenerators`,
    * without changing other nodes or updating any transversals. */
  protected[bsgs] def addToOwnGenerators(newGens: Iterable[P], newGensInv: Iterable[P])(implicit group: Group[P], classTag: ClassTag[P]): Unit
  protected[bsgs] def addToOwnGenerators(newGen: P, newGenInv: P)(implicit group: Group[P], classTag: ClassTag[P]): Unit

  /** Updates this node transversal by the addition of `newGens`,
    * provided with their inverses.
    * 
    * @note `newGens` must be already part of the strong generating set, i.e.
    *       have been added to this node or a children node `ownGenerators`
    *       by using addToOwnGenerators.
    */
  protected[bsgs] def updateTransversal(newGens: Iterable[P], newGensInv: Iterable[P])(implicit group: Group[P], classTag: ClassTag[P]): Unit
  protected[bsgs] def updateTransversal(newGen: P, newGenInv: P)(implicit group: Group[P], classTag: ClassTag[P]): Unit

  /** Conjugates the current node by the group element `g`, provided with its inverse
    * `gInv` to avoid multiple inverse element computations. 
    */
  protected[bsgs] def conjugate(g: P, gInv: P)(implicit group: Group[P], classTag: ClassTag[P]): Unit

}