package com.fkmed.domain.upload;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StorageNamespaceTest {

  @Test
  void exposesStableServerControlledPaths() {
    assertThat(StorageNamespace.PROFILE_PHOTO.path()).isEqualTo("profile-photo");
    assertThat(StorageNamespace.APPOINTMENT_ORDER.path()).isEqualTo("appointment-order");
    assertThat(StorageNamespace.REIMBURSEMENT_DOCUMENT.path()).isEqualTo("reimbursement-document");
    assertThat(StorageNamespace.REIMBURSEMENT_PREVIEW.path()).isEqualTo("reimbursement-preview");
  }
}
