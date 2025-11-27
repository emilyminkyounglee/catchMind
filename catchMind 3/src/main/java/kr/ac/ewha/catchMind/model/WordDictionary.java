package kr.ac.ewha.catchMind.model;


import jakarta.persistence.*;


@Entity
@Table(name = "words")
public class WordDictionary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String word;

    private String category;

    @Column(nullable = false, name = "use_flag")
    private boolean useFlag = true;

    public Long getId() {
        return id;
    }
    public String getWord() {
        return word;
    }
    public void setWord(String word) {
        this.word = word;
    }
    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isUse() {
        return useFlag;
    }
    public void setUse(boolean use) {
        this.useFlag = use;
    }

}
