package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Controller
public class HelloController {

    @GetMapping("/")
    public String index(Model model) {
        RestTemplate restTemplate = new RestTemplate();
        Map response = restTemplate.getForObject(
            "http://backend:5000/api/hello", // docker-compose service名
            Map.class
        );
        model.addAttribute("message", response.get("message"));
        return "index";
    }
}
