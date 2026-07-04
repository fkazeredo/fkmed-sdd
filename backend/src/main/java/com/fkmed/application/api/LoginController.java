package com.fkmed.application.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Serves the pt-BR form-login page of the embedded Authorization Server. */
@Controller
public class LoginController {

  @GetMapping("/login")
  String login() {
    return "login";
  }
}
