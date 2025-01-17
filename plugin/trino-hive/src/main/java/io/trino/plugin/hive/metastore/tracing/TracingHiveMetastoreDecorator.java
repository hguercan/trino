/*
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
 */
package io.trino.plugin.hive.metastore.tracing;

import com.google.inject.Inject;
import io.opentelemetry.api.trace.Tracer;
import io.trino.plugin.hive.metastore.HiveMetastore;
import io.trino.plugin.hive.metastore.HiveMetastoreDecorator;

import static java.util.Objects.requireNonNull;

public class TracingHiveMetastoreDecorator
        implements HiveMetastoreDecorator
{
    private final Tracer tracer;

    @Inject
    public TracingHiveMetastoreDecorator(Tracer tracer)
    {
        this.tracer = requireNonNull(tracer, "tracer is null");
    }

    @Override
    public int getPriority()
    {
        return PRIORITY_TRACING;
    }

    @Override
    public HiveMetastore decorate(HiveMetastore hiveMetastore)
    {
        return new TracingHiveMetastore(tracer, hiveMetastore);
    }
}
