package com.fkmed.application.api;

import com.fkmed.domain.content.HomeContent;
import com.fkmed.domain.content.HomeContentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The Home content endpoint (SPEC-0005 BR6/BR7): visible banners and active notices. */
@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
public class ContentController {

  private final HomeContent homeContent;

  @GetMapping("/home")
  HomeContentResponse home() {
    return homeContent.home();
  }
}
