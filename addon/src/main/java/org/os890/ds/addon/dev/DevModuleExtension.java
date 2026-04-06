/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.os890.ds.addon.dev;

import org.apache.deltaspike.core.util.ClassUtils;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * CDI extension that logs diagnostic information about available DeltaSpike modules
 * and detects duplicate classes across different JAR files on the classpath.
 *
 * <p>During container startup, this extension:
 * <ul>
 *   <li>Checks which DeltaSpike modules (API and Impl) are on the classpath and logs their versions.</li>
 *   <li>Logs the source location of each discovered bean class.</li>
 *   <li>Detects classes that appear in multiple JAR files and logs a grouped warning.</li>
 * </ul>
 */
public class DevModuleExtension implements Extension
{
    private static final Logger LOG = Logger.getLogger(DevModuleExtension.class.getName());

    private final Map<String, List<String>> classLocations = new LinkedHashMap<>();

    /**
     * Observes the {@link BeforeBeanDiscovery} event to log information about
     * available DeltaSpike modules and the classpath visible to this extension.
     *
     * @param beforeBeanDiscovery the CDI lifecycle event fired before bean discovery
     */
    protected void init(@Observes BeforeBeanDiscovery beforeBeanDiscovery)
    {
        logInfo("Core", "org.apache.deltaspike.core.api.scope.GroupedConversation", "org.apache.deltaspike.core.impl.exclude.extension.ExcludeExtension");
        logInfo("JSF", "org.apache.deltaspike.jsf.api.config.JsfModuleConfig", "org.apache.deltaspike.jsf.impl.scope.mapped.MappedJsf2ScopeExtension");
        logInfo("Security", "org.apache.deltaspike.security.api.authorization.Secured", "org.apache.deltaspike.security.impl.extension.SecurityExtension");
        logInfo("JPA", "org.apache.deltaspike.jpa.api.transaction.Transactional", "org.apache.deltaspike.jpa.impl.transaction.TransactionalInterceptor");
        logInfo("Partial-Bean", "org.apache.deltaspike.partialbean.api.PartialBeanBinding", "org.apache.deltaspike.partialbean.impl.PartialBeanBindingExtension");
        logInfo("Scheduler", "org.apache.deltaspike.scheduler.api.Scheduled", "org.apache.deltaspike.scheduler.impl.SchedulerExtension");
        logInfo("BV", "org.apache.deltaspike.beanvalidation.impl.CDIAwareConstraintValidatorFactory" /*no api currently*/, "org.apache.deltaspike.beanvalidation.impl.CDIAwareConstraintValidatorFactory");
        logInfo("Data", "org.apache.deltaspike.data.api.Query", "org.apache.deltaspike.data.impl.RepositoryExtension");
        logInfo("Servlet", "org.apache.deltaspike.servlet.api.resourceloader.WebResourceProvider", "org.apache.deltaspike.servlet.impl.event.EventBroadcaster");
        logInfo("Test-Control", "org.apache.deltaspike.testcontrol.api.TestControl", "org.apache.deltaspike.testcontrol.impl.request.ContextControlDecorator");

        ClassLoader extensionClassLoader = getClass().getClassLoader();

        if (extensionClassLoader instanceof URLClassLoader)
        {
            LOG.info("Visible paths: " + Arrays.toString(((URLClassLoader) extensionClassLoader).getURLs()));
        }
    }

    /**
     * Checks whether the API and Impl classes for a given DeltaSpike module are
     * available on the classpath and logs their JAR versions.
     *
     * @param name the human-readable module name (e.g. "Core", "JSF")
     * @param apiClassToCheck fully qualified name of a class in the module's API JAR
     * @param implClassToCheck fully qualified name of a class in the module's Impl JAR
     */
    protected void logInfo(String name, String apiClassToCheck, String implClassToCheck)
    {
        Class<?> currentClass = ClassUtils.tryToLoadClassForName(apiClassToCheck);

        if (currentClass == null)
        {
            LOG.info("DeltaSpike " + name + "-API not found");
            return;
        }

        LOG.info("DeltaSpike " + name + "-API v" + ClassUtils.getJarVersion(currentClass));

        currentClass = ClassUtils.tryToLoadClassForName(implClassToCheck);

        if (currentClass != null)
        {
            LOG.info("DeltaSpike " + name + "-Impl v" + ClassUtils.getJarVersion(currentClass));
        }
        else
        {
            LOG.warning("DeltaSpike " + name + "-Impl not found");
        }
    }

    /**
     * Observes each {@link ProcessAnnotatedType} event, logging the source location
     * of the bean class and tracking its classpath locations for duplicate detection.
     *
     * @param processAnnotatedType the CDI lifecycle event for the annotated type being processed
     * @param beanManager the CDI bean manager
     */
    protected void logProcessAnnotatedTypeSources(@Observes ProcessAnnotatedType<?> processAnnotatedType, BeanManager beanManager)
    {
        Class<?> beanClass = processAnnotatedType.getAnnotatedType().getJavaClass();
        if (!beanClass.getName().startsWith("org.apache.deltaspike.") &&
            !beanClass.getName().startsWith("org.apache.myfaces") &&
            !beanClass.getName().equals(getClass().getName()))
        {
            String resourcePath = beanClass.getName().replace(".", "/") + ".class";

            try
            {
                Enumeration<URL> resources = beanClass.getClassLoader().getResources(resourcePath);
                boolean first = true;
                while (resources.hasMoreElements())
                {
                    URL resourceUrl = resources.nextElement();
                    String location = resourceUrl.toString();
                    if (first)
                    {
                        LOG.info("class:\t" + beanClass.getName() + " - located at:\t" + location);
                        first = false;
                    }
                    trackClassLocation(beanClass.getName(), location);
                }
            }
            catch (IOException e)
            {
                LOG.info("class:\t" + beanClass.getName() + " - located at:\tunknown");
            }
        }
    }

    /**
     * Observes the {@link AfterDeploymentValidation} event and logs warnings for
     * any bean classes that were found in multiple JAR files on the classpath.
     *
     * @param afterDeploymentValidation the CDI lifecycle event fired after deployment validation
     */
    protected void logDuplicateClasses(@Observes AfterDeploymentValidation afterDeploymentValidation)
    {
        boolean duplicatesFound = false;

        for (Map.Entry<String, List<String>> entry : classLocations.entrySet())
        {
            List<String> locations = entry.getValue();
            if (locations.size() > 1)
            {
                if (!duplicatesFound)
                {
                    LOG.warning("Duplicate classes detected across different JAR files:");
                    duplicatesFound = true;
                }

                StringBuilder message = new StringBuilder();
                message.append("  ").append(entry.getKey()).append(" found in:");
                for (String loc : locations)
                {
                    message.append("\n    - ").append(extractJarPath(loc));
                }
                LOG.warning(message.toString());
            }
        }

        classLocations.clear();
    }

    private void trackClassLocation(String className, String location)
    {
        String jarPath = extractJarPath(location);

        List<String> locations = classLocations.get(className);
        if (locations == null)
        {
            locations = new ArrayList<>();
            classLocations.put(className, locations);
        }

        if (!locations.contains(jarPath))
        {
            locations.add(jarPath);
        }
    }

    private String extractJarPath(String resourceUrl)
    {
        if (resourceUrl == null)
        {
            return "unknown";
        }

        // jar:file:/path/to/file.jar!/com/example/Class.class -> /path/to/file.jar
        if (resourceUrl.startsWith("jar:"))
        {
            int bangIndex = resourceUrl.indexOf('!');
            String jarUrl = bangIndex > 0 ? resourceUrl.substring(4, bangIndex) : resourceUrl.substring(4);
            if (jarUrl.startsWith("file:"))
            {
                return jarUrl.substring(5);
            }
            return jarUrl;
        }

        // file:/path/to/classes/com/example/Class.class -> /path/to/classes/
        if (resourceUrl.startsWith("file:"))
        {
            return resourceUrl.substring(5);
        }

        return resourceUrl;
    }
}
