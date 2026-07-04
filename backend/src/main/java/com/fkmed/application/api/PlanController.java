package com.fkmed.application.api;

import com.fkmed.domain.plan.MyPlanResponse;
import com.fkmed.domain.plan.PlanService;
import com.fkmed.infra.security.UserContextProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The Meu Plano endpoint (SPEC-0001 BR6): plan + family of the logged-in beneficiary. */
@RestController
@RequestMapping("/api/plan")
@RequiredArgsConstructor
public class PlanController {

  private final PlanService planService;
  private final UserContextProvider userContext;

  @GetMapping("/my-plan")
  MyPlanResponse myPlan() {
    return planService.myPlanFor(userContext.current().beneficiaryCard().orElse(null));
  }
}
