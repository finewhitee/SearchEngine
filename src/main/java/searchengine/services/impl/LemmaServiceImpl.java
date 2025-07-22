package searchengine.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.services.LemmaService;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.tartarus.snowball.ext.PorterStemmer;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
public class LemmaServiceImpl implements LemmaService {
    private final LuceneMorphology ruMorph;
    private final EnglishLuceneMorphology enMorph;
    private final PorterStemmer stemmer;
    private static final Set<String> STOP_POS = Set.of("ПРЕДЛ", "СОЮЗ", "ЧАСТ", "МЕЖД", "СОЮЗ_ПРЕДЛ");

    public LemmaServiceImpl() throws IOException {
        this.ruMorph = new RussianLuceneMorphology();
        this.enMorph = new EnglishLuceneMorphology();
        this.stemmer = new PorterStemmer();
        log.info("LemmaService initialized with Russian and English morphologies");
    }

    @Override
    public Map<String, Integer> extractLemmas(String text) {
        log.debug("extractLemmas called, text length={} chars", text != null ? text.length() : 0);
        if (text == null || text.isBlank()) {
            log.warn("Received empty or null text for lemmatization");
            return Collections.emptyMap();
        }
        // Очищаем от HTML-тегов и приводим к нижнему регистру
        String cleaned = text.replaceAll("<[^>]+>", " ")
                .toLowerCase(Locale.ROOT);
        log.debug("Cleaned text: {}", cleaned);

        // Разбиваем по не-литерам
        String[] tokens = cleaned.split("[^a-zа-яё]+");
        log.debug("Tokenized into {} tokens", tokens.length);

        Map<String, Integer> freq = new HashMap<>();

        for (String token : tokens) {
            if (token.isBlank()) continue;
            log.trace("Processing token='{}'", token);
            String lemma = null;

            // Русское слово
            if (token.chars().allMatch(ch -> Character.UnicodeScript.of(ch) == Character.UnicodeScript.CYRILLIC)) {
                List<String> infos = ruMorph.getMorphInfo(token);
                List<String> forms = infos.stream()
                        .filter(info -> STOP_POS.stream().noneMatch(info::contains))
                        .flatMap(info -> ruMorph.getNormalForms(token).stream())
                        .collect(Collectors.toList());
                if (!forms.isEmpty()) lemma = forms.get(0);
            }
            // Английское слово
            else if (token.chars().allMatch(Character::isLetter)) {
                stemmer.setCurrent(token);
                stemmer.stem();
                lemma = stemmer.getCurrent();
            }

            if (lemma != null && !lemma.isBlank()) {
                freq.merge(lemma, 1, Integer::sum);
                log.trace("Lemma='{}' count={} (token='{}')", lemma, freq.get(lemma), token);
            }
        }

        log.info("extractLemmas result: {} unique lemmas found", freq.size());
        return freq;
    }
}
