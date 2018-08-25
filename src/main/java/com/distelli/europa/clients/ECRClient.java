/*
  $Id: $
  @file ECRClient.java
  @brief Contains the ECRClient.java class

  @author Rahul Singh [rsingh]
  Copyright (c) 2013, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa.clients;

import java.util.*;
import com.amazonaws.regions.*;
import com.amazonaws.services.ecr.*;
import com.amazonaws.auth.*;
import com.distelli.persistence.PageIterator;
import com.amazonaws.services.ecr.model.*;
import com.distelli.europa.models.*;
import lombok.extern.log4j.Log4j;
import java.net.URI;

@Log4j
public class ECRClient
{
    private RegistryCred _registryCred;
    private AmazonECRClient _awsEcrClient;

    public ECRClient(RegistryCred registryCred)
    {
        _registryCred = registryCred;
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(registryCred.getKey(),
                                                               registryCred.getSecret());
        _awsEcrClient = new AmazonECRClient(awsCreds);
        Region awsRegion = RegionUtils.getRegion(registryCred.getRegion());
        if(awsRegion == null)
            throw(new IllegalArgumentException("Invalid Region: "+registryCred.getRegion()+" for ECR Registry"));
        if(!awsRegion.isServiceSupported("ecr"))
            throw(new IllegalArgumentException("ECR is not supported in Region: "+registryCred.getRegion()));
        String endpoint = awsRegion.getServiceEndpoint("ecr");
        if(endpoint == null)
            throw(new IllegalArgumentException("Unable to determine ECR endpoint in Region: "+registryCred.getRegion()));
        _awsEcrClient.setEndpoint(endpoint);
    }

    // Obtain from ContainerRepo.registryId:
    public AuthorizationToken getAuthorizationToken(String registryId) {
        List<AuthorizationData> authList =
            _awsEcrClient.getAuthorizationToken(new GetAuthorizationTokenRequest()
                                                .withRegistryIds(registryId))
            .getAuthorizationData();
        if ( null == authList || authList.isEmpty() ) return null;
        AuthorizationData data = authList.get(0);
        if ( null == data ) return null;
        return AuthorizationToken.builder()
            .token(data.getAuthorizationToken())
            .endpoint(URI.create(data.getProxyEndpoint()))
            .build();
    }

    public ContainerRepo getRepository(String repoName)
    {
        DescribeRepositoriesRequest request = new DescribeRepositoriesRequest();
        List<String> repoNames = new ArrayList<String>();
        repoNames.add(repoName);
        request.setRepositoryNames(repoNames);
        DescribeRepositoriesResult result = _awsEcrClient.describeRepositories(request);

        List<Repository> ecrRepos = result.getRepositories();
        if(ecrRepos == null || ecrRepos.size() == 0)
            return null;

        Repository ecrRepo = ecrRepos.get(0);
        ContainerRepo repo = ContainerRepo
        .builder()
        .provider(RegistryProvider.ECR)
        .credId(_registryCred.getId())
        .region(_registryCred.getRegion())
        .name(ecrRepo.getRepositoryName())
        .registryId(ecrRepo.getRegistryId())
        .build();
        return repo;
    }

    public List<ContainerRepo> listRepositories(PageIterator pageIterator)
    {
        int pageSize = pageIterator.getPageSize();
        if(pageSize > 100)
            throw(new IllegalArgumentException("Page Size cannot be greater than 100"));
        DescribeRepositoriesRequest request = new DescribeRepositoriesRequest();
        request.setMaxResults(pageSize);
        request.setNextToken(pageIterator.getMarker());

        DescribeRepositoriesResult result = _awsEcrClient.describeRepositories(request);
        pageIterator.setMarker(result.getNextToken());
        List<Repository> ecrRepos = result.getRepositories();
        List<ContainerRepo> containerRepos = new ArrayList<ContainerRepo>();
        for(Repository ecrRepo : ecrRepos)
        {
            ContainerRepo repo = ContainerRepo
            .builder()
            .provider(RegistryProvider.ECR)
            .credId(_registryCred.getId())
            .region(_registryCred.getRegion())
            .name(ecrRepo.getRepositoryName())
            .registryId(ecrRepo.getRegistryId())
            .endpoint(URI.create(ecrRepo.getRepositoryUri()).getHost())
            .build();
            containerRepos.add(repo);
        }
        return containerRepos;
    }

    public List<DockerImageId> listImages(ContainerRepo repo, PageIterator pageIterator)
    {
        int pageSize = pageIterator.getPageSize();
        if(pageSize > 100)
            throw(new IllegalArgumentException("Page Size cannot be greater than 100"));
        ListImagesRequest request = new ListImagesRequest();
        request.setMaxResults(pageSize);
        request.setNextToken(pageIterator.getMarker());
        request.setRegistryId(repo.getRegistryId());
        request.setRepositoryName(repo.getName());

        ListImagesResult result = _awsEcrClient.listImages(request);
        pageIterator.setMarker(result.getNextToken());
        List<ImageIdentifier> imageIds = result.getImageIds();
        List<DockerImageId> imageIdList = new ArrayList<DockerImageId>();
        for(ImageIdentifier imageId : imageIds)
        {
            DockerImageId dockerImageId = DockerImageId
            .builder()
            .tag(imageId.getImageTag())
            .sha(imageId.getImageDigest())
            .build();
            imageIdList.add(dockerImageId);
        }

        return imageIdList;
    }

    public List<DockerImage> describeImages(ContainerRepo repo, Collection<DockerImageId> imageIds, PageIterator pageIterator)
    {
        List<ImageIdentifier> imageIdentifiers = null;
        String nextMarker = null;
        if(imageIds != null && ! imageIds.isEmpty())
        {
            imageIdentifiers = new ArrayList<ImageIdentifier>();
            boolean after = null == pageIterator.getMarker();
            for(DockerImageId imageId : imageIds)
            {
                if ( null == imageId.getSha() ) continue;
                if ( ! after ) {
                    if ( imageId.getSha().equals(pageIterator.getMarker()) ) {
                        after = true;
                    }
                    continue;
                }
                ImageIdentifier imageIdentifier = new ImageIdentifier();
                imageIdentifier.setImageDigest(imageId.getSha());
                imageIdentifiers.add(imageIdentifier);
                if ( imageIdentifiers.size() >= pageIterator.getPageSize() ) {
                    nextMarker = imageId.getSha();
                    break;
                }
            }
        }

        DescribeImagesRequest request = new DescribeImagesRequest()
        .withNextToken(null == imageIdentifiers ? pageIterator.getMarker() : null)
        .withImageIds(imageIdentifiers)
        .withRegistryId(repo.getRegistryId())
        .withRepositoryName(repo.getName());

        DescribeImagesResult result = _awsEcrClient.describeImages(request);
        if ( null == imageIdentifiers ) {
            pageIterator.setMarker(result.getNextToken());
        } else {
            pageIterator.setMarker(nextMarker);
        }
        List<ImageDetail> imageList = result.getImageDetails();
        List<DockerImage> images = new ArrayList<DockerImage>();
        for(ImageDetail imageDetail : imageList)
        {
            Date pushedAt = imageDetail.getImagePushedAt();
            Long pushTime = null;
            if(pushedAt != null)
                pushTime = pushedAt.getTime();

            String imageSha = imageDetail.getImageDigest().toLowerCase();
            List<String> imageTags = imageDetail.getImageTags();
            DockerImage.DockerImageBuilder builder = DockerImage
            .builder()
            .imageSha(imageSha)
            .pushTime(pushTime)
            .imageSize(imageDetail.getImageSizeInBytes());

            if(imageTags != null)
                builder.imageTags(imageTags);
            DockerImage image = builder.build();
            images.add(image);
        }
        return images;
    }
}
