package com.timetrade.queueservice.server.core.activation

/** Sum type to represent a small finite set of actor placement strategies. */
sealed trait ActorPlacementStrategy {
  this: Product =>
  def name: String = productPrefix
}

sealed trait LocationActorPlacementStrategy extends ActorPlacementStrategy {
  this: Product =>
}

/** When creating a LocationActor, deploy to least-loaded cluster member. */
case object LocationActorToLeastLoaded extends LocationActorPlacementStrategy

/** When creating a LocationActor, deploy to any "other" cluster member, from the member where
 *  the LicenseeActor is running.
 */
case object LocationActorToOther extends LocationActorPlacementStrategy

/** When creating a LocationActor, deploy to cluster members in a round-robin fashion. */
case object LocationActorRoundRobin extends LocationActorPlacementStrategy

/** Select node based on cluster metric data.
  * Uses random selection based on probabilities derived from the remaining capacity of corresponding node */
case object LocationActorAdaptivePlacement extends LocationActorPlacementStrategy

//sealed trait QueueActorPlacementStrategy extends ActorPlacementStrategy
//
///** When creating a QueueActor, deploy to least-loaded cluster member. */
//case object QueueActorToLeastLoaded extends QueueActorPlacementStrategy { val name = productPrefix }
//
///** When creating a QueueActor, deploy to any "other" cluster member, from the member where
// *  the LocationActor is running.
// */
//case object QueueActorToOther extends QueueActorPlacementStrategy { val name = productPrefix }
//
///** When creating a QueueActor, deploy to cluster members in a round-robin fashion. */
//case object QueueActorRoundRobin extends QueueActorPlacementStrategy { val name = productPrefix }

object ActorPlacementStrategy {

  private val strategies = Array(
    LocationActorToLeastLoaded
    ,LocationActorToOther
    ,LocationActorRoundRobin
//    ,QueueActorToLeastLoaded
//    ,QueueActorToOther
//    ,QueueActorRoundRobin
  )

  private val strategiesByName = Map() ++ (
    for {
      strategy <- strategies
    } yield strategy.name -> strategy)

  def apply(name: String): ActorPlacementStrategy = strategiesByName(name)
}
