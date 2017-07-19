package org.librairy.study.model;

import lombok.Data;

import java.util.List;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Data
public class Corpora {

    List<DirichletDistribution> documents;

}
