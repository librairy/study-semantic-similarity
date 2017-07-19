package org.librairy.study.dao;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.exceptions.InvalidQueryException;
import org.librairy.boot.storage.dao.DBSessionManager;
import org.librairy.boot.storage.generator.URIGenerator;
import org.librairy.study.model.ScoredResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Component
public class SimilarityDao {

    private static final Logger LOG = LoggerFactory.getLogger(SimilarityDao.class);

    @Autowired
    DBSessionManager sessionManager;

    public List<ScoredResource> getSimilarResources(String resourceUri, String domainUri, Optional<String> type, Optional<Double> minScore, Optional<Integer> limit, Optional<Double> maxScore){

        StringBuilder queryBuilder = new StringBuilder().append("select ")
                .append("resource_uri_2").append(", ").append("score").append(", ").append("date")
                .append(" from ").append("similarities")
                .append(" where ").append("resource_uri_1").append("='").append(resourceUri).append("' ");



        if (type.isPresent()){
            queryBuilder = queryBuilder.append(" and resource_type_2='").append(type.get()).append("' ");
        }


        if (minScore.isPresent()){
            queryBuilder = queryBuilder.append(" and score >=").append(minScore.get()).append(" ");
        }

        if (maxScore.isPresent()){
            queryBuilder = queryBuilder.append(" and score <").append(maxScore.get()).append(" ");
        }

        if (limit.isPresent()){
            queryBuilder = queryBuilder.append(" limit ").append(limit.get()).append(" ");
        }


        String query = queryBuilder.toString();

        try{
            ResultSet result = sessionManager.getSpecificSession("lda", URIGenerator.retrieveId(domainUri)).execute(query);
            List<Row> rows = result.all();

            if (rows == null || rows.isEmpty()) return Collections.emptyList();

            return rows
                    .stream()
                    .map(row -> new ScoredResource(row.getString(0), URIGenerator.typeFrom(row.getString(0)).key(),row.getDouble(1), row.getString(2)))
                    .collect(Collectors.toList());
        }catch (InvalidQueryException e){
            LOG.warn("Query error: " + e.getMessage());
            return Collections.emptyList();
        }catch (Exception e){
            LOG.error("Unexpected error", e);
            return Collections.emptyList();
        }


    }
}
