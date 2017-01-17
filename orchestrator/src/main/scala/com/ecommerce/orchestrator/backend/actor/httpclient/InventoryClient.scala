package com.ecommerce.orchestrator.backend.actor.httpclient

import java.time.ZonedDateTime
import java.util.UUID

import akka.actor.{ActorLogging, Props, Actor}
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model._
import akka.stream.scaladsl.{Sink, Source}
import com.ecommerce.orchestrator.backend.actor.httpclient.HttpClient.HttpClientResult
import de.heikoseeberger.akkahttpcirce.CirceSupport

import scala.concurrent.Future

/**
  * Created by lukewyman on 1/10/17.
  */
object InventoryClient {

  def props = Props(new InventoryClient)

  def name = "inventory-client"

  case class CreateItem(itemId: UUID)
  case class GetItem(itemId: UUID)
  case class AcceptShipment(itemId: UUID, shipmentId: UUID, date: ZonedDateTime, count: Int)
  case class AcknowledgeShipment(itemId: UUID, shipmentId: UUID, expectedDate: ZonedDateTime, count: Int)
  case class HoldItem(itemId: UUID, shoppingCartId: UUID, count: Int)
  case class ReserveItem(itemId: UUID, customerId: UUID, count: Int)
  case class AbandonCart(itemId: UUID, shoppingCartId: UUID)
  case class Checkout(itemId: UUID, shoppingCartId: UUID, paymentId: UUID)
}

class InventoryClient extends Actor with ActorLogging with InventoryHttpClient {
  import InventoryClient._
  import RequestViews._
  import akka.pattern.pipe
  implicit def executionContext = context.dispatcher
  implicit def system = context.system

  def receive = {
    case CreateItem(iid) =>
      createItem(CreateItemView(iid)).pipeTo(sender())
    case GetItem(iid) =>
      getItem(iid).pipeTo(sender())
    case AcceptShipment(iid, sid, d, c) =>
      acceptShipment(iid, AcceptShipmentView(sid, d, c)).pipeTo(sender())
    case AcknowledgeShipment(iid, sid, ed, c) =>
      acknowledgeShipment(iid, AcknowledgeShhipmentView(sid, ed, c)).pipeTo(sender())
    case HoldItem(iid, scid, c) =>
      holdItem(iid, scid, HoldItemView(c)).pipeTo(sender())
    case ReserveItem(iid, cid, c) =>
      reserveItem(iid, ReserveItemView(cid, c)).pipeTo(sender())
    case AbandonCart(iid, scid) =>
      abandonCart(iid, scid).pipeTo(sender())
    case Checkout(iid, scid, pid) =>
      checkout(iid, scid, CheckoutView(pid)).pipeTo(sender())
  }
}

trait InventoryHttpClient extends HttpClient {
  import CirceSupport._
  import io.circe.generic.auto._
  import io.circe.syntax._
  import io.circe.java8.time._
  import RequestViews._
  import ResponseViews._

  def createItem(civ: CreateItemView): Future[HttpClientResult[InventoryItemView]] = {

    val source = Source.single(HttpRequest(method = HttpMethods.POST,
      entity = HttpEntity(ContentTypes.`application/json`, civ.asJson.toString()),
      uri = Uri(path = Path("/items"))))
    val flow = http.outgoingConnection(host = "localhost", port = 8000).mapAsync(1) { r =>
      deserialize[InventoryItemView](r)
    }
    source.via(flow).runWith(Sink.head)
  }

  def getItem(itemId: UUID): Future[HttpClientResult[InventoryItemView]] = {

    val source = Source.single(HttpRequest(method = HttpMethods.GET,
      uri = Uri(path = Path(s"/items/${itemId}"))))
    val flow = http.outgoingConnection(host = "localhost", port = 9000).mapAsync(1) { r =>
      deserialize[InventoryItemView](r)
    }
    source.via(flow).runWith(Sink.head)
  }

  def acceptShipment(itemId: UUID, asv: AcceptShipmentView): Future[HttpClientResult[InventoryItemView]] = {

    val source = Source.single(HttpRequest(method = HttpMethods.POST,
      entity = HttpEntity(ContentTypes.`application/json`, asv.asJson.toString()),
      uri = Uri(path = Path(s"/items/${itemId}/shipments"))))
    val flow = http.outgoingConnection(host = "localhost", port = 8000).mapAsync(1) { r =>
      deserialize[InventoryItemView](r)
    }
    source.via(flow).runWith(Sink.head)
  }

  def acknowledgeShipment(itemId: UUID, asv: AcknowledgeShhipmentView): Future[HttpClientResult[InventoryItemView]] = {

    val source = Source.single(HttpRequest(method = HttpMethods.POST,
      entity = HttpEntity(ContentTypes.`application/json`, asv.asJson.toString()),
      uri = Uri(path = Path(s"/items/${itemId}/acknowledgements"))))
    val flow = http.outgoingConnection(host = "localhost", port = 8000).mapAsync(1) { r =>
      deserialize[InventoryItemView](r)
    }
    source.via(flow).runWith(Sink.head)
  }

  def holdItem(itemId: UUID, shoppingCartId: UUID, hiv: HoldItemView): Future[HttpClientResult[InventoryItemView]] = {

    val source = Source.single(HttpRequest(method = HttpMethods.POST,
      entity = HttpEntity(ContentTypes.`application/json`, hiv.asJson.toString()),
      uri = Uri(path = Path(s"/items/${itemId}/shoppingcarts/${shoppingCartId}"))))
    val flow = http.outgoingConnection(host = "localhost", port = 8000).mapAsync(1) {r =>
      deserialize[InventoryItemView](r)
    }
    source.via(flow).runWith(Sink.head)
  }

  def reserveItem(itemId: UUID, riv: ReserveItemView): Future[HttpClientResult[InventoryItemView]] = {

    val source = Source.single(HttpRequest(method = HttpMethods.POST,
      entity = HttpEntity(ContentTypes.`application/json`, riv.asJson.toString()),
      uri = Uri(path = Path(s"items/${itemId}/customers"))))
    val flow = http.outgoingConnection(host = "localhost", port = 8000).mapAsync(1) { r =>
      deserialize[InventoryItemView](r)
    }
    source.via(flow).runWith(Sink.head)
  }

  def abandonCart(itemId: UUID, shoppingCartId: UUID): Future[HttpClientResult[InventoryItemView]] = {

    val source = Source.single(HttpRequest(method = HttpMethods.DELETE,
      uri = Uri(path = Path(s"items/${itemId}/shoppingcarts/${shoppingCartId}"))))
    val flow = http.outgoingConnection(host = "localhost", port = 8000).mapAsync(1) { r =>
      deserialize[InventoryItemView](r)
    }
    source.via(flow).runWith(Sink.head)
  }

  def checkout(itemId: UUID, shoppingCartId: UUID, cv: CheckoutView): Future[HttpClientResult[InventoryItemView]] = {

    val source = Source.single(HttpRequest(method = HttpMethods.POST,
      entity = HttpEntity(ContentTypes.`application/json`, cv.asJson.toString()),
      uri = Uri(path = Path(s"items/${itemId}/shoppingcarts/${shoppingCartId}/payments"))))
    val flow = http.outgoingConnection(host = "localhost", port = 8000).mapAsync(1) { r =>
      deserialize[InventoryItemView](r)
    }
    source.via(flow).runWith(Sink.head)
  }
}
