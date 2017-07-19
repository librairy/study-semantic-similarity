package org.librairy.study.experiments;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.cbadenes.lab.test.IntegrationTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.librairy.study.Config;
import org.librairy.study.dao.ShapeDao;
import org.librairy.study.dao.SimilarityDao;
import org.librairy.study.model.Corpora;
import org.librairy.study.model.ScoredResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Category(IntegrationTest.class)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Config.class)
public class CorpusManagement {

    @Autowired
    ShapeDao shapeDao;

    @Autowired
    SimilarityDao similarityDao;

    @Test
    public void create() throws IOException {

        String domainUri = "http://librairy.linkeddata.es/resources/domains/group1";

        Corpora corpora = new Corpora();
        corpora.setDocuments(shapeDao.get(domainUri));

        ObjectMapper jsonMapper = new ObjectMapper();
        jsonMapper.writeValue(new File("src/main/resources/corpora.json"), corpora);

        System.out.println(corpora.getDocuments().size());

    }

    @Test
    public void load() throws IOException {

        ObjectMapper jsonMapper = new ObjectMapper();
        Corpora corpora = jsonMapper.readValue(new File("src/main/resources/corpora.json"), Corpora.class);

        System.out.println(corpora.getDocuments().size());



    }

    @Test
    public void getSimilarities(){

        Optional<Double> minScore   = Optional.of(0.9);
        Optional<Double> maxScore   = Optional.of(1.0);
        Optional<Integer> maxNum    = Optional.of(10);
        while(true){
            List<ScoredResource> sims = similarityDao.getSimilarResources(
                    "http://librairy.linkeddata.es/resources/items/2-s2.0-71249089662",
                    "http://librairy.linkeddata.es/resources/domains/group1",
                    Optional.of("item"),
                    minScore,
                    maxNum,
                    maxScore
            );

            sims.forEach(sim -> System.out.println(sim));

            if (sims.size() < maxNum.get()) break;

            maxScore = Optional.of(sims.get(maxNum.get()-1).getScore());

        }



    }

}
