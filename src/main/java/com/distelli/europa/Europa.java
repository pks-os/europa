/*
  $Id: $
  @file Europa.java
  @brief Contains the Europa.java class

  @author Rahul Singh [rsingh]
  Copyright (c) 2013, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa;

import com.distelli.europa.EuropaConfiguration.EuropaStage;
import com.distelli.europa.filters.ForceHttpsFilter;
import com.distelli.europa.filters.RegistryAuthFilter;
import com.distelli.europa.filters.StorageInitFilter;
import com.distelli.europa.guice.AjaxHelperModule;
import com.distelli.europa.guice.EuropaInjectorModule;
import com.distelli.europa.handlers.StaticContentErrorHandler;
import com.distelli.europa.monitor.DispatchRepoMonitorTasks;
import com.distelli.europa.util.CmdLineArgs;
import com.distelli.objectStore.impl.ObjectStoreModule;
import com.distelli.persistence.impl.PersistenceModule;
import com.distelli.webserver.*;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import lombok.extern.log4j.Log4j;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Arrays;
import java.util.List;

@Log4j
public class Europa
{
    protected RequestHandlerFactory _requestHandlerFactory = null;
    protected RequestContextFactory<EuropaRequestContext> _requestContextFactory = null;
    protected List<RequestFilter<EuropaRequestContext>> _registryApiFilters = null;
    protected List<RequestFilter<EuropaRequestContext>> _webappFilters = null;
    protected StaticContentErrorHandler _staticContentErrorHandler = null;
    protected int _port = 8080;
    protected int _sslPort = 8443;

    @Inject
    protected SslContextFactory _sslContextFactory;
    @Inject
    protected DispatchRepoMonitorTasks _dispatchRepoMonitorTasks;

    protected RouteMatcher _webappRouteMatcher = null;
    protected RouteMatcher _registryApiRouteMatcher = null;
    protected CmdLineArgs _cmdLineArgs = null;
    protected EuropaStage _stage = null;
    protected String _configFilePath = null;

    public Europa(String[] args)
    {
        _cmdLineArgs = new CmdLineArgs(args);
        // Initialize Logging
        File logsDir = new File("./logs/");
        if(!logsDir.exists())
            logsDir.mkdirs();

        _configFilePath = _cmdLineArgs.getOption("config");
        String portStr = _cmdLineArgs.getOption("port");
        if(portStr != null)
        {
            try {
                _port = Integer.parseInt(portStr);
            } catch(NumberFormatException nfe) {
                log.fatal("Invalid value for --port "+portStr);
                System.exit(1);
            }
        }
        portStr = _cmdLineArgs.getOption("ssl-port");
        if(portStr != null)
        {
            try {
                _sslPort = Integer.parseInt(portStr);
            } catch(NumberFormatException nfe) {
                log.fatal("Invalid value for --ssl-port "+portStr);
                System.exit(1);
            }
        }

        _stage = EuropaStage.prod;
        String stageArg = _cmdLineArgs.getOption("stage");
        if(stageArg != null) {
            try {
                _stage = EuropaStage.valueOf(stageArg);
            } catch(Throwable t) {
                throw(new RuntimeException("Invalid value for stage: "+stageArg, t));
            }
        }
        initialize();
    }

    protected void initializeWebServer(Injector injector)
    {
        _webappFilters = Arrays.asList(injector.getInstance(StorageInitFilter.class), injector.getInstance(ForceHttpsFilter.class));
        _registryApiFilters = Arrays.asList(injector.getInstance(RegistryAuthFilter.class));

        _requestContextFactory = new RequestContextFactory() {
                public RequestContext getRequestContext(HTTPMethod httpMethod, HttpServletRequest request) {
                    return new EuropaRequestContext(httpMethod, request);
                }
            };

        _requestHandlerFactory = new RequestHandlerFactory() {
                public RequestHandler getRequestHandler(MatchedRoute route) {
                    return injector.getInstance(route.getRequestHandler());
                }
            };
        _staticContentErrorHandler = injector.getInstance(StaticContentErrorHandler.class);
        _registryApiRouteMatcher = RegistryApiRoutes.getRouteMatcher();
        _webappRouteMatcher = WebAppRoutes.getRouteMatcher();
    }

    protected void initialize()
    {
        EuropaConfiguration europaConfiguration = null;
        if(_configFilePath != null)
            europaConfiguration = EuropaConfiguration.fromFile(new File(_configFilePath));
        else
            europaConfiguration = EuropaConfiguration.fromEnvironment();
        europaConfiguration.setStage(_stage);

        Injector injector = Guice.createInjector(Stage.PRODUCTION,
                                                 new PersistenceModule(),
                                                 new AjaxHelperModule(),
                                                 new ObjectStoreModule(),
                                                 new EuropaInjectorModule(europaConfiguration));
        injector.injectMembers(this);
        initializeWebServer(injector);
    }

    public void start()
    {
        _dispatchRepoMonitorTasks.schedule();

        WebServlet<EuropaRequestContext> servlet =
            new WebServlet<EuropaRequestContext>(_webappRouteMatcher, _requestHandlerFactory);
        servlet.setRequestContextFactory(_requestContextFactory);
        if(_webappFilters != null)
            servlet.setRequestFilters(_webappFilters);

        WebServer webServer = new WebServer(_port, servlet, "/", _sslPort, _sslContextFactory);

        ServletHolder staticHolder = new ServletHolder(DefaultServlet.class);
        staticHolder.setInitParameter("resourceBase", "./public");
        staticHolder.setInitParameter("dirAllowed","true");
        staticHolder.setInitParameter("pathInfoOnly","true");
        staticHolder.setInitParameter("etags", "true");
        staticHolder.setInitParameter("gzip", "true");
        staticHolder.setInitParameter("cacheControl", "max-age=3600");
        webServer.addStandardServlet("/public/*", staticHolder);

        WebServlet<EuropaRequestContext> registryApiServlet =
            new WebServlet<EuropaRequestContext>(_registryApiRouteMatcher, _requestHandlerFactory);
        servlet.setRequestContextFactory(_requestContextFactory);

        //Registry API Servlet uses the same request context (and
        //similar factory) as the main webapp servlet with the only
        //difference being that unmarshallJson is set to false
        registryApiServlet.setRequestContextFactory(new RequestContextFactory() {
                public RequestContext getRequestContext(HTTPMethod method, HttpServletRequest request) {
                    return new EuropaRequestContext(method, request, false);
                }
            });
        registryApiServlet.setRequestFilters(_registryApiFilters);
        webServer.addWebServlet("/v2/*", registryApiServlet);
        webServer.addWebServlet("/v1/*", registryApiServlet);

        webServer.setErrorHandler(_staticContentErrorHandler);
        webServer.start();
    }

    public static void main(String[] args)
    {
        Europa europa = new Europa(args);
        europa.start();
    }
}
