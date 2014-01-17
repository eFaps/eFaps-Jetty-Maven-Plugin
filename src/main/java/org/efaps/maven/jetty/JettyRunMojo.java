/*
 * Copyright 2003 - 2013 The eFaps Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Revision:        $Rev$
 * Last Changed:    $Date$
 * Last Changed By: $Author$
 */

package org.efaps.maven.jetty;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.jetty.plus.webapp.EnvConfiguration;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.efaps.init.StartupDatabaseConnection;
import org.efaps.init.StartupException;
import org.efaps.maven.jetty.configuration.ServerDefinition;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * The goal starts the Jetty web server.
 *
 * @author The eFaps Team
 * @version $Id$
 * @todo description
 */
@Mojo(name = "run", requiresDirectInvocation = true, defaultPhase = LifecyclePhase.INSTALL,
                requiresDependencyResolution = ResolutionScope.RUNTIME_PLUS_SYSTEM, requiresProject = true )
public class JettyRunMojo
    extends AbstractMojo
{

    /**
     * Defines the Port on which the Jetty is started. Default value is
     * <i>8888</i>.
     */
    @Parameter(defaultValue = "8888")
    private int port;

    /**
     * Defines the Host (Adapter) on which the jetty is started. Default value
     * is <i>localhost</i>.
     */
    @Parameter(defaultValue = "127.0.0.1")
    private String host;

    /**
     * Defines Form Limits for the Server. Default value
     * is <i>200000</i>.
     */
    @Parameter(defaultValue = "200000")
    private int maxFormContentSize;

    /**
     * Defines Form Limits for the Server. Default value
     * is <i>1500</i>.
     */
    @Parameter(defaultValue = "1500")
    private String maxFormKeys;

    /**
     * JaasConfigFile.
     */
    @Parameter(required = true)
    private String jaasConfigFile;

    /**
     * Configuration file path.
     */
    @Parameter(required = true)
    private String configFile;

    /**
     * Jetty env file path.
     */
    @Parameter
    private String envFile;

    /**
     * LogBack file path.
     */
    @Parameter(alias = "lbf")
    private String logbackFile;

    /**
     * Class name of the SQL database factory (implementing interface
     * {@link #javax.sql.DataSource}).
     *
     * @see javax.sql.DataSource
     * @see #initDatabase
     */
    @Parameter(required = true, property = "org.efaps.db.factory")
    private String factory;

    /**
     * Holds all properties of the connection to the database. The properties
     * are separated by a comma.
     */
    @Parameter(property = "org.efaps.db.connection", required = true)
    private String connection;

    /**
     * Defines the database type (used to define database specific
     * implementations).
     */
    @Parameter(property = "org.efaps.db.type", required = true)
    private String type;

    /**
     * Value for the timeout of the transaction.
     */
    @Parameter(property = "org.efaps.configuration.properties", required = false)
    private String configProps;

    /**
     * Name of the class for the transaction manager.
     */
    @Parameter(property = "org.efaps.transaction.manager",
                    defaultValue = "com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple",
                    required = true)
    private String transactionManager;

    /**
     * Name of the class for the transaction Synchronization Registry.
     */
    @Parameter(property = "org.efaps.transaction.synchronizationRegistry",
           defaultValue = "com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple",
                    required = true)
    private String transactionSynchronizationRegistry;

    /**
     * The current Maven project.
    */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * Runs the eFaps Jetty server.
     *
     * @throws MojoExecutionException if Jetty web server could not be started
     */
    public void execute()
        throws MojoExecutionException
    {
        init();

        final Server server = new Server();

        try {
            if (this.envFile != null) {
                final File file = new File(this.envFile);
                if (file.exists()) {
                    final EnvConfiguration envConfiguration = new EnvConfiguration();
                    envConfiguration.setJettyEnvXml(file.toURI().toURL());
                    final WebAppContext webcontext = new WebAppContext();
                    envConfiguration.configure(webcontext);
                }
            }
        } catch (final MalformedURLException e) {
            throw new MojoExecutionException("Could not read the Jetty env", e);
        } catch (final SAXException e) {
            throw new MojoExecutionException("Could not read the Jetty env", e);
        } catch (final IOException e) {
            throw new MojoExecutionException("Could not read the Jetty env", e);
        } catch (final Exception e) {
            throw new MojoExecutionException("Could not read the Jetty env", e);
        }

        getLog().info("Starting jetty Version " + server.getClass().getPackage().getImplementationVersion());

        server.setAttribute("org.eclipse.jetty.server.Request.maxFormContentSize", this.maxFormContentSize);
        server.setAttribute("org.eclipse.jetty.server.Request.maxFormKeys", this.maxFormKeys);

        final HttpConfiguration http_config = new HttpConfiguration();
        http_config.setRequestHeaderSize(131072);
        final ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(http_config));
        http.setPort(this.port);
        http.setHost(this.host);

        server.addConnector(http);

        final ContextHandlerCollection contexts = new ContextHandlerCollection();
        server.setHandler(contexts);

        System.setProperty("java.security.auth.login.config",
                           this.jaasConfigFile);
        new WebAppContext();
        final ServletContextHandler handler = new ServletContextHandler(contexts,
                                                                        "/eFaps",
                                                                        ServletContextHandler.SESSIONS);

        final ServerDefinition serverDef = ServerDefinition.read(this.configFile);
        serverDef.updateServer(handler);

        try {
            getLog().info("Starting Server");
            server.start();
            getLog().info("Server Started");
            server.join();
        } catch (final Exception e) {
            throw new MojoExecutionException("Could not Start Jetty Server", e);
        }
    }

    /**
     * @see #convertToMap   used to convert the connection string to a property
     *                      map
     * @see #type
     * @see #factory
     * @see #connection
     */
    protected void init()
    {
        // TODO correct that
        System.setProperty("ObjectStoreEnvironmentBean.objectStoreDir", "target");
        System.setProperty("ObjectStoreEnvironmentBean.localOSRoot", "eFapsStore");
        try {
            try {
                if (this.logbackFile != null) {
                    final ILoggerFactory logContext = LoggerFactory.getILoggerFactory();
                    if(logContext.getClass().getName().contains("ch.qos.logback.classic.LoggerContext")) {
                        final Class<?> logContextInter = this.project.getClass().getClassLoader()
                                        .loadClass("ch.qos.logback.core.Context");

                        final Class<?> configurator = this.project.getClass().getClassLoader()
                                        .loadClass("ch.qos.logback.classic.joran.JoranConfigurator");
                        final Object configInstance = configurator.newInstance();

                        final Method method = configurator.getMethod("setContext", new Class[] { logContextInter });
                        method.invoke(configInstance, logContext);

                        final Method reset = logContext.getClass().getMethod("reset");
                        reset.invoke(logContext);

                        final Method doConfigure = configurator.getMethod("doConfigure", new Class[] { String.class});
                        doConfigure.invoke(configInstance, this.logbackFile);
                    }
                }
            } catch (final Exception e) {
                getLog().error("Configuration of LogBack failed.",  e);
            }
            StartupDatabaseConnection.startup(this.type,
                            this.factory,
                            this.connection,
                            this.transactionManager,
                            this.transactionSynchronizationRegistry,
                            this.configProps);
        } catch (final StartupException e) {
            getLog().error("Initialize Database Connection failed: " + e.toString());
        }
    }
}
