package com.fkmed.infra.web;

import com.fkmed.domain.appointment.AppointmentNotFoundException;
import com.fkmed.domain.appointment.AppointmentOutsideHorizonException;
import com.fkmed.domain.appointment.AppointmentTimeConflictException;
import com.fkmed.domain.appointment.AppointmentTooLateException;
import com.fkmed.domain.appointment.MedicalOrderInvalidException;
import com.fkmed.domain.appointment.MedicalOrderRequiredException;
import com.fkmed.domain.appointment.SlotUnavailableException;
import com.fkmed.domain.card.CardUnavailableException;
import com.fkmed.domain.error.DomainException;
import com.fkmed.domain.identity.AccountAlreadyExistsException;
import com.fkmed.domain.identity.ConcurrentAccountUpdateException;
import com.fkmed.domain.identity.CurrentPasswordIncorrectException;
import com.fkmed.domain.identity.DependentUnderageException;
import com.fkmed.domain.identity.EmailAlreadyUsedException;
import com.fkmed.domain.identity.LegalVersionOutdatedException;
import com.fkmed.domain.identity.PasswordPolicyViolationException;
import com.fkmed.domain.identity.RegistrationNotFoundException;
import com.fkmed.domain.identity.ResetLinkInvalidException;
import com.fkmed.domain.identity.VerificationLinkInvalidException;
import com.fkmed.domain.network.NetworkQueryTooShortException;
import com.fkmed.domain.network.OutsideCoverageException;
import com.fkmed.domain.network.ProviderUnavailableException;
import com.fkmed.domain.notification.MandatoryPreferenceOptOutException;
import com.fkmed.domain.notification.NotificationNotFoundException;
import com.fkmed.domain.plan.BeneficiaryNotAccessibleException;
import com.fkmed.domain.plan.CepInvalidException;
import com.fkmed.domain.plan.ContactEmailInvalidException;
import com.fkmed.domain.plan.ContactEmailRequiredException;
import com.fkmed.domain.plan.LandlineInvalidException;
import com.fkmed.domain.plan.MobileInvalidException;
import com.fkmed.domain.plan.MobileRequiredException;
import com.fkmed.domain.plan.PhotoInvalidContentException;
import com.fkmed.domain.plan.PhotoTooLargeException;
import com.fkmed.domain.plan.PlanNotFoundException;
import com.fkmed.domain.plan.UfInvalidException;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;

/**
 * The presentation-layer registry mapping each {@link DomainException} subclass to its HTTP status
 * (DECISIONS-BASELINE §0011). A build-time completeness test fails when a subclass lacks an
 * explicit entry; {@link #DEFAULT_STATUS} (422) is the documented fallback for exceptions
 * introduced mid-slice.
 */
public final class HttpErrorMapping {

  static final HttpStatus DEFAULT_STATUS = HttpStatus.UNPROCESSABLE_CONTENT;

  private static final Map<Class<? extends DomainException>, HttpStatus> MAPPINGS =
      Map.ofEntries(
          Map.entry(PlanNotFoundException.class, HttpStatus.NOT_FOUND),
          // SPEC-0003 BR2/BR3: an out-of-scope beneficiary is a not-found — existence is not
          // revealed.
          Map.entry(BeneficiaryNotAccessibleException.class, HttpStatus.NOT_FOUND),
          // SPEC-0002 §Error Behavior.
          Map.entry(RegistrationNotFoundException.class, HttpStatus.UNPROCESSABLE_CONTENT),
          Map.entry(AccountAlreadyExistsException.class, HttpStatus.CONFLICT),
          Map.entry(DependentUnderageException.class, HttpStatus.UNPROCESSABLE_CONTENT),
          Map.entry(EmailAlreadyUsedException.class, HttpStatus.CONFLICT),
          Map.entry(PasswordPolicyViolationException.class, HttpStatus.UNPROCESSABLE_CONTENT),
          Map.entry(VerificationLinkInvalidException.class, HttpStatus.GONE),
          Map.entry(ResetLinkInvalidException.class, HttpStatus.GONE),
          Map.entry(CurrentPasswordIncorrectException.class, HttpStatus.UNPROCESSABLE_CONTENT),
          // Débito técnico A (DL-0005): a concurrent-update conflict is retryable by the client.
          Map.entry(ConcurrentAccountUpdateException.class, HttpStatus.CONFLICT),
          // SPEC-0007 BR10: an inactive beneficiary's card is unavailable, distinct from the 404
          // out-of-scope case above.
          Map.entry(CardUnavailableException.class, HttpStatus.CONFLICT),
          // SPEC-0004 §Error Behavior.
          Map.entry(NotificationNotFoundException.class, HttpStatus.NOT_FOUND),
          Map.entry(MandatoryPreferenceOptOutException.class, HttpStatus.UNPROCESSABLE_CONTENT),
          // SPEC-0006 §Error Behavior: contact/photo validation is a 422; an outdated
          // legal-document
          // acceptance is a 409 (a newer version exists).
          Map.entry(ContactEmailRequiredException.class, HttpStatus.UNPROCESSABLE_CONTENT),
          Map.entry(ContactEmailInvalidException.class, HttpStatus.UNPROCESSABLE_CONTENT),
          Map.entry(MobileRequiredException.class, HttpStatus.UNPROCESSABLE_CONTENT),
          Map.entry(MobileInvalidException.class, HttpStatus.UNPROCESSABLE_CONTENT),
          Map.entry(LandlineInvalidException.class, HttpStatus.UNPROCESSABLE_CONTENT),
          Map.entry(CepInvalidException.class, HttpStatus.UNPROCESSABLE_CONTENT),
          Map.entry(UfInvalidException.class, HttpStatus.UNPROCESSABLE_CONTENT),
          Map.entry(PhotoInvalidContentException.class, HttpStatus.UNPROCESSABLE_CONTENT),
          Map.entry(PhotoTooLargeException.class, HttpStatus.UNPROCESSABLE_CONTENT),
          Map.entry(LegalVersionOutdatedException.class, HttpStatus.CONFLICT),
          // SPEC-0008 §Error Behavior.
          Map.entry(NetworkQueryTooShortException.class, HttpStatus.UNPROCESSABLE_CONTENT),
          Map.entry(ProviderUnavailableException.class, HttpStatus.GONE),
          Map.entry(OutsideCoverageException.class, HttpStatus.UNPROCESSABLE_CONTENT),
          // SPEC-0009 §Error Behavior: the slot race and time conflict are retryable 409s; the
          // attachment and horizon failures are 422s; the appointment 404 never reveals existence.
          Map.entry(SlotUnavailableException.class, HttpStatus.CONFLICT),
          Map.entry(AppointmentTimeConflictException.class, HttpStatus.CONFLICT),
          Map.entry(AppointmentTooLateException.class, HttpStatus.CONFLICT),
          Map.entry(MedicalOrderRequiredException.class, HttpStatus.UNPROCESSABLE_CONTENT),
          Map.entry(MedicalOrderInvalidException.class, HttpStatus.UNPROCESSABLE_CONTENT),
          Map.entry(AppointmentOutsideHorizonException.class, HttpStatus.UNPROCESSABLE_CONTENT),
          Map.entry(AppointmentNotFoundException.class, HttpStatus.NOT_FOUND));

  private HttpErrorMapping() {}

  /** The HTTP status registered for the exception's type, or 422 when unmapped. */
  public static HttpStatus statusOf(DomainException exception) {
    return MAPPINGS.getOrDefault(exception.getClass(), DEFAULT_STATUS);
  }

  /** The explicitly mapped exception types (consumed by the completeness gate). */
  public static Set<Class<? extends DomainException>> mappedTypes() {
    return MAPPINGS.keySet();
  }
}
