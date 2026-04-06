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
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the duplicate class detection feature of {@link DevModuleExtension}.
 *
 * <p>Both {@code test-lib-a} and {@code test-lib-b} contain the same
 * {@code org.os890.ds.addon.dev.testlib.DuplicateBean} class. When the CDI
 * container boots, the extension should detect the duplicate via
 * {@code ClassLoader.getResources()} and log a warning.</p>
 */
@EnableTestBeans(manageContainer = false)
class DuplicateClassDetectionTest
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
    void duplicateClassIsDetected()
    {
        assertTrue(
            logHandler.containsMessageAtLevel("Duplicate classes detected", Level.WARNING),
            "Extension should log a warning about duplicate classes"
        );
    }

    @Test
    void duplicateWarningContainsBeanClassName()
    {
        assertTrue(
            logHandler.containsMessageAtLevel("DuplicateBean", Level.WARNING),
            "Duplicate warning should mention the duplicated class name"
        );
    }

    @Test
    void duplicateWarningContainsTestLibAPath()
    {
        assertTrue(
            logHandler.containsMessageAtLevel("test-lib-a", Level.WARNING),
            "Duplicate warning should contain the path to test-lib-a"
        );
    }

    @Test
    void duplicateWarningContainsTestLibBPath()
    {
        assertTrue(
            logHandler.containsMessageAtLevel("test-lib-b", Level.WARNING),
            "Duplicate warning should contain the path to test-lib-b"
        );
    }

    private static class TestLogHandler extends Handler
    {
        private final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(LogRecord record)
        {
            records.add(record);
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

        boolean containsMessageAtLevel(String substring, Level level)
        {
            for (LogRecord record : records)
            {
                if (record.getLevel().equals(level) &&
                    record.getMessage() != null &&
                    record.getMessage().contains(substring))
                {
                    return true;
                }
            }
            return false;
        }
    }
}
