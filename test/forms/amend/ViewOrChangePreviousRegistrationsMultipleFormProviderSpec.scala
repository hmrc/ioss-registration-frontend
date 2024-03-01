package forms.amend

import forms.behaviours.OptionFieldBehaviours
import models.ViewOrChangePreviousRegistrationsMuiltiple
import play.api.data.FormError

class ViewOrChangePreviousRegistrationsMultipleFormProviderSpec extends OptionFieldBehaviours {

  val form = new ViewOrChangePreviousRegistrationsMultipleFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "viewOrChangePreviousRegistrationsMuiltiple.error.required"

    behave like optionsField[ViewOrChangePreviousRegistrationsMuiltiple](
      form,
      fieldName,
      validValues  = ViewOrChangePreviousRegistrationsMuiltiple.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
