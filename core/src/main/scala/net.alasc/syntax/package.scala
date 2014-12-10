package net.alasc

package object syntax {
  object check extends CheckSyntax
  object monoid extends MonoidSyntax
  object sequence extends SequenceSyntax
  object finiteGroup extends FiniteGroupSyntax
  object permutationAction extends PermutationActionSyntax
  object shiftablePermutation extends ShiftablePermutationSyntax
  object subgroup extends SubgroupSyntax
  object permutationSubgroup extends PermutationSubgroupSyntax
  object semigroupoid extends SemigroupoidSyntax
  object withBase extends WithBaseSyntax
  object partialMonoid extends PartialMonoidSyntax
  object partialMonoidWithBase extends PartialMonoidWithBaseSyntax
  object groupoid extends GroupoidSyntax
  object partialAction extends PartialActionSyntax
  object all extends AllSyntax
}
