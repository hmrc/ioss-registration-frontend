/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services

import base.SpecBase
import generators.Generators
import models.core.{Match, MatchType}
import models.domain.PreviousSchemeNumbers
import models.requests.AuthenticatedDataRequest
import models.{Country, Index, PreviousScheme, PreviousSchemeType}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.MockitoSugar.when
import org.scalacheck.Gen
import org.scalatest.PrivateMethodTester
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pages.previousRegistrations.{PreviousEuCountryPage, PreviousOssNumberPage, PreviousSchemePage, PreviousSchemeTypePage}
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import testutils.RegistrationData
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Month._
import java.time._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DateServiceSpec extends SpecBase with ScalaCheckPropertyChecks with Generators with PrivateMethodTester {

  private def getStubClock(date: LocalDate): Clock =
    Clock.fixed(date.atStartOfDay(ZoneId.systemDefault).toInstant, ZoneId.systemDefault)

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val request = AuthenticatedDataRequest(FakeRequest("GET", "/"), testCredentials, vrn, None, emptyUserAnswers)

  private implicit val dataRequest: AuthenticatedDataRequest[AnyContent] = AuthenticatedDataRequest(request, testCredentials, vrn, None, emptyUserAnswers)
  private val coreRegistrationValidationService: CoreRegistrationValidationService = mock[CoreRegistrationValidationService]

  ".startOfNextQuarter" - {

    "must be 1st January of the next year for any date in October, November or December" in {

      forAll(datesBetween(LocalDate.of(2021, 10, 1), LocalDate.of(2021, 12, 31))) {
        date =>
          val stubClock = getStubClock(date)
          val service   = new DateService(stubClock, coreRegistrationValidationService)

          service.startOfNextQuarter() mustEqual LocalDate.of(2022, 1, 1)
      }
    }

    "must be 1st April for any date in January, February or March" in {

      forAll(datesBetween(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 3, 31))) {
        date =>
          val stubClock = getStubClock(date)
          val service   = new DateService(stubClock, coreRegistrationValidationService)

          service.startOfNextQuarter() mustEqual LocalDate.of(2022, 4, 1)
      }
    }

    "must be 1st July for any date in April, May or June" in {

      forAll(datesBetween(LocalDate.of(2022, 4, 1), LocalDate.of(2022, 6, 30))) {
        date =>
          val stubClock = getStubClock(date)
          val service   = new DateService(stubClock, coreRegistrationValidationService)

          service.startOfNextQuarter() mustEqual LocalDate.of(2022, 7, 1)
      }
    }

    "must be 1st October for any date in July, August or September" in {

      forAll(datesBetween(LocalDate.of(2022, 7, 1), LocalDate.of(2022, 9, 30))) {
        date =>
          val stubClock = getStubClock(date)
          val service   = new DateService(stubClock, coreRegistrationValidationService)

          service.startOfNextQuarter() mustEqual LocalDate.of(2022, 10, 1)
      }
    }
  }

  ".lastDayOfNextCalendarQuarter" - {

    "must be 31st March of the next year for any date in October, November or December" in {
      forAll(datesBetween(LocalDate.of(2021, 10, 1), LocalDate.of(2021, 12, 31))) {
        date =>
          val stubClock = getStubClock(date)
          val service   = new DateService(stubClock, coreRegistrationValidationService)

          service.lastDayOfNextCalendarQuarter mustEqual LocalDate.of(2022, 3, 31)
      }
    }

    "must be 30th June for any date in January, February or March" in {

      forAll(datesBetween(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 3, 31))) {
        date =>
          val stubClock = getStubClock(date)
          val service   = new DateService(stubClock, coreRegistrationValidationService)

          service.lastDayOfNextCalendarQuarter mustEqual LocalDate.of(2022, 6, 30)
      }
    }

    "must be 30th September for any date in April, May or June" in {

      forAll(datesBetween(LocalDate.of(2022, 4, 1), LocalDate.of(2022, 6, 30))) {
        date =>
          val stubClock = getStubClock(date)
          val service   = new DateService(stubClock, coreRegistrationValidationService)

          service.lastDayOfNextCalendarQuarter mustEqual LocalDate.of(2022, 9, 30)
      }
    }

    "must be 31st December for any date in July, August or September" in {

      forAll(datesBetween(LocalDate.of(2022, 7, 1), LocalDate.of(2022, 9, 30))) {
        date =>
          val stubClock = getStubClock(date)
          val service   = new DateService(stubClock, coreRegistrationValidationService)

          service.lastDayOfNextCalendarQuarter mustEqual LocalDate.of(2022, 12, 31)
      }
    }
  }

  ".lastDayOfMonthAfterNextCalendarQuarter" - {

    "must be 30th April of the next year for any date in October, November or December" in {

      forAll(datesBetween(LocalDate.of(2021, 10, 1), LocalDate.of(2021, 12, 31))) {
        date =>
          val stubClock = getStubClock(date)
          val service   = new DateService(stubClock, coreRegistrationValidationService)

          service.lastDayOfMonthAfterNextCalendarQuarter mustEqual LocalDate.of(2022, 4, 30)
      }
    }

    "must be 31st July for any date in January, February or March" in {

      forAll(datesBetween(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 3, 31))) {
        date =>
          val stubClock = getStubClock(date)
          val service   = new DateService(stubClock, coreRegistrationValidationService)

          service.lastDayOfMonthAfterNextCalendarQuarter mustEqual LocalDate.of(2022, 7, 31)
      }
    }

    "must be 31st October for any date in April, May or June" in {

      forAll(datesBetween(LocalDate.of(2022, 4, 1), LocalDate.of(2022, 6, 30))) {
        date =>
          val stubClock = getStubClock(date)
          val service   = new DateService(stubClock, coreRegistrationValidationService)

          service.lastDayOfMonthAfterNextCalendarQuarter mustEqual LocalDate.of(2022, 10, 31)
      }
    }

    "must be 31st January for any date in July, August or September" in {

      forAll(datesBetween(LocalDate.of(2022, 7, 1), LocalDate.of(2022, 9, 30))) {
        date =>
          val stubClock = getStubClock(date)
          val service   = new DateService(stubClock, coreRegistrationValidationService)

          service.lastDayOfMonthAfterNextCalendarQuarter mustEqual LocalDate.of(2023, 1, 31)
      }
    }
  }

  ".lastDayOfCalendarQuarter" - {

    "must be 30th September for any date in July, August or September" in {

      forAll(datesBetween(LocalDate.of(2022, 7, 1), LocalDate.of(2022, 9, 30))) {
        date =>
          val stubClock = getStubClock(date)
          val service   = new DateService(stubClock, coreRegistrationValidationService)

          service.lastDayOfCalendarQuarter mustEqual LocalDate.of(2022, 9, 30)
      }
    }
  }

  ".lastDayOfMonthAfterCalendarQuarter" - {

    "must be 31st October for any date in July, August or September" in {

      forAll(datesBetween(LocalDate.of(2022, 7, 1), LocalDate.of(2022, 9, 30))) {
        date =>
          val stubClock = getStubClock(date)
          val service   = new DateService(stubClock, coreRegistrationValidationService)

          service.lastDayOfMonthAfterCalendarQuarter mustEqual LocalDate.of(2022, 10, 31)
      }
    }
  }

  ".isDateOfFirstSaleDifferentToCommencementDate" - {

    "must return true if the Date Of First Sale is a different date to the Commencement Date" in {
      val dates: Gen[(LocalDate, LocalDate)] = for {
        dateOfFirstSale  <- datesBetween(
          LocalDate.of(2021, 7, 1),
          LocalDate.now().minusDays(1)
        )
        extraMonths <- Gen.choose(2, 12)
      } yield (dateOfFirstSale, dateOfFirstSale.plusMonths(extraMonths))

      val commencementDate = LocalDate.now()

      forAll(dates) {
        case (dateOfFirstSale, dateOfFirstSalePlusMonths) =>
          val stubClock = getStubClock(dateOfFirstSalePlusMonths)
          val service = new DateService(stubClock, coreRegistrationValidationService)

          service.isDOFSDifferentToCommencementDate(Some(dateOfFirstSale), commencementDate) mustEqual true
      }
    }

    "must return false if the Date Of First Sale is the same date as the Commencement Date" in {
      val service = new DateService(getStubClock(LocalDate.now()), coreRegistrationValidationService)

      service.isDOFSDifferentToCommencementDate(Some(LocalDate.now()), LocalDate.now()) mustEqual false
    }

    "must return false if the Date Of First Sale is empty" in {
      val service = new DateService(getStubClock(LocalDate.now()), coreRegistrationValidationService)

      service.isDOFSDifferentToCommencementDate(None, LocalDate.now()) mustEqual false
    }
  }

  "getVatReturnEndDate" - {

    "must return the end of the quarter based on commencement date" in {
      val commencementDate         = LocalDate.of(2021, 8, 11)
      val expectedVatReturnEndDate = LocalDate.of(2021, 9, 30)
      val service = new DateService(getStubClock(LocalDate.now()), coreRegistrationValidationService)

      val vatReturnEndDate = service.getVatReturnEndDate(commencementDate)

      vatReturnEndDate mustEqual expectedVatReturnEndDate
    }

  }

  "getVatReturnDeadline" - {

    "must return the end of the month following the VAT Return end date" in {
      val vatReturnsEndDate = LocalDate.of(2021, 9, 30)
      val expectedVatReturnDeadline = LocalDate.of(2021, 10, 31)
      val service = new DateService(getStubClock(LocalDate.now()), coreRegistrationValidationService)

      val vatReturnDeadline = service.getVatReturnDeadline(vatReturnsEndDate)

      vatReturnDeadline mustEqual expectedVatReturnDeadline
    }
  }

  ".earliestSaleAllowed" - {

    "between 1st July 2021 and 30th September 2021" - {

      "must be 1st July 2021" in {

        forAll(datesBetween(LocalDate.of(2021, 7, 1), LocalDate.of(2021, 9, 30))) {
          date =>
            val stubClock = getStubClock(date)
            val service   = new DateService(stubClock, coreRegistrationValidationService)

            service.earliestSaleAllowed() mustEqual LocalDate.of(2021, 7, 1)
        }
      }
    }

    "after 30th September 2021" - {

      "in the 1st to the 10th day of any quarter" - {

        "must be the 1st of the previous month" in {

          val dates = for {
            day   <- Gen.choose(1, 10)
            month <- Gen.oneOf(JANUARY, APRIL, JULY, OCTOBER).map(_.getValue)
            year  <- Gen.choose(2022, 2030)
          } yield LocalDate.of(year, month, day)

          forAll(dates) {
            date: LocalDate =>
              val stubClock = getStubClock(date)
              val service   = new DateService(stubClock, coreRegistrationValidationService)

              service.earliestSaleAllowed() mustEqual date.minusMonths(1).withDayOfMonth(1)
          }
        }
      }

      "in the 11th day onwards of the first month of any quarter" - {

        "must be the first day of the quarter" in {

          val datesIn31DayMonths = for {
            day   <- Gen.choose(11, 31)
            month <- Gen.oneOf(JANUARY, JULY, OCTOBER).map(_.getValue)
            year  <- Gen.choose(2022, 2030)
          } yield LocalDate.of(year, month, day)

          val datesInApril = for {
            day  <- Gen.choose(11, 30)
            year <- Gen.choose(2022, 2030)
          } yield LocalDate.of(year, APRIL.getValue, day)

          val dates = Gen.oneOf(datesInApril, datesIn31DayMonths)

          forAll(dates) {
            date =>
              val stubClock = getStubClock(date)
              val service   = new DateService(stubClock, coreRegistrationValidationService)

              service.earliestSaleAllowed() mustEqual date.withDayOfMonth(1)
          }
        }
      }

      "on any day after the first month of any quarter" - {

        "must be the first day of the quarter" in {

          val dates = for {
            year  <- Gen.choose(2022, 2030)
            month <- Gen.oneOf(FEBRUARY, MARCH, MAY, JUNE, AUGUST, SEPTEMBER, NOVEMBER, DECEMBER)
            day   <- Gen.choose(1, month.length(Year.of(year).isLeap))
            quarterStart = month match {
              case FEBRUARY | MARCH     => JANUARY
              case MAY      | JUNE      => APRIL
              case AUGUST   | SEPTEMBER => JULY
              case NOVEMBER | DECEMBER  => OCTOBER
              case _ => month
            }
          } yield (LocalDate.of(year, month, day), LocalDate.of(year, quarterStart, 1))

          forAll(dates) {
            case (today, startOfQuarter) =>
              val stubClock = getStubClock(today)
              val service   = new DateService(stubClock, coreRegistrationValidationService)

              service.earliestSaleAllowed() mustEqual startOfQuarter
          }
        }
      }
    }
  }

  "calculateCommencementDate" - {

    "must return exclusion effective date as commencement date when trader has a previous registration and is transferring MSID" in {

      val exclusionEffectiveDate = LocalDate.of(2023, 3, 15)
      val aMatch = Match(MatchType.TransferringMSID, "123456789-id", None, "EE", Some(7), None, Some(exclusionEffectiveDate), None, None)

      when(coreRegistrationValidationService.searchScheme(
        eqTo("123456789"),
        eqTo(PreviousScheme.OSSU),
        eqTo(None),
        eqTo("EE"))(any(), any())) thenReturn Future.successful(Some(aMatch))

      val index = Index(0)

      val stubClock = getStubClock(LocalDate.of(2023, 4, 1))
      val userAnswers = emptyUserAnswers
        .set(PreviousEuCountryPage(index), Country("EE", "Estonia")).success.value
        .set(PreviousSchemeTypePage(index, index), PreviousSchemeType.OSS).success.value
        .set(PreviousSchemePage(index, index), PreviousScheme.OSSU).success.value
        .set(PreviousOssNumberPage(index, index), PreviousSchemeNumbers("123456789", None)).success.value

      val dateService = new DateService(stubClock, coreRegistrationValidationService)

      dateService.calculateCommencementDate(userAnswers).futureValue mustBe exclusionEffectiveDate

    }

    "must return exclusion effective date as commencement date when there are multiple previous registrations and one is transferring MSID" in {

      val exclusionEffectiveDate = LocalDate.of(2023, 3, 15)
      val aMatch = Match(MatchType.TransferringMSID, "123456789-id", None, "EE", Some(7), None, Some(exclusionEffectiveDate), None, None)

      when(coreRegistrationValidationService.searchScheme(
        eqTo("123456789"),
        eqTo(PreviousScheme.OSSU),
        eqTo(None),
        eqTo("EE"))(any(), any())) thenReturn Future.successful(Some(aMatch))

      val index = Index(0)
      val index1 = Index(1)

      val stubClock = getStubClock(LocalDate.of(2023, 4, 1))
      val userAnswers = emptyUserAnswers
        .set(PreviousEuCountryPage(index), Country("EE", "Estonia")).success.value
        .set(PreviousSchemeTypePage(index, index), PreviousSchemeType.OSS).success.value
        .set(PreviousSchemePage(index, index), PreviousScheme.OSSU).success.value
        .set(PreviousOssNumberPage(index, index), PreviousSchemeNumbers("123456789", None)).success.value
        .set(PreviousSchemePage(index, index1), PreviousScheme.OSSNU).success.value
        .set(PreviousOssNumberPage(index, index1), PreviousSchemeNumbers("345678912", None)).success.value
        .set(PreviousEuCountryPage(index1), Country("FR", "France")).success.value
        .set(PreviousSchemeTypePage(index1, index), PreviousSchemeType.OSS).success.value
        .set(PreviousSchemePage(index1, index), PreviousScheme.OSSNU).success.value
        .set(PreviousOssNumberPage(index1, index), PreviousSchemeNumbers("234567891", None)).success.value

      val dateService = new DateService(stubClock, coreRegistrationValidationService)

      dateService.calculateCommencementDate(userAnswers).futureValue mustBe exclusionEffectiveDate

    }

    "must return commencement date in next quarter when not declaring a sale now" in {
      val stubClock = getStubClock(LocalDate.of(2023, 4, 1))
      val commencementDate = LocalDate.of(2023, 7, 1)

      val userAnswers = emptyUserAnswers

      val dateService = new DateService(stubClock, coreRegistrationValidationService)

      dateService.calculateCommencementDate(userAnswers).futureValue mustBe commencementDate
    }

  }

  "calculateFinalAmendmentDate" - {

    "must return 10th of April given commencement date is 20th March" in {
      val stubClock = getStubClock(LocalDate.of(2023, 3, 21))
      val commencementDate = LocalDate.of(2023, 3, 20)

      val expectedFinalAmendmentDate = LocalDate.of(2023, 4, 10)

      val dateService = new DateService(stubClock, coreRegistrationValidationService)

      val result = dateService.calculateFinalAmendmentDate(commencementDate)

      result mustBe expectedFinalAmendmentDate
    }

    "must return 10th October given commencement date is 14th July" in {
      val stubClock = getStubClock(LocalDate.of(2023, 7, 14))
      val commencementDate = LocalDate.of(2023, 7, 14)

      val expectedFinalAmendmentDate = LocalDate.of(2023, 10, 10)

      val dateService = new DateService(stubClock, coreRegistrationValidationService)

      val result = dateService.calculateFinalAmendmentDate(commencementDate)

      result mustBe expectedFinalAmendmentDate
    }

    "must return 10th October given commencement date is 14th July if today is 11th November" in {
      val stubClock = getStubClock(LocalDate.of(2023, 11, 11))
      val commencementDate = LocalDate.of(2023, 7, 14)

      val expectedFinalAmendmentDate = LocalDate.of(2023, 10, 10)

      val dateService = new DateService(stubClock, coreRegistrationValidationService)

      val result = dateService.calculateFinalAmendmentDate(commencementDate)

      result mustBe expectedFinalAmendmentDate
    }

    "must return 10th July given commencement date is 1st Jult if today is 19th June" in {

      val stubClock = getStubClock(LocalDate.of(2023, 6, 19))
      val commencementDate = LocalDate.of(2023, 7, 1)

      val expectedFinalAmendmentDate = LocalDate.of(2023, 7, 10)

      val dateService = new DateService(stubClock, coreRegistrationValidationService)

      val result = dateService.calculateFinalAmendmentDate(commencementDate)

      result mustBe expectedFinalAmendmentDate
    }

    "must return 10th OCtober given commencement date is 1st July if today is 1st July and there is a registration already which was today" in {

      implicit val dataRequest: AuthenticatedDataRequest[AnyContent] =
        AuthenticatedDataRequest(
          request,
          testCredentials,
          vrn,
          Some(
            RegistrationData.registration.copy(
              submissionReceived = Some(Instant.parse("2023-07-01T12:00:00Z"))
            )
          ),
          emptyUserAnswers
        )

      val stubClock = getStubClock(LocalDate.of(2023, 7, 1))
      val commencementDate = LocalDate.of(2023, 7, 1)

      val expectedFinalAmendmentDate = LocalDate.of(2023, 10, 10)

      val dateService = new DateService(stubClock, coreRegistrationValidationService)

      val result = dateService.calculateFinalAmendmentDate(commencementDate)

      result mustBe expectedFinalAmendmentDate
    }

  }
}
