package com.fkmed.domain.upload;

/** Fixed technical namespaces used only to organize server-generated storage keys. */
public enum StorageNamespace {
  PROFILE_PHOTO("profile-photo"),
  APPOINTMENT_ORDER("appointment-order"),
  REIMBURSEMENT_DOCUMENT("reimbursement-document"),
  REIMBURSEMENT_PREVIEW("reimbursement-preview");

  private final String path;

  StorageNamespace(String path) {
    this.path = path;
  }

  public String path() {
    return path;
  }
}
