package org.librairy.study.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Data
public class Result {

    List<Recommendation> recommendationList = new ArrayList<>();


    public Result addRecommendation(Recommendation recommendation){
        this.recommendationList.add(recommendation);
        return this;
    }

}
