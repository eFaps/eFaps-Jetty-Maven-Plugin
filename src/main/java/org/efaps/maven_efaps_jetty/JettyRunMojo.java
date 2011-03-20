/*
 * Copyright 2003 - 2009 The eFaps Team
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

package org.efaps.maven_efaps_jetty;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.efaps.init.StartupDatabaseConnection;
import org.efaps.init.StartupException;
import org.efaps.maven.logger.SLF4JOverMavenLog;
import org.efaps.maven_efaps_jetty.configuration.ServerDefinition;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoPhase;
import org.jfrog.maven.annomojo.annotations.MojoRequiresDependencyResolution;
import org.jfrog.maven.annomojo.annotations.MojoRequiresDirectInvocation;

/**
 * The goal starts the Jetty web server.
 *
 * @author The eFaps Team
 * @version $Id$
 * @todo description
 */
@MojoGoal(value = "run")
@MojoRequiresDirectInvocation(value = true)
@MojoPhase(value = "install")
@MojoRequiresDependencyResolution(value = "compile")
public class JettyRunMojo
    implements Mojo
{
    /**
     * Defines the Port on which the Jetty is started. Default value is
     * <i>8888</i>.
     */
    @MojoParameter(defaultValue = "8888")
    private int port;

    /**
     * Defines the Host (Adapter) on which the jetty is started. Default value
     * is <i>localhost</i>.
     */
    @MojoParameter(defaultValue = "127.0.0.1")
    private String host;

    /**
     *
     */
    @MojoParameter(required = true)
    private String jaasConfigFile;

    /**
     *
     */
    @MojoParameter(required = true)
    private String configFile;

    /**
     * Class name of the SQL database factory (implementing interface
     * {@link #javax.sql.DataSource}).
     *
     * @see javax.sql.DataSource
     * @see #initDatabase
     */
    @MojoParameter(required = true, expression = "${org.efaps.db.factory}")
    private String factory;

    /**
     * Holds all properties of the connection to the database. The properties
     * are separated by a comma.
     */
    @MojoParameter(expression = "${org.efaps.db.connection}", required = true)
    private String connection;

    /**
     * Defines the database type (used to define database specific
     * implementations).
     */
    @MojoParameter(expression = "${org.efaps.db.type}", required = true)
    private String type;


    /**
     * Value for the timeout of the transaction.
     */
    @MojoParameter(expression = "${org.efaps.transaction.timeout}", required = false)
    private String transactionTimeout;

    /**
     * Name of the class for the transaction manager.
     */
    @MojoParameter(expression = "${org.efaps.transaction.manager}", defaultValue = "org.objectweb.jotm.Current",
                    required = true)
    private String transactionManager;


    /**
     * The Apache Maven logger is stored in this instance variable.
     *
     * @see #getLog
     * @see #setLog
     */
    private Log log = null;

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

        getLog().info("Starting jetty Version "
                      + server.getClass().getPackage().getImplementationVersion());

        final Connector connector = new SelectChannelConnector();
        connector.setPort(this.port);
        connector.setHost(this.host);
        server.addConnector(connector);

        final ContextHandlerCollection contexts = new ContextHandlerCollection();
        server.setHandler(contexts);

        System.setProperty("java.security.auth.login.config",
                           this.jaasConfigFile);

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
        try  {
            Class.forName("org.efaps.maven.logger.SLF4JOverMavenLog");
            SLF4JOverMavenLog.LOGGER = getLog();
        } catch (final ClassNotFoundException e)  {
            getLog().error("could not initialize SLF4J over maven logger");
        }

        try {
            StartupDatabaseConnection.startup(this.type,
                                              this.factory,
                                              this.connection,
                                              this.transactionManager,
                                              this.transactionTimeout == null
                                                  ? null
                                                  : Integer.parseInt(this.transactionTimeout));
        } catch (final StartupException e) {
            getLog().error("Initialize Database Connection failed: " + e.toString());
        }
    }

    /**
     * This is the setter method for instance variable {@link #log}.
     *
     * @param _log
     *          new value for instance variable {@link #log}
     * @see #log
     * @see #getLog
     */
    public void setLog(final Log _log)
    {
        this.log = _log;
    }

    /**
     * This is the getter method for instance variable {@link #log}.
     *
     * @return value of instance variable {@link #log}
     * @see #log
     * @see #setLog
     */
    public Log getLog()
    {
        return this.log;
    }
}
