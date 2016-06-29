package net.sigmalab.crdt

import java.util.UUID

import cats.kernel.CommutativeMonoid
import cats.kernel.Order
import cats.implicits._

object GCounterImpl {

  /**
    * Created by schrepfler on 01/05/2016.
    */
  case class GCounterImpl[@specialized(Int, Long, Float, Double) N](shardId: UUID = io.jvm.uuid.UUID.randomUUID(), payload: Map[UUID, N] = Map[UUID, N]()) extends GCounter[UUID, N] {

    override def increment(amt: N)(implicit commutativeMonoid: CommutativeMonoid[N]): GCounterImpl[N] = {
      //    assert(amt >= 0, s"GCounters can only grow, increment $amt is negative")
      payload.get(shardId) match {
        case Some(x) => GCounterImpl(shardId, payload.updated(shardId, amt |+| x))
        case None => GCounterImpl(shardId, payload.updated(shardId, amt))
      }
    }

    override def value()(implicit commutativeMonoid: CommutativeMonoid[N]): N = commutativeMonoid.combineAll(payload.withDefaultValue[N](commutativeMonoid.empty).valuesIterator)

    override def merge(other: StateBased[N, Map[java.util.UUID, N]])(implicit order: Order[N], commutativeMonoid: CommutativeMonoid[N]): GCounterImpl[N] = {
      val mergedPayload = (this.payload.keySet ++ other.payload.keySet).map(uuid => (uuid, this.payload.getOrElse(uuid, commutativeMonoid.empty) max other.payload.getOrElse(uuid, commutativeMonoid.empty))).toMap
      GCounterImpl(shardId, mergedPayload)
    }

  }

  def apply[@specialized(Int, Long, Float, Double) N](param: N): GCounterImpl[N] = {
    val uuid = java.util.UUID.randomUUID()
    GCounterImpl(shardId = uuid, payload = Map(uuid -> param))
  }

}