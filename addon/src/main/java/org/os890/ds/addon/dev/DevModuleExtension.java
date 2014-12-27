package org.os890.ds.addon.dev;

import org.apache.deltaspike.core.util.ClassUtils;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.logging.Logger;

public class DevModuleExtension implements Extension
{
    private static final Logger LOG = Logger.getLogger(DevModuleExtension.class.getName());

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

    protected void logInfo(String name, String apiClassToCheck, String implClassToCheck)
    {
        Class currentClass = ClassUtils.tryToLoadClassForName(apiClassToCheck);

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

    protected void logProcessAnnotatedTypeSources(@Observes ProcessAnnotatedType processAnnotatedType, BeanManager beanManager)
    {
        Class beanClass = processAnnotatedType.getAnnotatedType().getJavaClass();
        if (!beanClass.getName().startsWith("org.apache.deltaspike.") &&
            !beanClass.getName().startsWith("org.apache.myfaces") &&
            !beanClass.getName().equals(getClass().getName()))
        {
            LOG.info("class:\t" + beanClass.getName() + " - located at:\t" +
                beanClass.getClassLoader().getResource(beanClass.getName().replace(".", "/") + ".class"));
        }
    }
}
