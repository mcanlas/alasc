/*
# Orbits #
*/
package net.alasc

trait GenOrbit {
  def builder: OrbitBuilder
  def beta: Dom
  def isDefinedAt(k: Dom): Boolean
  def contains(k: Dom): Boolean
  def size: Int
  def orbitSet: Set[Dom]
}

trait GenOrbitImpl extends GenOrbit {
  def contains(k: Dom) = isDefinedAt(k)
}

trait Orbit[F <: Finite[F]] extends GenOrbit {
  def action: PRepr[F]
  /** Add the new generators newGen to the orbit.
    * 
    * @param newGen   New generators to add to the orbit.
    * @param gens     Generators of the orbit, including newGen.
    * 
    * @return New Orbit including newGen.
    */
  def updated(newGen: Iterable[F], gens: Iterable[F]): Orbit[F]
}

trait OrbitBuilder {
  def empty[F <: Finite[F]](beta: Dom, action: PRepr[F]): Orbit[F]
  def fromSet[F <: Finite[F]](beta: Dom, action: PRepr[F], set: Iterable[F]): Orbit[F] =
    empty(beta, action).updated(set, set)
}

/*
## Implementation of `Orbit` using a `Set`
*/
case class OrbitSet[F <: Finite[F]](beta: Dom, action: PRepr[F], intOrbit: collection.immutable.BitSet) extends Orbit[F] with GenOrbitImpl {
  def orbitSet = intOrbit.map(k => Dom._0(k))
  def builder = OrbitSet
  def size = intOrbit.size
  def isDefinedAt(k: Dom) = intOrbit(k._0)
  def updated(newGen: Iterable[F], gens: Iterable[F]): OrbitSet[F] = {
    val newOrbit = collection.mutable.BitSet.empty
    for (k <- intOrbit; g <- newGen)
      newOrbit += action(g, Dom._0(k))._0
    val newPoints = newOrbit.clone
    for (k <- intOrbit)
      newPoints -= k
    if (newPoints.isEmpty)
      return this
    def checkForNew: Boolean = {
      val ret = false
      for (k <- newOrbit; g <- gens) {
        val img = action(g, Dom._0(k))._0
        if (!newOrbit.contains(img)) {
          newOrbit += img
          return true
        }
      }
      false
    }
    while (checkForNew) { }
    OrbitSet(beta, action, newOrbit.toImmutable)
  }
}

object OrbitSet extends OrbitBuilder {
  def empty[F <: Finite[F]](beta: Dom, action: PRepr[F]) = 
    OrbitSet(beta, action, collection.immutable.BitSet(beta._0))
}
