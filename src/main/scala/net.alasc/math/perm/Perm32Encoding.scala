package net.alasc.math
package perm

import net.alasc.algebra._
import scala.collection.immutable.BitSet
import spire.syntax.signed._
import spire.syntax.groupAction._

/** 5 bits per permutation image shift * 32 images = 160 bits.
  * 
  * - long1 stores image shifts for indices  0...11 in bits 0...59
  * - long2 stores image shifts for indices 12...23 in bits 0...59
  * - long3 stores image shifts for indices 24...31 in bits 0...39
  */
object Perm32Encoding {
  import LongBits._

  @inline def maskWidth = 5
  //                       FEDCBA9876543210
  @inline def longMask = 0x0FFFFFFFFFFFFFFFL
  @inline def numPerLong = 12
  @inline def leftBlank = 64 - numPerLong * maskWidth
  @inline def long1Start = numPerLong
  @inline def long2Start = numPerLong * 2
  @inline def mask = 0x1FL
  @inline def supportMaxElement = 31

  import java.lang.Long.{numberOfLeadingZeros, numberOfTrailingZeros}

  @inline def supportMin(long2: Long, long1: Long, long0: Long) =
    if (long0 != 0)
      numberOfTrailingZeros(long0) / maskWidth
    else if (long1 != 0)
      long1Start + numberOfTrailingZeros(long1) / maskWidth
    else if (long2 != 0)
      long2Start + numberOfTrailingZeros(long2) / maskWidth
    else
      -1

  @inline def supportMax(long2: Long, long1: Long, long0: Long) =
    if (long2 != 0)
      31 - (numberOfLeadingZeros(long2) - 24) / maskWidth
    else if (long1 != 0)
      long2Start - 1 - (numberOfLeadingZeros(long1) - leftBlank) / maskWidth
    else if (long0 != 0)
      long1Start - 1 - (numberOfLeadingZeros(long0) - leftBlank) / maskWidth
    else
      -1

  @inline def hash(long2: Long, long1: Long, long0: Long) = {
    import scala.util.hashing.MurmurHash3.{mix, finalizeHash}
    var h = PermHash.seed
    h = mix(h, long0.toInt)
    h = mix(h, (long0 >> 30).toInt)
    h = mix(h, long1.toInt)
    h = mix(h, (long1 >> 30).toInt)
    h = mix(h, long2.toInt)
    h = mix(h, (long2 >> 30).toInt)
    finalizeHash(h, 6)
  }

  @inline def inSupport(long2: Long, long1: Long, long0: Long, preimage: Int): Boolean =
    if (preimage < long1Start)
      ((long0 >>> (preimage * maskWidth)) & mask) != 0
    else if (preimage < long2Start)
      ((long1 >>> ((preimage - long1Start) * maskWidth)) & mask) != 0
    else if (preimage <= supportMaxElement)
      ((long2 >>> ((preimage - long2Start) * maskWidth)) & mask) != 0
    else
      false

  @inline def support(long2: Long, long1: Long, long0: Long): BitSet = {
    var bitset = 0L
    if (long2 != 0) {
      var k = 31
      while (k >= 24) {
        if (inSupport(long2, long1, long0, k))
          bitset |= 1L << k
        k -= 1
      }
    }
    if (long1 != 0) {
      var k = 23
      while (k >= 12) {
        if (inSupport(long2, long1, long0, k))
          bitset |= 1L << k
        k -= 1
      }
    }
    if (long0 != 0) {
      var k = 11
      while (k >= 0) {
        if (inSupport(long2, long1, long0, k))
          bitset |= 1L << k
        k -= 1
      }
    }
    BitSet.fromBitMask(Array(bitset))
  }

  @inline def decode(long2: Long, long1: Long, long0: Long, preimage: Int): Int =
    if (preimage < long1Start)
      ((preimage + (long0 >>> (preimage * maskWidth))) & mask).toInt
    else if (preimage < long2Start)
      ((preimage + (long1 >>> ((preimage - long1Start) * maskWidth))) & mask).toInt
    else
      ((preimage + (long2 >>> ((preimage - long2Start) * maskWidth))) & mask).toInt

  @inline def encode(perm: Perm32, preimage: Int, image: Int): Unit = {
    if (preimage < long1Start)
      perm.long0 |= ((image - preimage).toLong & mask) << (preimage * maskWidth)
    else if (preimage < long2Start)
      perm.long1 |= ((image - preimage).toLong & mask) << ((preimage - long1Start) * maskWidth)
    else
      perm.long2 |= ((image - preimage).toLong & mask) << ((preimage - long2Start) * maskWidth)
  }

  def toPerm16(long2: Long, long1: Long, long0: Long): Perm16 = {
    var k = Perm32Encoding.supportMax(long2, long1, long0)
    if (k > Perm16Encoding.supportMaxElement) sys.error("Cannot fit in Perm16.")
    var encoding = 0L
    var l1 = long1
    while (k >= 12) {
      encoding += (l1 & 0xF) << (k * 4)
      l1 = l1 >>> maskWidth
      k -= 1
    }
    var l0 = long0
    while (k >= 0) {
      encoding += (l0 & 0xF) << (k * 4)
      l0 = l0 >> maskWidth
    }
    new Perm16(encoding)
  }

  @inline def image(long2: Long, long1: Long, long0: Long, preimage: Int): Int =
    if (preimage > supportMaxElement) preimage else decode(long2, long1, long0, preimage)

  def invImage(long2: Long, long1: Long, long0: Long, i: Int): Int = {
    if ((long2 == 0 && long1 == 0 && long0 == 0) || i > supportMaxElement)
      return i
    var k = Perm32Encoding.supportMax(long2, long1, long0)
    if (i > k) return i
    val low = Perm32Encoding.supportMin(long2, long1, long0)
    if (i < low) return i
    while (k >= low) {
      if (decode(long2, long1, long0, k) == i)
        return k
      k -= 1
    }
    sys.error("Invalid permutation")
  }

  @inline def inverse(p: Perm32): Perm32 = {
    if (p.long2 == 0 && p.long1 == 0 && p.long0 == 0) return p
    val low = supportMin(p.long2, p.long1, p.long0)
    var k = supportMax(p.long2, p.long1, p.long0)
    val res = new Perm32
    while (k >= low) {
      encode(res, decode(p.long2, p.long1, p.long0, k), k)
      k -= 1
    }
    res
  }

  // long2 contains indices 12..23, and the indices 12..15 occupy the right-most 20 bits of long1
  @inline def isValidPerm16(long2: Long, long1: Long, long0: Long) = long2 == 0 && (long1 & leftFill(44)) == 0

  def fromImages(images: Seq[Int], supportMax: Int = 31): Perm32 = {
    assert(supportMax <= supportMaxElement)
    assert(supportMax > Perm16Encoding.supportMaxElement)
    var k = supportMax
    val res = new Perm32
    while (k >= 0) {
      val i = images(k)
      encode(res, k, i)
      k -= 1
    }
    res
  }

  def fromSupportAndImages(support: BitSet, image: Int => Int): Perm32 = {
    val res = new Perm32
    var supportMax = -1
    support.foreach { k =>
      val i = image(k)
      if (i != k && k > supportMax)
        supportMax = k
      assert(k <= supportMaxElement && i <= supportMaxElement)
      encode(res, k, i)
    }
    assert(supportMax > Perm16Encoding.supportMaxElement) 
    res
  }

  def op3232(lhs: Perm32, rhs: Perm32): Perm = {
    val low = Perm32Encoding.supportMin(lhs.long2 | rhs.long2, lhs.long1 | rhs.long1, lhs.long0 | rhs.long0)
    var k = Perm32Encoding.supportMax(lhs.long2 | rhs.long2, lhs.long1 | rhs.long1, lhs.long0 | rhs.long0)
    var i = 0
    assert(k > Perm16Encoding.supportMaxElement)
    @inline def img(preimage: Int) = decode(rhs.long2, rhs.long1, rhs.long0, decode(lhs.long2, lhs.long1, lhs.long0, preimage))
    while (k >= low) {
      i = img(k)
      if (k != i) {
        if (k <= Perm16Encoding.supportMaxElement) {
          var encoding = Perm16Encoding.encode(k, i)
          k -= 1
          while (k >= low) {
            encoding |= Perm16Encoding.encode(k, img(k))
            k -= 1
          }
          return new Perm16(encoding)
        } else {
          val res = new Perm32
          encode(res, k, i)
          k -= 1
          while (k >= low) {
            encode(res, k, img(k))
            k -= 1
          }
          return res
        }
      }
      k -= 1
    }
    Perm16Encoding.id
  }

  def op1632(lhs: Perm16, rhs: Perm32): Perm = {
    if (lhs.isId) return rhs
    val low = Perm16Encoding.supportMin(lhs.encoding).min(Perm32Encoding.supportMin(rhs.long2, rhs.long1, rhs.long0))
    var k = Perm16Encoding.supportMax(lhs.encoding).max(Perm32Encoding.supportMax(rhs.long2, rhs.long1, rhs.long0))
    var i = 0
    assert(k > Perm16Encoding.supportMaxElement)
    @inline def img(preimage: Int) = {
      val inter = if (preimage > Perm16Encoding.supportMaxElement) preimage else Perm16Encoding.decode(lhs.encoding, preimage)
      decode(rhs.long2, rhs.long1, rhs.long0, inter)
    }
    while (k >= low) {
      i = img(k)
      if (k != i) {
        if (k <= Perm16Encoding.supportMaxElement) {
          var encoding = Perm16Encoding.encode(k, i)
          k -= 1
          while (k >= low) {
            encoding |= Perm16Encoding.encode(k, img(k))
            k -= 1
          }
          return new Perm16(encoding)
        } else {
          val res = new Perm32
          encode(res, k, i)
          k -= 1
          while (k >= low) {
            Perm32Encoding.encode(res, k, img(k))
            k -= 1
          }
          return res
        }
      }
      k -= 1
    }
    Perm16Encoding.id
  }

  def op3216(lhs: Perm32, rhs: Perm16): Perm = {
    if (rhs.isId) lhs
    val low = Perm32Encoding.supportMin(lhs.long2, lhs.long1, lhs.long0).min(Perm16Encoding.supportMin(rhs.encoding))
    var k = Perm32Encoding.supportMax(lhs.long2, lhs.long1, lhs.long0).max(Perm16Encoding.supportMax(rhs.encoding))
    var i = 0
    assert(k > Perm16Encoding.supportMaxElement)
    @inline def img(preimage: Int) = {
      val inter = decode(lhs.long2, lhs.long1, lhs.long0, preimage)
      if (inter > Perm16Encoding.supportMaxElement) inter else Perm16Encoding.decode(rhs.encoding, inter)
    }
    while (k >= low) {
      i = img(k)
      if (k != i) {
        if (k <= Perm16Encoding.supportMaxElement) {
          var encoding = Perm16Encoding.encode(k, i)
          k -= 1
          while (k >= low) {
            encoding |= Perm16Encoding.encode(k, img(k))
            k -= 1
          }
          return new Perm16(encoding)
        } else {
          val res = new Perm32
          Perm32Encoding.encode(res, k, i)
          k -= 1
          while (k >= low) {
            Perm32Encoding.encode(res, k, img(k))
            k -= 1
          }
          return res
        }
      }
      k -= 1
    }
    Perm16Encoding.id
  }
}

/*


  def minus(n: Int): Perm32 =
    if (n >= Perm32.Algebra.supportMaxElement)
      sys.error(s"Does not support shifts of more than ${Perm32.Algebra.supportMaxElement} positions.")
    else if (n == 0)
      lhs
    else if (n < 0)
      plus(-n)
    else if (n >= long1Start) {
      assert(long0 == 0)
      new Perm32(0L, long2, long1).specMinus(n - long1Start)
    } else {
      assert((long0 & LongBits.rightFill(n * maskWidth)) == 0)
      val nBits = n * maskWidth
      val leftShift = (numPerLong - n) * maskWidth
      val rFill = rightFill(nBits)
      new Perm32(long2 >>> nBits,
        ((long2 & rFill) << leftShift) + (long1 >>> nBits),
        ((long1 & rFill) << leftShift) + (long0 >>> nBits))
    }

  def plus(n: Int): Perm32 =
    if (n >= Perm32.Algebra.supportMaxElement)
      sys.error(s"Does not support shifts of more than ${Perm32.Algebra.supportMaxElement} positions.")
    else if (n == 0)
      lhs
    else if (n < 0)
      minus(-n)
    else if (n >= long1Start) {
      assert(long2 == 0)
      new Perm32(long1, long0, 0L).specPlus(n - long1Start)
    } else {
      val nBits = n * maskWidth
      val rightShift = (numPerLong - n) * maskWidth
      new Perm32(((long2 << nBits) & longMask) + (long1 >>> rightShift),
        ((long1 << nBits) & longMask) + (long0 >>> rightShift),
        (long0 << nBits) & longMask)
    }
}
 */