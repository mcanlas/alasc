package net.alasc.math

import scala.collection.SortedSet

import org.scalatest.{FunSuite, Matchers}

import spire.syntax.partialAction._

import net.alasc.syntax.all._
import net.alasc.std.seq._
import bsgs._

class PartitionSuite extends FunSuite with Matchers {
  test("Domain.Partition bug") {
    val t1 = Domain(8).Partition.fromSortedBlocks(Seq(SortedSet(0,5,6,2,4),SortedSet(1,3,7)))
    val t2 = Domain.Partition(Set(0,5,6,2,4),Set(1,3,7))
    t1.toString
    t2.toString
  }
}
