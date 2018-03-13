/*
  $Id: $
  @file ContainerRepoDb.java
  @brief Contains the ContainerRepoDb.java class

  @author Rahul Singh [rsingh]
  Copyright (c) 2013, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa.db;

import com.distelli.europa.models.ContainerRepo;
import com.distelli.europa.models.RegistryProvider;
import com.distelli.europa.models.RepoEvent;
import com.distelli.europa.registry.ContainerRepoNotFoundException;
import com.distelli.jackson.transform.TransformModule;
import com.distelli.persistence.AttrType;
import com.distelli.persistence.ConvertMarker;
import com.distelli.persistence.Index;
import com.distelli.persistence.IndexDescription;
import com.distelli.persistence.IndexType;
import com.distelli.persistence.PageIterator;
import com.distelli.persistence.TableDescription;
import com.distelli.utils.CompositeKey;
import com.distelli.webserver.AjaxClientException;
import com.distelli.webserver.JsonError;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Singleton;
import lombok.extern.log4j.Log4j;

import javax.inject.Inject;
import javax.persistence.RollbackException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Log4j
@Singleton
public class ContainerRepoDb extends BaseDb
{
    private Index<ContainerRepo> _main;
    private Index<ContainerRepo> _secondaryIndex;
    private Index<ContainerRepo> _byCredId;

    private final ObjectMapper _om = new ObjectMapper();

    private static final Pattern REPO_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_.-]+");

    public static TableDescription getTableDescription() {
        return TableDescription.builder()
            .tableName("repos")
            .indexes(
                Arrays.asList(
                    IndexDescription.builder()
                    .hashKey(attr("hk", AttrType.STR))
                    .rangeKey(attr("id", AttrType.STR))
                    .indexType(IndexType.MAIN_INDEX)
                    .readCapacity(1L)
                    .writeCapacity(1L)
                    .build(),
                    IndexDescription.builder()
                    .indexName("hk-sidx-index")
                    .hashKey(attr("hk", AttrType.STR))
                    .rangeKey(attr("sidx", AttrType.STR))
                    .indexType(IndexType.GLOBAL_SECONDARY_INDEX)
                    .readCapacity(1L)
                    .writeCapacity(1L)
                    .build(),
                    IndexDescription.builder()
                    .indexName("hk-cid-index")
                    .hashKey(attr("hk", AttrType.STR))
                    .rangeKey(attr("cid", AttrType.STR))
                    .indexType(IndexType.GLOBAL_SECONDARY_INDEX)
                    .readCapacity(1L)
                    .writeCapacity(1L)
                    .build()))
            .build();
    }

    private TransformModule createTransforms(TransformModule module) {
        module.createTransform(ContainerRepo.class)
        .put("hk", String.class,
             (item) -> getHashKey(item),
             (item, domain) -> setHashKey(item, domain))
        .put("id", String.class,
             (item) -> item.getId().toLowerCase(),
             (item, id) -> item.setId(id.toLowerCase()))
        .put("sidx", String.class,
             (item) -> getSecondaryKey(item.getProvider(), item.getRegion(), item.getName()))
        .put("prov", RegistryProvider.class, "provider")
        .put("region", String.class, "region")
        .put("name", String.class, "name")
        .put("rid", String.class, "registryId")
        .put("cid", String.class, "credId")
        .put("pr", Boolean.class, "publicRepo")
        .put("oid", String.class, "overviewId")
        .put("endpt", String.class, "endpoint")
        .put("lr", Boolean.class, "local")
        .put("mr", Boolean.class, "mirror")
        .put("lst", Long.class, "lastSyncTime")
        .put("syc", Long.class, "syncCount")
        .put("sdcrid", new TypeReference<Set<String>>(){}, "syncDestinationContainerRepoIds")
        .put("levent", RepoEvent.class, "lastEvent");
        return module;
    }

    private final String getHashKey(ContainerRepo repo)
    {
        return getHashKey(repo.getDomain());
    }

    private final String getHashKey(String domain)
    {
        return domain.toLowerCase();
    }

    private final void setHashKey(ContainerRepo repo, String domain)
    {
        repo.setDomain(domain);
    }

    private final String getSecondaryKey(RegistryProvider provider, String region, String name)
    {
        return CompositeKey.build(provider.toString().toLowerCase(),
                                  region.toLowerCase(),
                                  name.toLowerCase());
    }

    @Inject
    protected ContainerRepoDb(Index.Factory indexFactory,
                              ConvertMarker.Factory convertMarkerFactory) {
        _om.registerModule(createTransforms(new TransformModule()));
        _om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        _main = indexFactory.create(ContainerRepo.class)
        .withTableName("repos")
            .withNoEncrypt("hk", "id", "sidx", "cid", "syc")
        .withHashKeyName("hk")
        .withRangeKeyName("id")
        .withConvertValue(_om::convertValue)
        .withConvertMarker(convertMarkerFactory.create("hk", "id"))
        .build();

        _secondaryIndex = indexFactory.create(ContainerRepo.class)
        .withIndexName("repos", "hk-sidx-index")
        .withNoEncrypt("hk", "id", "sidx", "cid")
        .withHashKeyName("hk")
        .withRangeKeyName("sidx")
        .withConvertValue(_om::convertValue)
        .withConvertMarker(convertMarkerFactory.create("hk", "sidx", "id"))
        .build();

        _byCredId = indexFactory.create(ContainerRepo.class)
        .withIndexName("repos", "hk-cid-index")
        .withNoEncrypt("hk", "id", "sidx", "cid")
        .withHashKeyName("hk")
        .withRangeKeyName("cid")
        .withConvertValue(_om::convertValue)
        .withConvertMarker(convertMarkerFactory.create("hk", "cid", "id"))
        .build();
    }

    public void save(ContainerRepo repo)
    {
        String region = repo.getRegion();
        if(region == null)
            throw(new AjaxClientException("Invalid Region "+region+" in Container Repo", JsonError.Codes.BadContent, 400));
        String name = repo.getName();
        if(name == null || name.trim().isEmpty())
            throw(new AjaxClientException("Invalid Name "+name+" in Container Repo", JsonError.Codes.BadContent, 400));
        String id = repo.getId();
        if(id == null)
            throw(new IllegalArgumentException("Invalid id "+id+" in container repo"));
        if(repo.getDomain() == null)
            throw(new IllegalArgumentException("Invalid null domain for ContainerRepo: "+repo));
        _main.putItem(repo);
    }

    public void deleteRepo(String domain, String id)
    {
        _main.deleteItem(getHashKey(domain),
                         id.toLowerCase());
    }

    public List<ContainerRepo> listRepos(PageIterator pageIterator)
    {
        return _main.scanItems(pageIterator);
    }

    public List<ContainerRepo> listRepos(String domain, PageIterator pageIterator)
    {
        return _main.queryItems(getHashKey(domain), pageIterator).list();
    }

    public List<ContainerRepo> listRepos(String domain,
                                         RegistryProvider provider,
                                         PageIterator pageIterator)
    {
        String rangeKey = CompositeKey.buildPrefix(provider.toString().toLowerCase());
        return _secondaryIndex.queryItems(getHashKey(domain), pageIterator)
        .beginsWith(rangeKey)
        .list();
    }

    public List<ContainerRepo> listEuropaRepos(String domain,
                                               PageIterator pageIterator)
    {
        String rangeKey = CompositeKey.buildPrefix(RegistryProvider.EUROPA.toString().toLowerCase());
        return _secondaryIndex.queryItems(getHashKey(domain), pageIterator)
            .beginsWith(rangeKey)
            .list();
    }

    public List<ContainerRepo> listRepos(String domain,
                                         RegistryProvider provider,
                                         String region,
                                         PageIterator pageIterator)
    {
        String rangeKey = CompositeKey.buildPrefix(provider.toString().toLowerCase(),
                                             region.toLowerCase());
        return _main.queryItems(getHashKey(domain), pageIterator)
        .beginsWith(rangeKey)
        .list();
    }

    public ContainerRepo getRepo(String domain,
                                 RegistryProvider provider,
                                 String region,
                                 String name)
    {
        List<ContainerRepo> repos =
            _secondaryIndex.queryItems(getHashKey(domain), new PageIterator().pageSize(1))
            .eq(getSecondaryKey(provider, region, name))
            .list();
        if ( repos.size() < 1 ) return null;
        return repos.get(0);
    }

    public ContainerRepo getLocalRepo(String domain,
                                      String name)
    {
        for(PageIterator iter : new PageIterator().pageSize(1000))
        {
            List<ContainerRepo> repos = _secondaryIndex.queryItems(getHashKey(domain), iter)
            .eq(getSecondaryKey(RegistryProvider.EUROPA, "", name))
            .list();

            for(ContainerRepo repo : repos)
            {
                if(repo.isLocal())
                    return repo;
            }
        }

        return null;
    }

    public boolean repoExists(String domain,
                              RegistryProvider provider,
                              String region,
                              String name)
    {
        return null != getRepo(domain, provider, region, name);
    }

    public ContainerRepo getRepo(String domain, String id)
    {
        return _main.getItem(getHashKey(domain),
                             id.toLowerCase());
    }

    public void setLastEvent(String domain, String id, RepoEvent lastEvent)
    {
        _main.updateItem(getHashKey(domain),
                         id.toLowerCase())
        .set("levent", lastEvent)
        .when((expr) -> expr.eq("id", id.toLowerCase()));
    }

    public void setRepoPublic(String domain, String id)
    {
        _main.updateItem(getHashKey(domain),
                         id.toLowerCase())
        .set("pr", true)
        .when((expr) -> expr.eq("id", id.toLowerCase()));
    }

    public void setRepoPrivate(String domain, String id)
    {
        _main.updateItem(getHashKey(domain),
                         id.toLowerCase())
        .set("pr", false)
        .when((expr) -> expr.eq("id", id.toLowerCase()));
    }

    public void setLastSyncTime(String domain, String id, long lastSyncTime)
    {
        _main.updateItem(getHashKey(domain),
                         id.toLowerCase())
        .set("lst", lastSyncTime)
        .when((expr) -> expr.eq("id", id.toLowerCase()));
    }

    public void addSyncDestinationContainerRepoId(String domain, String id, String destinationRepoId) {
        try {
            _main.updateItem(getHashKey(domain),
                             id.toLowerCase())
                .setAdd("sdcrid", destinationRepoId)
                .when((expr) -> expr.eq("id", id.toLowerCase()));
        } catch (RollbackException e) {
            throw new ContainerRepoNotFoundException(domain, null, id, e);
        }
    }

    public void removeSyncDestinationContainerRepoId(String domain, String id, String destinationRepoId) {
        try {
            _main.updateItem(getHashKey(domain),
                             id.toLowerCase())
                .setRemove("sdcrid", destinationRepoId)
                .when((expr) -> expr.eq("id", id.toLowerCase()));
        } catch (RollbackException e) {
            throw new ContainerRepoNotFoundException(domain, null, id, e);
        }
    }

    public List<ContainerRepo> listReposByCred(String domain, String credId, PageIterator pageIterator)
    {
        return _byCredId.queryItems(getHashKey(domain),
                                    pageIterator)
        .eq(credId.toLowerCase())
        .list();
    }

    // Returns true if count was incremented.
    public boolean incrementSyncCount(String domain, String id, long currentCount) {
        try {
            _main.updateItem(getHashKey(domain), id.toLowerCase())
                .set("syc", currentCount+1)
                .when((expr) -> {
                        if ( 0 == currentCount ) {
                            return expr.or(expr.eq("syc", 0),
                                    expr.and(expr.exists("id"), expr.not(expr.exists("syc"))));
                        } else {
                            return expr.eq("syc", currentCount);
                        }
                    });
            return true;
        } catch ( RollbackException ex ) {
            return false;
        }
    }

    public String getMainIndexMarker(ContainerRepo repo, boolean hasHashKey) {
        return _main.toMarker(repo, hasHashKey);
    }

    public String getSecondaryIndexMarker(ContainerRepo repo, boolean hasHashKey) {
        return _secondaryIndex.toMarker(repo, hasHashKey);
    }

    /**
     * Check whether repository names are valid and available.
     *
     * @param domain the domain to check names under
     * @param repoNames the names to check
     * @return a Map between name and validity
     */
    public Map<String, RepoNameValidity> validateLocalNames(String domain, Collection<String> repoNames) {
        if (null == domain) {
            throw new NullPointerException("Domain cannot be null");
        }
        if (null == repoNames) {
            throw new NullPointerException("Why would you ever pass a null value for a List?");
        }
        Map<String, RepoNameValidity> retval = new HashMap<>();
        for (String name : repoNames) {
            if (!isRepoNameValidFormat(name)) {
                retval.put(name, RepoNameValidity.INVALID);
            } else if (repoExists(domain, RegistryProvider.EUROPA, "", name)) {
                retval.put(name, RepoNameValidity.EXISTS);
            } else {
                retval.put(name, RepoNameValidity.VALID);
            }
        }
        return retval;
    }

    /**
     * Check if a repository name is in the valid format.
     *
     * A repository name is considered valid if it matches the regular
     * expression {@code [a-zA-Z0-9_.-]+}.
     *
     * @param repoName the name to check
     * @return true if the name is valid, false if it is not
     */
    public boolean isRepoNameValidFormat(String repoName) {
        return null != repoName && REPO_NAME_PATTERN.matcher(repoName).matches();
    }

    /**
     * Represents the validity of a repository name.
     */
    public enum RepoNameValidity {
        /**
         * The name is valid and available.
         */
        VALID,
        /**
         * The name is not valid.
         */
        INVALID,
        /**
         * A repository with the name already exists.
         */
        EXISTS;
    }
}
