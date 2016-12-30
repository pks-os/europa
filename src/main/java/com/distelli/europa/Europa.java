/*
  $Id: $
  @file Europa.java
  @brief Contains the Europa.java class

  @author Rahul Singh [rsingh]
  Copyright (c) 2013, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa;

import java.io.File;

import com.distelli.europa.util.*;
import com.distelli.ventura.*;
import com.distelli.europa.guice.*;
import com.distelli.europa.monitor.*;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Stage;
import com.distelli.persistence.impl.PersistenceModule;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j;

@Log4j
public class Europa
{
    private RequestHandlerFactory _requestHandlerFactory = null;
    private RouteMatcher _routeMatcher = null;
    private int _port = 8080;

    @Inject
    private MonitorQueue _monitorQueue;
    private Thread _monitorThread;

    public Europa(String[] args)
    {
        CmdLineArgs cmdLineArgs = new CmdLineArgs(args);
        boolean logToConsole = cmdLineArgs.hasOption(Constants.LOG_TO_CONSOLE_ARG);
        // Initialize Logging
        File logsDir = new File("./logs/");
        if(!logsDir.exists())
            logsDir.mkdirs();

        if(logToConsole)
            Log4JConfigurator.configure(true);
        else
            Log4JConfigurator.configure(logsDir, "Europa");
        Log4JConfigurator.setLogLevel("INFO");
        Log4JConfigurator.setLogLevel("com.distelli.europa.monitor", "DEBUG");
        Log4JConfigurator.setLogLevel("com.distelli.gcr", "DEBUG");
        String configFilePath = cmdLineArgs.getOption("config");
        if(configFilePath == null)
        {
            log.fatal("Missing value for arg --config");
            System.exit(1);
        }
        String portStr = cmdLineArgs.getOption("port");
        if(portStr != null)
        {
            try {
                _port = Integer.parseInt(portStr);
            } catch(NumberFormatException nfe) {
                log.fatal("Invalid value for --port "+portStr);
                System.exit(1);
            }
        }

        EuropaConfiguration europaConfiguration = EuropaConfiguration.fromFile(new File(configFilePath));
        final Injector injector = Guice.createInjector(Stage.PRODUCTION,
                                                       new PersistenceModule(),
                                                       new AjaxHelperModule(),
                                                       new EuropaInjectorModule(europaConfiguration));
        injector.injectMembers(this);
        _requestHandlerFactory = new RequestHandlerFactory() {
                public RequestHandler getRequestHandler(MatchedRoute route) {
                    return injector.getInstance(route.getRequestHandler());
                }
            };
        _routeMatcher = Routes.getRouteMatcher();
    }

    public void start()
    {
        RepoMonitor monitor = new RepoMonitor(_monitorQueue);
        _monitorThread = new Thread(monitor);
        _monitorThread.start();

        WebServlet servlet = new WebServlet(_routeMatcher, _requestHandlerFactory);
        WebServer webServer = new WebServer(_port, servlet, "/");
        webServer.setCacheControl("max-age=300");
        webServer.setEtags(true);
        webServer.start();
    }

    public static void main(String[] args)
    {
        Europa europa = new Europa(args);
        europa.start();
    }
}