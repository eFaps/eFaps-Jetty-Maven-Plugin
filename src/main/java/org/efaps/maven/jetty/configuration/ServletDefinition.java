/*
 * Copyright 2003 - 2016 The eFaps Team
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

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * @author The eFaps Team
 */
public class ServletDefinition
    extends AbstractDefinition
{
    /**
     * Initialize order of the servlet.
     */
    private Integer initorder;

    /**
     * Display name of the servlet.
     */
    private String displayname;

    /**
     *
     * @param _handler servlet context handler
     */
    public void updateServer(final ServletContextHandler _handler)
    {
        final ServletHolder servlet = new ServletHolder();
        servlet.setName(getName());
        servlet.setDisplayName(this.displayname);
        servlet.setClassName(getClassname());
        servlet.setInitParameters(getIniParams());
        if (this.initorder != null)  {
            servlet.setInitOrder(this.initorder);
        }
        _handler.addServlet(servlet, getPath());
    }

    /**
     * Setter method for instance variable {@link #initOrder}.
     *
     * @param _initOrder    new initt order to set
     * @see #initOrder
     */
    public void setInitorder(final int _initOrder)
    {
        this.initorder = _initOrder;
    }

    /**
     * Setter method for instance variable {@link #displayName}.
     *
     * @param _displayName  new display name to set
     * @see #displayName
     */
    public void setDisplayname(final String _displayName)
    {
        this.displayname = _displayName;
    }
}
