/*
 * Copyright (C) 2023 The Prometheus jmx_exporter Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx.test;

import static io.prometheus.jmx.test.support.Assertions.assertCommonMetricsResponse;
import static io.prometheus.jmx.test.support.Assertions.assertHealthyResponse;
import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.test.common.ExporterTestEnvironment;
import io.prometheus.jmx.test.common.ExporterTestEnvironmentFactory;
import io.prometheus.jmx.test.common.ExporterTestSupport;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.testcontainers.containers.Network;
import org.verifyica.api.ArgumentContext;
import org.verifyica.api.ClassContext;
import org.verifyica.api.Trap;
import org.verifyica.api.Verifyica;

public class LowerCaseOutputNamesTest {

    @Verifyica.ArgumentSupplier(parallelism = 4)
    public static Stream<ExporterTestEnvironment> arguments() {
        return ExporterTestEnvironmentFactory.createExporterTestEnvironments();
    }

    @Verifyica.Prepare
    public static void prepare(ClassContext classContext) {
        ExporterTestSupport.getOrCreateNetwork(classContext);
    }

    @Verifyica.BeforeAll
    public void beforeAll(ArgumentContext argumentContext) {
        Class<?> testClass = argumentContext.classContext().testClass();
        Network network = ExporterTestSupport.getOrCreateNetwork(argumentContext);
        ExporterTestSupport.initializeExporterTestEnvironment(argumentContext, network, testClass);
    }

    @Verifyica.Test
    public void testHealthy(ExporterTestEnvironment exporterTestEnvironment) throws IOException {
        String url = exporterTestEnvironment.getBaseUrl() + "/-/healthy";
        HttpResponse httpResponse = HttpClient.sendRequest(url);

        assertHealthyResponse(httpResponse);
    }

    @Verifyica.Test
    public void testMetrics(ExporterTestEnvironment exporterTestEnvironment) throws IOException {
        String url = exporterTestEnvironment.getBaseUrl() + "/metrics";
        HttpResponse httpResponse = HttpClient.sendRequest(url);

        assertMetricsResponse(exporterTestEnvironment, httpResponse);
    }

    @Verifyica.Test
    public void testMetricsOpenMetricsFormat(ExporterTestEnvironment exporterTestEnvironment)
            throws IOException {
        String url = exporterTestEnvironment.getBaseUrl() + "/metrics";
        HttpResponse httpResponse =
                HttpClient.sendRequest(
                        url,
                        "CONTENT-TYPE",
                        "application/openmetrics-text; version=1.0.0; charset=utf-8");

        assertMetricsResponse(exporterTestEnvironment, httpResponse);
    }

    @Verifyica.Test
    public void testMetricsPrometheusFormat(ExporterTestEnvironment exporterTestEnvironment)
            throws IOException {
        String url = exporterTestEnvironment.getBaseUrl() + "/metrics";
        HttpResponse httpResponse =
                HttpClient.sendRequest(
                        url, "CONTENT-TYPE", "text/plain; version=0.0.4; charset=utf-8");

        assertMetricsResponse(exporterTestEnvironment, httpResponse);
    }

    @Verifyica.Test
    public void testMetricsPrometheusProtobufFormat(ExporterTestEnvironment exporterTestEnvironment)
            throws IOException {
        String url = exporterTestEnvironment.getBaseUrl() + "/metrics";
        HttpResponse httpResponse =
                HttpClient.sendRequest(
                        url,
                        "CONTENT-TYPE",
                        "application/vnd.google.protobuf; proto=io.prometheus.client.MetricFamily;"
                                + " encoding=delimited");

        assertMetricsResponse(exporterTestEnvironment, httpResponse);
    }

    @Verifyica.AfterAll
    public void afterAll(ArgumentContext argumentContext) throws Throwable {
        List<Trap> traps = new ArrayList<>();

        traps.add(
                new Trap(
                        () -> ExporterTestSupport.destroyExporterTestEnvironment(argumentContext)));
        traps.add(new Trap(() -> ExporterTestSupport.destroyNetwork(argumentContext)));

        Trap.assertEmpty(traps);
    }

    @Verifyica.Conclude
    public static void conclude(ClassContext classContext) throws Throwable {
        ExporterTestSupport.destroyNetwork(classContext);
    }

    private void assertMetricsResponse(
            ExporterTestEnvironment exporterTestEnvironment, HttpResponse httpResponse) {
        assertCommonMetricsResponse(httpResponse);

        Collection<Metric> metrics = MetricsParser.parseCollection(httpResponse);

        /*
         * Assert that all metrics have lower case names
         */
        metrics.forEach(
                metric ->
                        assertThat(metric.name())
                                .isEqualTo(metric.name().toLowerCase(Locale.ENGLISH)));
    }
}
