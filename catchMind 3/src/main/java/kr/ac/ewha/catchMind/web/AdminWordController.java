package kr.ac.ewha.catchMind.web;


import kr.ac.ewha.catchMind.model.WordDictionary;
import kr.ac.ewha.catchMind.repository.WordDictionaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/words")
public class AdminWordController {
    private final WordDictionaryRepository wordDictionaryRepository;
    @Autowired
    public AdminWordController(WordDictionaryRepository wordDictionaryRepository) {
        this.wordDictionaryRepository = wordDictionaryRepository;
    }

    @GetMapping
    public String showWords(Model model){
        List<WordDictionary> words = wordDictionaryRepository.findAll();
        List<String> categories = wordDictionaryRepository.findDistinctCategory();
        model.addAttribute("words", words);
        model.addAttribute("categories", categories);
        model.addAttribute("newWord", new WordDictionary());
        return "admin/words";
    }

    @PostMapping("/add")
    public String addWord(@ModelAttribute WordDictionary newWord, @RequestParam(required = false) String newCategory, RedirectAttributes redirectAttributes){
        if (newWord.getWord() == null || newWord.getWord().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "단어를 입력하세요");
            return "redirect:/admin/words";
        }
        boolean oldCategoryEmpty = (newWord.getCategory() == null || newWord.getCategory().isBlank());
        boolean newCategoryEmpty = (newCategory == null || newCategory.isEmpty());
        if (oldCategoryEmpty && newCategoryEmpty) {
            redirectAttributes.addFlashAttribute("error", "카테고리는 필수입니다");
            return "redirect:/admin/words";
        }
        if (!newCategoryEmpty) {
            newWord.setCategory(newCategory.trim());
        }
        if(wordDictionaryRepository.existsByWord(newWord.getWord().trim())) {
            redirectAttributes.addFlashAttribute("error", "이미 존재하는 단어");
            return "redirect:/admin/words";
        }
        newWord.setUse(true);
        wordDictionaryRepository.save(newWord);

        return "redirect:/admin/words";
    }
}
