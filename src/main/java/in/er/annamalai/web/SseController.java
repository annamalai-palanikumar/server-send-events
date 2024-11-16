package in.er.annamalai.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class SseController {
    
    @GetMapping(value = "/")
    public String home() {
        return "index";
    }

    @GetMapping(value = "/broadcast")
    public String broadcast() {
        return "broadcast";
    }
    
}
