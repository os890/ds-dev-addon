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

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.os890.cdi.addon.dynamictestbean.EnableTestBeans;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CDI integration test that boots the container and verifies the
 * {@link DevModuleExtension} observes lifecycle events and logs
 * DeltaSpike module information.
 *
 * <p>Uses manual container management so that the log handler is
 * installed before the CDI container boots.</p>
 */
@EnableTestBeans(manageContainer = false)
class DevModuleExtensionTest
{
    private static final Logger EXTENSION_LOGGER = Logger.getLogger(DevModuleExtension.class.getName());
    private static TestLogHandler logHandler;
    private static SeContainer container;

    @BeforeAll
    static void bootContainer()
    {
        logHandler = new TestLogHandler();
        EXTENSION_LOGGER.addHandler(logHandler);
        container = SeContainerInitializer.newInstance().initialize();
    }

    @AfterAll
    static void shutdownContainer()
    {
        if (container != null && container.isRunning())
        {
            container.close();
        }
        EXTENSION_LOGGER.removeHandler(logHandler);
    }

    @Test
    void extensionLogsDeltaSpikeCoreApiVersion()
    {
        assertTrue(
            logHandler.containsMessage("DeltaSpike Core-API v"),
            "Extension should log DeltaSpike Core-API version"
        );
    }

    @Test
    void extensionLogsDeltaSpikeCoreImplVersion()
    {
        assertTrue(
            logHandler.containsMessage("DeltaSpike Core-Impl v"),
            "Extension should log DeltaSpike Core-Impl version"
        );
    }

    @Test
    void extensionLogsNotFoundForAbsentModules()
    {
        assertTrue(
            logHandler.containsMessage("not found"),
            "Extension should log 'not found' for absent DeltaSpike modules"
        );
    }

    private static class TestLogHandler extends Handler
    {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void publish(LogRecord record)
        {
            if (record.getMessage() != null)
            {
                messages.add(record.getMessage());
            }
        }

        @Override
        public void flush()
        {
            // no-op
        }

        @Override
        public void close()
        {
            // no-op
        }

        boolean containsMessage(String substring)
        {
            for (String message : messages)
            {
                if (message.contains(substring))
                {
                    return true;
                }
            }
            return false;
        }
    }
}
