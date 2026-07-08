package com.fkmed.domain.reimbursement;

import java.util.List;

/** The "Veja a documentação necessária" summary for one expense type (SPEC-0015 BR9). */
public record DocumentationGuideView(String expenseType, List<String> items) {}
