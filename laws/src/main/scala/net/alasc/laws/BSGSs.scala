package net.alasc.laws

import spire.algebra.{Group, Order}
import spire.util.Opt

import org.scalacheck.Gen
import org.scalatest.matchers.{MatchResult, Matcher}

import spire.syntax.partialAction._

import net.alasc.algebra.{FaithfulPermutationAction, Permutation, PermutationAction}
import net.alasc.bsgs.{Chain, Node, Term}
import net.alasc.finite.Grp
import net.alasc.std.seq._
import spire.syntax.order._
import net.alasc.perms.Perm

object BSGSs {

  def genExistingBasePoint(chain: Chain[_, _]): Gen[Opt[Int]] = {
    val base = chain.base
    if (base.isEmpty) Gen.const(Opt.empty[Int]) else Gen.oneOf(base).map(k => Opt(k))
  }

  def genSwapIndex(chain: Chain[_, _]): Gen[Opt[Int]] =
    if (chain.length < 2)
      Gen.const(Opt.empty[Int])
    else
      Gen.choose(0, chain.length - 2).map(i => Opt(i))

  def genSwappedSeq[A](seq: Seq[A]): Gen[Seq[A]] =
    Permutations.forSize[Perm](seq.size).map( perm => (seq <|+|? perm).get )

  def genNewBase[G, F <: FaithfulPermutationAction[G] with Singleton](chain: Chain[G, F]): Gen[Seq[Int]] = chain match {
    case _: Term[G, F] => Gen.const(Seq.empty[Int])
    case node: Node[G, F] =>
      import node.action
      val n = PermutationAction.largestMovedPoint(node.strongGeneratingSet).getOrElse(0) + 1
      genSwappedSeq(0 until n).flatMap(full => Gen.choose(0, n - 1).map(m => full.take(m)))
  }

  def genRandomElement[G:Group](chain: Chain[G, _]): Gen[G] =
    Gen.parameterized( p => chain.randomElement(p.rng) )

  def beWeaklyIncreasing[A:Order] = new Matcher[Seq[A]] {

    def apply(left: Seq[A]) = {
      val pairs = left.iterator zip left.iterator.drop(1)
      MatchResult(
        pairs.forall { case (i, j) => i <= j },
        s"""Sequence $left was not (weakly) increasing""",
        s"""Sequence $left was (weakly) increasing"""
      )
    }

  }

}