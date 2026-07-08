package com.fkmed.domain.reimbursement;

import java.util.List;

/**
 * The wizard's registries (SPEC-0015 §API Contracts): expense types, professional councils, banks.
 */
public record CatalogView(
    List<ExpenseTypeView> expenseTypes,
    List<ProfessionalCouncilView> councils,
    List<BankView> banks) {

  public record ExpenseTypeView(String code, String name) {}

  public record ProfessionalCouncilView(String code, String name) {}

  public record BankView(String code, String name) {}
}
