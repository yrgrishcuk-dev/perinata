package ru.antenatal.web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SpaController {

    @GetMapping({"/", "/app", "/app/**", "/cabinet", "/cabinet/**", "/invite"})
    public String spa() {
        return "forward:/index.html";
    }

    @GetMapping(value = "/site.webmanifest", produces = "application/manifest+json")
    @ResponseBody
    public Resource manifest() {
        return new ClassPathResource("static/site.webmanifest");
    }
}
