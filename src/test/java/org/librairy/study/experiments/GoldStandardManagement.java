package org.librairy.study.experiments;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.Doubles;
import es.cbadenes.lab.test.IntegrationTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.librairy.metrics.similarity.JensenShannonSimilarity;
import org.librairy.study.Config;
import org.librairy.study.model.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Category(IntegrationTest.class)
public class GoldStandardManagement {

    private Double threshold = 0.99;

    @Test
    public void multipleThreshold(){

        List<Double> thresholds = Arrays.asList(new Double[]{0.5,0.6,0.7,0.8,0.9,0.95,0.99});

        thresholds.forEach(th -> {
            threshold = th;
            try {
                create();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });


    }

    @Test
    public void create() throws IOException {

        ObjectMapper jsonMapper = new ObjectMapper();

        Corpora corpora = jsonMapper.readValue(new File("src/main/resources/corpora.json"), Corpora.class);

        List<DirichletDistribution> sample = corpora.getDocuments().stream().limit(1000).collect(Collectors.toList());


        Map<String, List<SimilarityPair>> similarities = sample
                .parallelStream()
                .flatMap(d1 -> sample.stream().filter(e -> !e.getId().equals(d1.getId())).map(d2 ->
                {
                    SimilarityPair pair = new SimilarityPair();
                    pair.setReference(d1.getId());
                    pair.setRelated(d2.getId());
                    pair.setScore(JensenShannonSimilarity.apply(Doubles.toArray(d1.getVector()), Doubles.toArray(d2.getVector())));
                    return pair;
                }))
                .filter(pair -> pair.getScore() > threshold)
                .collect(Collectors.groupingBy(SimilarityPair::getReference));



        Result result = new Result();


        similarities.entrySet().forEach( entry -> {
            Recommendation recommendation = new Recommendation();
            recommendation.setReference(entry.getKey());
            recommendation.setRelated(entry.getValue().stream().map(p -> p.getRelated()).collect(Collectors.toList()));
            result.addRecommendation(recommendation);
        });


        String suffix = StringUtils.replace(String.valueOf(threshold),".","_");
        jsonMapper.writeValue(new File("src/main/resources/goldStandard"+suffix+".json"),result);
        System.out.println("GoldStandard created!");

    }

    @Test
    public void load() throws IOException {

        ObjectMapper jsonMapper = new ObjectMapper();
        Result result = jsonMapper.readValue(new File("src/main/resources/goldStandard.json"), Result.class);

        System.out.println(result.getRecommendationList().size());



    }

}
