/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.platform.ws;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.cluster.health.NodeDetails;
import org.sonar.cluster.health.NodeHealth;
import org.sonar.server.health.ClusterHealth;
import org.sonar.server.health.Health;
import org.sonar.server.health.HealthChecker;
import org.sonar.server.platform.WebServer;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.WsSystem;

import static java.util.Collections.emptySet;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.cluster.health.NodeDetails.newNodeDetailsBuilder;
import static org.sonar.cluster.health.NodeHealth.newNodeHealthBuilder;
import static org.sonar.server.health.Health.newHealthCheckBuilder;
import static org.sonar.test.JsonAssert.assertJson;

public class ClusterHealthActionTest {
  private Random random = new Random();
  private WebServer webServer = mock(WebServer.class);
  private HealthChecker healthChecker = mock(HealthChecker.class);
  private WsActionTester underTest = new WsActionTester(new ClusterHealthAction(new ClusterHealthActionSupport(), webServer, healthChecker));

  @Test
  public void verify_definition() {
    WebService.Action def = underTest.getDef();

    assertThat(def.key()).isEqualTo("cluster_health");
    assertThat(def.params()).isEmpty();
    assertThat(def.since()).isEqualTo("6.6");
    assertThat(def.description()).isNotEmpty();
    assertThat(def.responseExample()).isNotNull();
    assertThat(def.isInternal()).isFalse();
    assertThat(def.isPost()).isFalse();
  }

  @Test
  public void returns_501_if_standalone() {
    when(webServer.isStandalone()).thenReturn(true);

    TestResponse response = underTest.newRequest().execute();

    assertThat(response.getStatus()).isEqualTo(501);
    assertThat(response.getInput()).isEmpty();
  }

  @Test
  public void returns_200_if_clustering_enabled() {
    when(webServer.isStandalone()).thenReturn(false);
    when(healthChecker.checkCluster())
      .thenReturn(new ClusterHealth(Health.GREEN, emptySet()));

    TestResponse response = underTest.newRequest().execute();

    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  public void response_contains_status_and_causes_from_HealthChecker_checkCluster() {
    Health.Status randomStatus = Health.Status.values()[random.nextInt(Health.Status.values().length)];
    String[] causes = IntStream.range(0, random.nextInt(33)).mapToObj(i -> randomAlphanumeric(4)).toArray(String[]::new);
    Health.Builder healthBuilder = newHealthCheckBuilder()
      .setStatus(randomStatus);
    Arrays.stream(causes).forEach(healthBuilder::addCause);
    when(webServer.isStandalone()).thenReturn(false);
    when(healthChecker.checkCluster()).thenReturn(new ClusterHealth(healthBuilder.build(), emptySet()));

    WsSystem.ClusterHealthResponse clusterHealthResponse = underTest.newRequest().executeProtobuf(WsSystem.ClusterHealthResponse.class);
    assertThat(clusterHealthResponse.getHealth().name()).isEqualTo(randomStatus.name());
    assertThat(clusterHealthResponse.getCausesList())
      .extracting(WsSystem.Cause::getMessage)
      .containsOnly(causes);
  }

  @Test
  public void verify_response_example() {
    when(webServer.isStandalone()).thenReturn(false);
    when(healthChecker.checkCluster())
      .thenReturn(
        new ClusterHealth(newHealthCheckBuilder()
          .setStatus(Health.Status.RED)
          .addCause("Application node app-1 is RED")
          .build(),
          ImmutableSet.of(
            newNodeHealthBuilder()
              .setStatus(NodeHealth.Status.RED)
              .addCause("foo")
              .setDetails(
                newNodeDetailsBuilder()
                  .setName("app-1")
                  .setType(NodeDetails.Type.APPLICATION)
                  .setHost("192.168.1.1")
                  .setPort(999)
                  .setStarted(parseDateTime("2015-08-13T23:34:59+0200").getTime())
                  .build())
              .setDate(1 + random.nextInt(888))
              .build(),
            newNodeHealthBuilder()
              .setStatus(NodeHealth.Status.YELLOW)
              .addCause("bar")
              .setDetails(
                newNodeDetailsBuilder()
                  .setName("app-2")
                  .setType(NodeDetails.Type.APPLICATION)
                  .setHost("192.168.1.2")
                  .setPort(999)
                  .setStarted(parseDateTime("2015-08-13T23:34:59+0200").getTime())
                  .build())
              .setDate(1 + random.nextInt(888))
              .build(),
            newNodeHealthBuilder()
              .setStatus(NodeHealth.Status.GREEN)
              .setDetails(
                newNodeDetailsBuilder()
                  .setName("es-1")
                  .setType(NodeDetails.Type.SEARCH)
                  .setHost("192.168.1.3")
                  .setPort(999)
                  .setStarted(parseDateTime("2015-08-13T23:34:59+0200").getTime())
                  .build())
              .setDate(1 + random.nextInt(888))
              .build(),
            newNodeHealthBuilder()
              .setStatus(NodeHealth.Status.GREEN)
              .setDetails(
                newNodeDetailsBuilder()
                  .setName("es-2")
                  .setType(NodeDetails.Type.SEARCH)
                  .setHost("192.168.1.4")
                  .setPort(999)
                  .setStarted(parseDateTime("2015-08-13T23:34:59+0200").getTime())
                  .build())
              .setDate(1 + random.nextInt(888))
              .build(),
            newNodeHealthBuilder()
              .setStatus(NodeHealth.Status.GREEN)
              .setDetails(
                newNodeDetailsBuilder()
                  .setName("es-3")
                  .setType(NodeDetails.Type.SEARCH)
                  .setHost("192.168.1.5")
                  .setPort(999)
                  .setStarted(parseDateTime("2015-08-13T23:34:59+0200").getTime())
                  .build())
              .setDate(1 + random.nextInt(888))
              .build())));

    TestResponse response = underTest.newRequest().execute();

    assertJson(response.getInput())
      .isSimilarTo(underTest.getDef().responseExampleAsString());
  }
}
