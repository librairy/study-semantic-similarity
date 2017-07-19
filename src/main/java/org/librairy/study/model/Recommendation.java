package org.librairy.study.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Data
public class Recommendation {

    String reference;

    List<String> related = new ArrayList<>();

    public Recommendation addRelated(String element){
        this.related.add(element);
        return this;
    }

    public Recommendation addRelatedAll(List<String> elements){
        this.related.addAll(elements);
        return this;
    }
}
