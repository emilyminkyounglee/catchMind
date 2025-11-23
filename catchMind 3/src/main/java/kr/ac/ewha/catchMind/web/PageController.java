package kr.ac.ewha.catchMind;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    // 홈 화면: home_page.html
    @GetMapping("/")
    public String home() {
        return "home_page";
    }

    // Drawer 화면: drawer.html
    @GetMapping("/drawer")
    public String drawer() {
        return "mainUI_Drawer";
    }

    // Guesser 화면: guesser.html
    @GetMapping("/guesser")
    public String guesser() {
        return "mainUI_Guesser";
    }

    // 중간 결과 화면: midResult.html
    @GetMapping("/mid-result")
    public String midResult() {
        return "midResult";
    }

    // 최종 결과 화면: result.html
    @GetMapping("/result")
    public String result() {
        return "finalResult";
    }
}