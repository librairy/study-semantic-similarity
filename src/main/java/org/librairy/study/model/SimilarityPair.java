package org.librairy.study.model;

import lombok.Data;
import lombok.ToString;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Data
@ToString
public class SimilarityPair {

    String reference;

    String related;

    Double score;
}
