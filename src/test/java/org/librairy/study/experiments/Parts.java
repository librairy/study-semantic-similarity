package org.librairy.study.experiments;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.Doubles;
import es.cbadenes.lab.test.IntegrationTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.librairy.boot.model.domain.resources.Part;
import org.librairy.boot.storage.dao.ItemsDao;
import org.librairy.boot.storage.dao.PartsDao;
import org.librairy.metrics.similarity.JensenShannonSimilarity;
import org.librairy.study.Config;
import org.librairy.study.model.Corpora;
import org.librairy.study.model.DirichletDistribution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Category(IntegrationTest.class)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Config.class)
public class Parts {


    @Autowired
    ItemsDao itemsDao;

    @Autowired
    PartsDao partsDao;

    @Test
    public void count() throws IOException {

        ObjectMapper jsonMapper = new ObjectMapper();
        Corpora corpora = jsonMapper.readValue(new File("src/main/resources/corpora.json"), Corpora.class);

        Map<String,Integer> counter = new HashMap<>();

        corpora.getDocuments().forEach(doc -> itemsDao.listParts(doc.getId(), 100, Optional.empty(), false).stream().map(part -> partsDao.get(part.getUri(),false).get().asPart().getSense()).forEach(key -> counter.put(key, counter.containsKey(key)? counter.get(key)+1 : 1)));

        FileWriter writer = new FileWriter("results/parts.txt");
        writer.write(counter.entrySet().stream().sorted((a, b) -> a.getKey().compareTo(b.getKey())).map(entry -> entry.getKey()).collect(Collectors.joining("\t")));
        writer.write("\n");
        writer.write(counter.entrySet().stream().sorted((a, b) -> a.getKey().compareTo(b.getKey())).map(entry -> String.valueOf(entry.getValue())).collect(Collectors.joining("\t")));
        writer.close();
    }

    @Test
    public void withAllParts() throws IOException {

        ObjectMapper jsonMapper = new ObjectMapper();
        Corpora corpora = jsonMapper.readValue(new File("src/main/resources/corpora.json"), Corpora.class);

        Map<String,Integer> counter = new HashMap<>();

        long number = corpora.getDocuments().parallelStream().filter(doc -> itemsDao.listParts(doc.getId(), 100, Optional.empty(), false).size() == 6).count();
        System.out.println(number);
    }


}
