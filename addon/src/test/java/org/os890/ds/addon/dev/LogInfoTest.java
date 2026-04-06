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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link DevModuleExtension#logInfo(String, String, String)} method.
 */
class LogInfoTest
{
    private final Logger extensionLogger = Logger.getLogger(DevModuleExtension.class.getName());
    private TestLogHandler logHandler;

    @BeforeEach
    void installLogHandler()
    {
        logHandler = new TestLogHandler();
        extensionLogger.addHandler(logHandler);
    }

    @AfterEach
    void removeLogHandler()
    {
        extensionLogger.removeHandler(logHandler);
    }

    @Test
    void logInfoReportsVersionForPresentClass()
    {
        DevModuleExtension extension = new DevModuleExtension();
        extension.logInfo(
            "Core",
            "org.apache.deltaspike.core.api.scope.GroupedConversation",
            "org.apache.deltaspike.core.impl.exclude.extension.ExcludeExtension"
        );

        assertTrue(
            logHandler.containsMessage("DeltaSpike Core-API v"),
            "logInfo should log the API version when the class is on the classpath"
        );
    }

    @Test
    void logInfoReportsNotFoundForMissingClass()
    {
        DevModuleExtension extension = new DevModuleExtension();
        extension.logInfo(
            "JSF",
            "org.apache.deltaspike.jsf.api.config.JsfModuleConfig",
            "org.apache.deltaspike.jsf.impl.scope.mapped.MappedJsf2ScopeExtension"
        );

        assertTrue(
            logHandler.containsMessageAtLevel("DeltaSpike JSF-API not found", Level.INFO),
            "logInfo should log 'not found' when the API class is missing"
        );
    }

    @Test
    void logInfoWarnsWhenImplIsMissing()
    {
        DevModuleExtension extension = new DevModuleExtension();
        extension.logInfo(
            "TestModule",
            "org.apache.deltaspike.core.api.scope.GroupedConversation",
            "com.nonexistent.SomeClass"
        );

        assertTrue(
            logHandler.containsMessageAtLevel("DeltaSpike TestModule-Impl not found", Level.WARNING),
            "logInfo should log a WARNING when the impl class is missing but API is present"
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

        boolean containsMessage(String substring)
        {
            for (LogRecord record : records)
            {
                if (record.getMessage() != null && record.getMessage().contains(substring))
                {
                    return true;
                }
            }
            return false;
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
