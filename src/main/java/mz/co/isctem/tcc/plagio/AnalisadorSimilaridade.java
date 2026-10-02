package mz.co.isctem.tcc.plagio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * RF08: calculo de similaridade textual por TF-IDF combinado com similaridade de cosseno.
 *
 * <p>Abordagem escolhida no levantamento de requisitos por ser adequada ao ambito de uma monografia
 * de licenciatura e por nao implicar custos de licenciamento de servicos externos.
 *
 * <p>O IDF e calculado sobre o conjunto formado pelo documento em analise mais todo o corpus, de
 * modo que termos comuns a muitos trabalhos (metodologia, objectivos, ISCTEM...) percam peso e a
 * semelhanca reflicta sobretudo vocabulario distintivo.
 */
@Component
public class AnalisadorSimilaridade {

    private static final Pattern SEPARADORES = Pattern.compile("[^\\p{L}\\p{Nd}]+");

    /**
     * Palavras vazias do portugues. Sem esta filtragem quaisquer dois textos em portugues
     * apresentariam semelhanca elevada apenas por partilharem artigos e preposicoes.
     */
    private static final Set<String> PALAVRAS_VAZIAS =
            Set.of(
                    "a", "ao", "aos", "as", "com", "como", "da", "das", "de", "dela", "dele", "delas",
                    "deles", "do", "dos", "e", "ela", "elas", "ele", "eles", "em", "entre", "essa", "esse",
                    "esta", "este", "eu", "foi", "foram", "isso", "isto", "ja", "la", "mais", "mas", "me",
                    "mesmo", "meu", "minha", "muito", "na", "nas", "nao", "no", "nos", "num", "numa", "o",
                    "os", "ou", "para", "pela", "pelas", "pelo", "pelos", "por", "qual", "quando", "que",
                    "quem", "se", "sem", "ser", "seu", "sua", "tambem", "te", "tem", "ter", "teu", "tua",
                    "um", "uma", "voce", "vos", "sao", "dum", "duma", "the", "of", "and", "to",
                    "in", "is", "for", "on", "with", "by", "an", "be", "this", "that", "are", "it");

    /** Comprimento minimo de um termo relevante. */
    private static final int MIN_COMPRIMENTO_TERMO = 3;

    /** Documento do corpus, identificado por uma chave opaca para o analisador. */
    public record DocumentoCorpus(Long id, String texto) {}

    /** Resultado da comparacao com um documento do corpus. */
    public record Semelhanca(Long id, double similaridade) {}

    /**
     * Compara o texto submetido com o corpus.
     *
     * @return semelhancas por ordem decrescente; lista vazia se o texto ou o corpus forem
     *     insuficientes.
     */
    public List<Semelhanca> comparar(String texto, List<DocumentoCorpus> corpus, int minTermos) {
        List<String> termosDocumento = tokenizar(texto);
        if (termosDocumento.size() < minTermos || corpus.isEmpty()) {
            return List.of();
        }

        List<List<String>> termosCorpus = corpus.stream().map(d -> tokenizar(d.texto())).toList();

        // Frequencia documental sobre o documento em analise + todo o corpus.
        Map<String, Integer> frequenciaDocumental = new HashMap<>();
        contarPresencas(termosDocumento, frequenciaDocumental);
        termosCorpus.forEach(termos -> contarPresencas(termos, frequenciaDocumental));
        int totalDocumentos = corpus.size() + 1;

        Map<String, Double> vectorDocumento =
                vectorTfIdf(termosDocumento, frequenciaDocumental, totalDocumentos);
        double normaDocumento = norma(vectorDocumento);
        if (normaDocumento == 0) {
            return List.of();
        }

        List<Semelhanca> resultados = new ArrayList<>(corpus.size());
        for (int i = 0; i < corpus.size(); i++) {
            List<String> termos = termosCorpus.get(i);
            if (termos.size() < minTermos) {
                continue;
            }
            Map<String, Double> vector = vectorTfIdf(termos, frequenciaDocumental, totalDocumentos);
            double similaridade = cosseno(vectorDocumento, normaDocumento, vector);
            if (similaridade > 0) {
                resultados.add(new Semelhanca(corpus.get(i).id(), similaridade));
            }
        }
        resultados.sort((a, b) -> Double.compare(b.similaridade(), a.similaridade()));
        return resultados;
    }

    /** Divide o texto em termos normalizados, sem acentos, numeros isolados nem palavras vazias. */
    List<String> tokenizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        String semAcentos =
                java.text.Normalizer.normalize(texto.toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "");
        List<String> termos = new ArrayList<>();
        for (String bruto : SEPARADORES.split(semAcentos)) {
            if (bruto.length() >= MIN_COMPRIMENTO_TERMO
                    && !PALAVRAS_VAZIAS.contains(bruto)
                    && !bruto.chars().allMatch(Character::isDigit)) {
                termos.add(bruto);
            }
        }
        return termos;
    }

    private void contarPresencas(List<String> termos, Map<String, Integer> frequenciaDocumental) {
        new HashSet<>(termos).forEach(termo -> frequenciaDocumental.merge(termo, 1, Integer::sum));
    }

    /** TF logaritmico x IDF suavizado. */
    private Map<String, Double> vectorTfIdf(
            List<String> termos, Map<String, Integer> frequenciaDocumental, int totalDocumentos) {
        Map<String, Integer> frequencias = new HashMap<>();
        termos.forEach(termo -> frequencias.merge(termo, 1, Integer::sum));

        Map<String, Double> vector = new HashMap<>(frequencias.size());
        frequencias.forEach(
                (termo, frequencia) -> {
                    double tf = 1 + Math.log(frequencia);
                    int df = frequenciaDocumental.getOrDefault(termo, 1);
                    double idf = Math.log((double) (totalDocumentos + 1) / (df + 1)) + 1;
                    vector.put(termo, tf * idf);
                });
        return vector;
    }

    private double norma(Map<String, Double> vector) {
        double soma = 0;
        for (double peso : vector.values()) {
            soma += peso * peso;
        }
        return Math.sqrt(soma);
    }

    private double cosseno(Map<String, Double> a, double normaA, Map<String, Double> b) {
        double normaB = norma(b);
        if (normaA == 0 || normaB == 0) {
            return 0;
        }
        // Percorre o vector mais curto: o produto interno so depende dos termos comuns.
        Map<String, Double> menor = a.size() <= b.size() ? a : b;
        Map<String, Double> maior = menor == a ? b : a;

        double produto = 0;
        for (Map.Entry<String, Double> entrada : menor.entrySet()) {
            Double outro = maior.get(entrada.getKey());
            if (outro != null) {
                produto += entrada.getValue() * outro;
            }
        }
        return Math.min(1.0, produto / (normaA * normaB));
    }
}
