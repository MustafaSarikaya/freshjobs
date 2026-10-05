package com.jobscout.api;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class SpaForwardController {

    @GetMapping(
        value = {"/", "/sources", "/admin", "/admin/", "/admin/login", "/admin/login/", "/admin/runs"},
        produces = MediaType.TEXT_HTML_VALUE)
    String forwardSpaRoutes() {
        return "forward:/index.html";
    }
}
