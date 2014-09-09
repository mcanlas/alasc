package net.alasc
package math
package enum

import scala.collection.mutable

import spire.algebra.{Eq, GroupAction, Order}
import spire.syntax.group._
import spire.syntax.groupAction._

import net.alasc.algebra._
import net.alasc.syntax.sequence._
import net.alasc.util._

import bsgs._

trait RepresentativesHead[T, G] extends RepresentativesOrdered[T, G] with coll.HasHead[Representative[T, G]] {
//  lazy val lexGrpStart = grp.chain(representation, 
//  def head: LexRepresentative[T, G] = {
//
//  }
}
