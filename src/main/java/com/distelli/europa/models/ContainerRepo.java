package com.distelli.europa.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerRepo
{
    private static final Pattern REPO_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_.-]+");

    protected String domain = null;
    protected String id = null;
    protected String name = null;
    protected String credId = null;
    protected String region = null;
    protected RegistryProvider provider = null;
    /**
     * The AWS Account for ECR registries
     */
    protected String registryId = null;
    protected String endpoint = null;
    protected RepoEvent lastEvent = null;
    protected boolean publicRepo = false;
    protected boolean local = true;
    /**
     * Users cannot push to a cache repo; sync tasks populate them instead.
     *
     * @see com.distelli.europa.sync.RepoSyncTask
     * @see com.distelli.europa.sync.ImageSyncTask
     */
    protected boolean mirror = false;
    /**
     * The ID of the object in the ObjectStore that holds the readme
     */
    protected String overviewId;
    protected long lastSyncTime;
    protected long syncCount; // Incremented at the beginning of each sync.
    protected Set<String> syncDestinationContainerRepoIds = new HashSet<>();

    public String getPullCommand()
    {
        if(this.provider == null)
            return null;
        switch(provider)
        {
        case GCR:
            //gcloud docker -- pull us.gcr.io/distelli-alpha/europa-enterprise
            return String.format("gcloud docker -- pull %s/%s", this.region, this.name);
        case ECR:
            if(this.registryId == null)
                return null;
            //docker pull 708141427824.dkr.ecr.us-east-1.amazonaws.com/distelli:latest
            return String.format("docker pull %s.dkr.ecr.%s.amazonaws.com/%s",this.registryId, this.region, this.name);
        case DOCKERHUB:
            return String.format("docker pull %s", this.name);
        case PRIVATE:
            if(this.endpoint == null)
                return null;
            return String.format("docker pull %s/%s", this.endpoint, this.name);
        case EUROPA:
            if(this.endpoint == null)
                return null;
            return String.format("docker pull %s/%s", this.endpoint, this.name);
        default:
            return null;
        }
    }

    /**
     * Check if a repository name is valid.
     *
     * A repository name is considered valid if it matches the regular
     * expression {@code [a-zA-Z0-9_.-]+}.
     * @param repoName the repository name to check
     * @return true if the name is valid, false if it is not
     */
    public static boolean isValidName(String repoName) {
        return null != repoName && REPO_NAME_PATTERN.matcher(repoName).matches();
    }
}
