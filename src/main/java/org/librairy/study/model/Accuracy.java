package org.librairy.study.model;

import lombok.Data;
import lombok.ToString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Data
@ToString
public class Accuracy {

    private final String id;

    private final double precision;
    private final double recall;
    private final double fmeasure;

    long tp = 0;

    long tn = 0;

    long fp = 0;

    long fn = 0;


    public Accuracy(String id, Result result, Result goldStandard){

        this.id = id;


        Map<String,List<String>> goldMap    = new HashMap<>();
        goldStandard.getRecommendationList().forEach(rec -> goldMap.put(rec.getReference(),rec.getRelated()));

        List<String> all = goldMap.entrySet().stream().map(entry -> entry.getKey()).collect(Collectors.toList());

        for (Recommendation recommendation : result.getRecommendationList()){

            List<String> goldList = goldMap.get(recommendation.getReference());

            tp += recommendation.getRelated().stream().filter(goldList::contains).count();

            tn += all.stream().filter(el -> !goldList.contains(el)).filter(el -> !recommendation.getRelated().contains(el)).count();

            fp += recommendation.getRelated().stream().filter(rel -> !goldList.contains(rel)).count();

            fn += all.stream().filter(el -> !recommendation.getRelated().contains(el)).filter(el -> goldList.contains(el)).count();

        }

        precision   = (Double.valueOf(tp) + Double.valueOf(fp)) == 0.0 ? 0.0 : Double.valueOf(tp) / (Double.valueOf(tp) + Double.valueOf(fp));

        recall      = (Double.valueOf(tp) + Double.valueOf(fn)) == 0.0? 0.0 : Double.valueOf(tp) / (Double.valueOf(tp) + Double.valueOf(fn));

        fmeasure    = (precision != 0.0 && recall != 0.0)? 2 * (getPrecision()*getRecall())/(getPrecision()+getRecall()) : 0.0;
    }

}
