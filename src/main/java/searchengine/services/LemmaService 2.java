package searchengine.services;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Сервис для извлечения лемм из текста на русском и английском языках.
 */
@Service
public interface LemmaService {
    /**
     * Извлекает из переданного текста карту лемм и их частот.
     * @param text входной текст (может содержать HTML-теги)
     * @return Map<лемма, количество вхождений>
     */
    Map<String, Integer> extractLemmas(String text);
}
