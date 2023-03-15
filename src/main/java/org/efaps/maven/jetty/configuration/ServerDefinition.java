/*
 * Copyright 2003 - 2023 The eFaps Team
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
 */

package org.efaps.maven.jetty.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.digester3.Digester;
import org.apache.commons.digester3.annotations.rules.SetNext;
import org.apache.commons.digester3.binder.AbstractRulesModule;
import org.apache.commons.digester3.binder.DigesterLoader;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author The eFaps Team
 */
public class ServerDefinition
    extends AbstractDefinition
{
    /**
     * Logging instance used to give logging information of this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ServerDefinition.class);

    /**
     * List of all Filters used in this server definition.
     */
    private final List<FilterDefinition> filters = new ArrayList<>();

    /**
     * List of all servlets used in this server definition.
     */
    private final List<ServletDefinition> servlets = new ArrayList<>();

    /**
     * Use websocket or not.
     */
    private boolean websocket;

    /**
     * Initializes a new instanc of the server definition a a XML file.
     *
     * @param _url  path to the XML file within the server definition
     * @return configured instance from the XML file
     */
    public static ServerDefinition read(final String _url)
    {
        ServerDefinition ret = null;
        try {
            final DigesterLoader loader = DigesterLoader.newLoader(new AbstractRulesModule()
            {

                @Override
                protected void configure()
                {
                    forPattern("server").createObject().ofType(ServerDefinition.class)
                        .then().setProperties();
                    forPattern("server/parameter")
                        .callMethod("addIniParam").withParamCount(2)
                        .withParamTypes(String.class, String.class)
                        .then().callParam().fromAttribute("key").ofIndex(0)
                        .then().callParam().ofIndex(1);

                    forPattern("server/filter").createObject().ofType(FilterDefinition.class)
                        .then().setNext("addFilter");
                    forPattern("server/filter").setProperties();
                    forPattern("server/filter/parameter")
                        .callMethod("addIniParam").withParamCount(2)
                        .withParamTypes(String.class, String.class)
                        .then().callParam().fromAttribute("key").ofIndex(0)
                        .then().callParam().ofIndex(1);

                    forPattern("server/servlet").createObject().ofType(ServletDefinition.class)
                        .then().setNext("addServlet");
                    forPattern("server/servlet").setProperties();
                    forPattern("server/servlet/parameter")
                        .callMethod("addIniParam").withParamCount(2)
                        .withParamTypes(String.class, String.class)
                        .then().callParam().fromAttribute("key").ofIndex(0)
                        .then().callParam().ofIndex(1);
                }
            });

            final Digester digester = loader.newDigester();
            ret = (ServerDefinition) digester.parse(_url);

        } catch (final Exception e) {
            ServerDefinition.LOG.error(_url.toString() + " is not readable", e);
        }
        return ret;
    }

    /**
     * Updates the context handler (defining the server) by appending servlets
     * and filters.
     *
     * @param _handler  context handler used to add filters / servlets
     * @see FilterDefinition#updateServer(Context)
     * @see ServletDefinition#updateServer(Context)
     */
    public void updateServer(final ServletContextHandler _handler)
    {
        for (final Entry<String, String> entry : getIniParams().entrySet()) {
            _handler.setInitParameter(entry.getKey(), entry.getValue());
        }
        for (final FilterDefinition filter : this.filters)  {
            filter.updateServer(_handler);
        }
        for (final ServletDefinition servlet : this.servlets)  {
            servlet.updateServer(_handler);
        }
    }

    /**
     * Adds a new filter definition to the list of filter definition.
     *
     * @param _filter filter to add to the list of filters
     * @see #filters
     */
    @SetNext
    public void addFilter(final FilterDefinition _filter)
    {
        this.filters.add(_filter);
    }

    /**
     * Adds a new servlet definition to the list of servlet definitions.
     *
     * @param _servlet  servlet to add to the list of servlets
     * @see #servlets
     */
    @SetNext
    public void addServlet(final ServletDefinition _servlet)
    {
        this.servlets.add(_servlet);
    }

    /**
     * Getter method for the instance variable {@link #websocket}.
     *
     * @return value of instance variable {@link #websocket}
     */
    public boolean isWebsocket()
    {
        return this.websocket;
    }

    /**
     * Setter method for instance variable {@link #websocket}.
     *
     * @param _websocket value for instance variable {@link #websocket}
     */
    public void setWebsocket(final boolean _websocket)
    {
        this.websocket = _websocket;
    }

    /**
     * @param _wac context to be updated
     */
    public void updateContext(final WebAppContext _wac)
    {
        for (final Entry<String, String> entry : getIniParams().entrySet()) {
            _wac.setInitParameter(entry.getKey(), entry.getValue());
        }
    }
}
