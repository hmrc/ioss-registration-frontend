package controllers.amend

import base.SpecBase
import forms.amend.ViewOrChangePreviousRegistrationsMultipleFormProvider
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.ViewOrChangePreviousRegistrationsMultiplePage
import pages.{EmptyWaypoints, JourneyRecoveryPage, Waypoints}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.AuthenticatedUserAnswersRepository
import utils.FutureSyntax.FutureOps
import views.html.amend.ViewOrChangePreviousRegistrationsMultipleView

class ViewOrChangePreviousRegistrationsMultipleControllerSpec extends SpecBase with MockitoSugar {

  private val waypoints: Waypoints = EmptyWaypoints

  private lazy val viewOrChangePreviousRegistrationsMultipleRoute = routes.ViewOrChangePreviousRegistrationsMultipleController.onPageLoad(waypoints).url

  val formProvider = new ViewOrChangePreviousRegistrationsMultipleFormProvider()
  val form: Form[Any] = formProvider()

  "ViewOrChangePreviousRegistrationsMultiple Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo)).build()

      running(application) {
        val request = FakeRequest(GET, viewOrChangePreviousRegistrationsMultipleRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ViewOrChangePreviousRegistrationsMultipleView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = emptyUserAnswersWithVatInfo.set(ViewOrChangePreviousRegistrationsMultiplePage, "").success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, viewOrChangePreviousRegistrationsMultipleRoute)

        val view = application.injector.instanceOf[ViewOrChangePreviousRegistrationsMultipleView]

        val result = route(application, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe view(form.fill(""), waypoints)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, viewOrChangePreviousRegistrationsMultipleRoute)
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe ViewOrChangePreviousRegistrationsMultiplePage.route(waypoints).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo)).build()

      running(application) {
        val request =
          FakeRequest(POST, viewOrChangePreviousRegistrationsMultipleRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[ViewOrChangePreviousRegistrationsMultipleView]

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(boundForm, waypoints)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, viewOrChangePreviousRegistrationsMultipleRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, viewOrChangePreviousRegistrationsMultipleRoute)
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER

        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
