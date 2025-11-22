package kr.ac.ewha.catchMind.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GameController {
    @GetMapping("/game/guesser")
    public String showGuesser() {
        return "game/mainUI_Guesser";
    }

    @GetMapping("/game/drawer")
    public String showDrawer() {
        return "game/mainUI_Drawer";
    }
}
