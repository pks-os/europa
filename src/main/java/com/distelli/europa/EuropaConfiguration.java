/*
  $Id: $
  @file EuropaConfiguration.java
  @brief Contains the EuropaConfiguration.java class

  @author Rahul Singh [rsingh]
  Copyright (c) 2013, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;

import java.io.File;

@Log4j
public class EuropaConfiguration
{
    @Getter @Setter
    protected String dbEndpoint;
    @Getter @Setter
    protected String dbUser;
    @Getter @Setter
    protected String dbPass;
    @Getter @Setter
    protected String dbPrefix;
    @Getter @Setter
    protected int dbMaxPoolSize = 2;
    @Getter @Setter
    protected EuropaStage stage;

    public static enum EuropaStage {
        alpha,
        beta,
        gamma,
        prod
    }

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    static {
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }


    public EuropaConfiguration()
    {

    }

    public static final EuropaConfiguration fromEnvironment() {
        String dbEndpoint = getEnvVar("EUROPA_DB_ENDPOINT");
        String dbUser = getEnvVar("EUROPA_DB_USER");
        String dbPass = getEnvVar("EUROPA_DB_PASS");
        String dbPrefix = getEnvVar("EUROPA_DB_PREFIX", false);
        int dbPoolSize = 2;
        String dbPoolSizeStr = null;
        try {
            dbPoolSizeStr = getEnvVar("EUROPA_DB_POOL_SIZE", false);
            if(dbPoolSizeStr != null && !dbPoolSizeStr.trim().isEmpty())
                dbPoolSize = Integer.parseInt(dbPoolSizeStr);
        } catch(Throwable t) {
            log.error("Invalid Value ["+dbPoolSizeStr+"] for Env Variable: EUROPA_DB_POOL_SIZE");
            dbPoolSize = 2;
        }

        EuropaConfiguration config = new EuropaConfiguration();
        config.setDbEndpoint(dbEndpoint);
        config.setDbUser(dbUser);
        config.setDbPass(dbPass);
        config.setDbPrefix(dbPrefix);
        config.setDbMaxPoolSize(dbPoolSize);
        config.validate();
        return config;
    }

    private static boolean missingConfigSettings = false;

    private static final String getEnvVar(String varName)
    {
        return getEnvVar(varName, true);
    }

    private static final String getEnvVar(String varName, boolean required)
    {
        String value = System.getenv(varName);
        if(value != null)
            return value;
        if(required) {
            log.error("Configuration error: environment variable " + varName + " must be set");
            missingConfigSettings = true;
        }
        return null;
    }

    public static final EuropaConfiguration fromFile(File configFile)
    {
        try {
            EuropaConfiguration config = OBJECT_MAPPER.readValue(configFile, EuropaConfiguration.class);
            config.validateConfigFile(configFile);
            config.validate();
            return config;
        } catch(Throwable t) {
            throw(new RuntimeException(t));
        }
    }

    private void validateConfigFile(File configFile) {
        if(dbEndpoint == null) {
            log.error("Configuration error: \"dbEndPoint\" not set in configuration file " + configFile.getAbsolutePath());
            missingConfigSettings = true;
        }
        if(dbUser == null) {
            log.error("Configuration error: \"dbUser\" not set in configuration file " + configFile.getAbsolutePath());
            missingConfigSettings = true;
        }
        if(dbPass == null) {
            log.error("Configuration error: \"dbPass\" not set in configuration file " + configFile.getAbsolutePath());
            missingConfigSettings = true;
        }
    }

    private void validate() throws RuntimeException {
        if(missingConfigSettings)
            throw(new RuntimeException("Configuration error: missing required settings"));
    }

    public boolean isProd()
    {
        return stage == EuropaStage.prod;
    }
}
