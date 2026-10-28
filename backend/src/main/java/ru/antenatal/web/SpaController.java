package ru.antenatal.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping({"/", "/app", "/app/**", "/cabinet", "/cabinet/**", "/invite"})
    public String spa() {
        return "forward:/index.html";
    }
}
