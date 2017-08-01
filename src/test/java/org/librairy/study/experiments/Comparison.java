package org.librairy.study.experiments;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import es.cbadenes.lab.test.IntegrationTest;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.librairy.boot.model.domain.resources.Item;
import org.librairy.boot.model.domain.resources.Part;
import org.librairy.boot.storage.dao.ItemsDao;
import org.librairy.boot.storage.dao.PartsDao;
import org.librairy.study.Config;
import org.librairy.study.dao.ItemCache;
import org.librairy.study.dao.PartCache;
import org.librairy.study.dao.ShapeDao;
import org.librairy.study.dao.SimilarityDao;
import org.librairy.study.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Category(IntegrationTest.class)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Config.class)
public class Comparison {

    private static final String domainUri = "http://librairy.linkeddata.es/resources/domains/group1";

//    private static final Double threshold = 0.99;

    @Autowired
    ItemsDao itemsDao;

    @Autowired
    ItemCache itemCache;

    @Autowired
    PartsDao partsDao;

    @Autowired
    PartCache cache;

    @Autowired
    SimilarityDao similarityDao;

    @Autowired
    ShapeDao shapeDao;

    @Test
    public void accuracy() throws IOException {


        FileWriter precisionWriter = new FileWriter("results/precision.txt");
        FileWriter recallWriter = new FileWriter("results/recall.txt");
        FileWriter fmeasureWriter = new FileWriter("results/fmeasure.txt");

        boolean addedHeader = false;

        for (Double threshold : Arrays.asList(new Double[]{0.5,0.6,0.7,0.8,0.9,0.95,0.99})){
            ObjectMapper jsonMapper = new ObjectMapper();
            String suffix = StringUtils.replace(String.valueOf(threshold),".","_");
            Result goldStandard = jsonMapper.readValue(new File("src/main/resources/goldStandard"+suffix+".json"), Result.class);

            int size = goldStandard.getRecommendationList().size();

            Map<String,Result> results = new ConcurrentHashMap<>();

            AtomicInteger counter = new AtomicInteger();
            goldStandard.getRecommendationList().parallelStream().forEach(doc -> {

                System.out.println(counter.getAndIncrement() + "/" + size);

                List<Part> parts = itemsDao.listParts(doc.getReference(), 10, Optional.empty(), false).stream().map(part -> cache.get(part.getUri())).collect(Collectors.toList());

                parts.parallelStream().forEach(part -> {

                    System.out.println("Similarities from '"+part.getSense()+"' of " + doc.getReference());

                    String partType = cache.get(part.getUri()).getSense();

                    Result partResult = results.get(part.getSense());

                    if (partResult == null) partResult = new Result();

                    Recommendation recommendation = new Recommendation();
                    recommendation.setReference(doc.getReference());


                    Optional<Double> minScore   = Optional.of(threshold);
                    Optional<Double> maxScore   = Optional.of(1.0);
                    Optional<Integer> maxNum    = Optional.of(200);
                    while(true){
                        List<ScoredResource> simParts = similarityDao.getSimilarResources(
                                part.getUri(),
                                domainUri,
                                Optional.of("part"),
                                minScore,
                                maxNum,
                                maxScore);

//                            simParts.forEach(sim -> System.out.println(sim));

                        List<Part> validParts = simParts.stream().map(sr -> cache.get(sr.getUri())).filter(simPart -> simPart.getSense().equalsIgnoreCase(partType)).collect(Collectors.toList());

                        recommendation.addRelatedAll(validParts.stream().flatMap(sr -> itemCache.get(sr.getUri()).stream().map(item -> item.getUri())).collect(Collectors.toList()));


                        if (simParts.size() < maxNum.get()) break;

                        maxScore = Optional.of(simParts.get(maxNum.get()-1).getScore());

                    }

                    partResult.addRecommendation(recommendation);

                    results.put(part.getSense(),partResult);

                });




            });


            List<Accuracy> accuracyResults = results.entrySet().stream().map(entry -> new Accuracy(entry.getKey(), entry.getValue(), goldStandard)).collect(Collectors.toList());

            System.out.println("Threshold: " + threshold);
            accuracyResults.forEach(res -> System.out.println(res));

            if(!addedHeader){
                String header = accuracyResults.stream().map(acc-> acc.getId()).sorted((a,b) -> a.compareTo(b)).collect(Collectors.joining("\t"));
                precisionWriter.write("threshold\t"+header+"\n");
                recallWriter.write("threshold\t"+header+"\n");
                fmeasureWriter.write("threshold\t"+header+"\n");
                addedHeader=true;
            }


            String precisionResults = String.valueOf(threshold) + "\t" + accuracyResults.stream().sorted((a, b) -> a.getId().compareTo(b.getId())).map(a -> String.valueOf(a.getPrecision())).collect(Collectors.joining("\t"));
            precisionWriter.write(precisionResults+"\n");

            String recallResults = String.valueOf(threshold) + "\t" + accuracyResults.stream().sorted((a, b) -> a.getId().compareTo(b.getId())).map(a -> String.valueOf(a.getRecall())).collect(Collectors.joining("\t"));
            recallWriter.write(recallResults+"\n");

            String fmeasureResults = String.valueOf(threshold) + "\t" + accuracyResults.stream().sorted((a, b) -> a.getId().compareTo(b.getId())).map(a -> String.valueOf(a.getFmeasure())).collect(Collectors.joining("\t"));
            fmeasureWriter.write(fmeasureResults+"\n");

//            createReport("precision", accuracyResults, accuracy -> String.valueOf(accuracy.getPrecision()));
//            createReport("recall", accuracyResults, accuracy -> String.valueOf(accuracy.getRecall()));
//            createReport("fmeasure", accuracyResults, accuracy -> String.valueOf(accuracy.getFmeasure()));
        }

        precisionWriter.close();
        recallWriter.close();
        fmeasureWriter.close();

    }

    @Test
    public void size() throws IOException {

        ObjectMapper jsonMapper = new ObjectMapper();
        Result corpora = jsonMapper.readValue(new File("src/main/resources/goldStandard.json"), Result.class);

        int size = corpora.getRecommendationList().size();

        Map<String,List<Integer>> sizes = new ConcurrentHashMap<>();

        AtomicInteger counter = new AtomicInteger();
        corpora.getRecommendationList().stream().forEach(doc -> {

            System.out.println(counter.getAndIncrement() + "/" + size);

            List<Part> parts = itemsDao.listParts(doc.getReference(), 100, Optional.empty(), false).stream().map(part -> partsDao.get(part.getUri(), false).get().asPart()).collect(Collectors.toList());

            parts.parallelStream().forEach(part -> {

                Part partContent = partsDao.get(part.getUri(), true).get().asPart();

                List<Integer> partSizes = sizes.get(part.getSense());

                if (partSizes == null) partSizes = new ArrayList<Integer>();

                // fixed string as 'empty' or similar
                if (!Strings.isNullOrEmpty(partContent.getContent()) && partContent.getContent().length() > 25) {
                    partSizes.add(partContent.getContent().length());
                    sizes.put(part.getSense(), partSizes);
                }

            });

            Item item = itemsDao.get(doc.getReference(), true).get().asItem();

            List<Integer> itemSizes = sizes.get("full-text");

            if (itemSizes == null) itemSizes = new ArrayList<Integer>();

            itemSizes.add(Strings.isNullOrEmpty(item.getContent())? 0 : item.getContent().length());
            sizes.put("full-text",itemSizes);

        });

        for (Map.Entry<String,List<Integer>> entry : sizes.entrySet()){

            FileWriter writer = new FileWriter("results/sizeOf"+entry.getKey()+".txt");
            writer.write(entry.getKey());
            writer.write("\n");
            for(Integer length : entry.getValue()){
                writer.write(String.valueOf(length));
                writer.write("\n");
            }
            writer.close();
        }

    }

    @Test
    public void internalRepresentativeness() throws IOException {

        ObjectMapper jsonMapper = new ObjectMapper();
        Corpora corpora = jsonMapper.readValue(new File("src/main/resources/corpora.json"), Corpora.class);

        int size = corpora.getDocuments().size();

        Map<String,List<Double>> similarities = new ConcurrentHashMap<>();

        AtomicInteger counter = new AtomicInteger();
        corpora.getDocuments().parallelStream().forEach(doc -> {

            System.out.println(counter.getAndIncrement() + "/" + size);

            List<Part> parts = itemsDao.listParts(doc.getId(), 100, Optional.empty(), false).stream().map(part -> partsDao.get(part.getUri(), false).get().asPart()).collect(Collectors.toList());

            parts.stream().forEach(part -> {

                DirichletDistribution partDistribution = new DirichletDistribution();
                partDistribution.setId(part.getSense());
                partDistribution.setVector(shapeDao.get(domainUri,part.getUri()));


                List<Double> partSimilarities = similarities.get(part.getSense());

                if (partSimilarities == null) partSimilarities = new ArrayList<Double>();

                Double similarity = partDistribution.similarTo(doc);

                if (similarity > 0.0){
                    partSimilarities.add(partDistribution.similarTo(doc));
                    similarities.put(part.getSense(), partSimilarities);
                }


            });

        });

        for (Map.Entry<String,List<Double>> entry : similarities.entrySet()){

            FileWriter writer = new FileWriter("results/similaritiesFrom"+entry.getKey()+".txt");
            writer.write(entry.getKey());
            writer.write("\n");
            for(Double similarity: entry.getValue()){
                writer.write(String.valueOf(similarity));
                writer.write("\n");
            }
            writer.close();
        }

    }


}

