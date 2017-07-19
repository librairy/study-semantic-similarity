package org.librairy.study.experiments;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.Doubles;
import es.cbadenes.lab.test.IntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.librairy.metrics.similarity.JensenShannonSimilarity;
import org.librairy.study.model.Corpora;
import org.librairy.study.model.DirichletDistribution;
import org.librairy.study.model.SimilarityPair;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Category(IntegrationTest.class)
public class SimilarityAnalysis {


    private List<DirichletDistribution> sample;

    @Before
    public void setup() throws IOException {
        ObjectMapper jsonMapper = new ObjectMapper();
        Corpora corpora = jsonMapper.readValue(new File("src/main/resources/corpora.json"), Corpora.class);


        // Getting distribution of similarities
        this.sample = corpora.getDocuments().stream().limit(1000).collect(Collectors.toList());
    }

    @Test
    public void aggregated() throws IOException {

        StringBuilder out = new StringBuilder();
        out.append("Score").append("\t").append("Num").append("\n");

        DecimalFormat decimalFormat = new DecimalFormat("0.000");

        Map<String, List<String>> similarities = sample.parallelStream().flatMap(d1 -> sample.stream().filter(e -> !e.getId().equals(d1.getId())).map(d2 -> decimalFormat.format(JensenShannonSimilarity.apply(Doubles.toArray(d1.getVector()), Doubles.toArray(d2.getVector()))))).collect(Collectors.groupingBy(String::toString));

        similarities.entrySet().stream().sorted((a,b) -> a.getKey().compareTo(b.getKey())).forEach( entry -> out.append(entry.getKey()).append("\t").append(entry.getValue().size()).append("\n"));

        System.out.println(out.toString());

        FileWriter writer = new FileWriter("results/similarities.txt");
        writer.write(out.toString());
        writer.close();
    }

    @Test
    public void minScore() throws IOException {


        Map<String, List<SimilarityPair>> similarities = sample.parallelStream().flatMap(d1 -> sample.stream().filter(e -> !e.getId().equals(d1.getId())).map(d2 -> {
            SimilarityPair pair = new SimilarityPair();
            pair.setReference(d1.getId());
            pair.setRelated(d2.getId());
            pair.setScore(JensenShannonSimilarity.apply(Doubles.toArray(d1.getVector()), Doubles.toArray(d2.getVector())));
            return pair;
        })).collect(Collectors.groupingBy(SimilarityPair::getReference));


        Double minThreshold = similarities.entrySet().stream().map(entry -> entry.getValue().stream().map(pair -> pair.getScore()).reduce((a, b) -> Math.max(a, b)).get()).reduce((x, y) -> Math.min(x, y)).get();

        System.out.println(minThreshold);


        SimilarityPair pair = similarities.entrySet().stream().map(entry -> entry.getValue().stream().reduce((a, b) -> a.getScore() >= b.getScore() ? a : b).get()).reduce((x, y) -> x.getScore() <= y.getScore() ? x : y).get();

        System.out.println(pair);
    }


}
