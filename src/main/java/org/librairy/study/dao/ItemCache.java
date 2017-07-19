package org.librairy.study.dao;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.librairy.boot.model.domain.resources.Item;
import org.librairy.boot.model.domain.resources.Part;
import org.librairy.boot.storage.dao.PartsDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Component
public class ItemCache {


    @Autowired
    PartsDao partsDao;

    private LoadingCache<String, List<Item>> cache;

    @PostConstruct
    public void setup() {
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(10000)
//                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build(
                        new CacheLoader<String, List<Item>>() {
                            public List<Item> load(String partUri) {
                                return partsDao.listItems(partUri, 10, Optional.empty(), false);
                            }
                        });
    }


    public List<Item> get(String uri) {
        try {
            return this.cache.get(uri);
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }
}