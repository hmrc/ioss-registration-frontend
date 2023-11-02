package models.previousRegistrations

import play.api.libs.json._

case class NonCompliantDetails(
                                nonCompliantReturns: Option[Int],
                                nonCompliantPayments: Option[Int]
                              )

object NonCompliantDetails {

  implicit val format: OFormat[NonCompliantDetails] = Json.format[NonCompliantDetails]
}
