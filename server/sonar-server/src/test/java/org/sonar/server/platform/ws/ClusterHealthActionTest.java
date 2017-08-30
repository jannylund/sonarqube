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

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.health.Health;
import org.sonar.server.health.HealthChecker;
import org.sonar.server.platform.cluster.Cluster;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;
import org.sonarqube.ws.WsSystem;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClusterHealthActionTest {
  private Random random = new Random();
  private Cluster cluster = mock(Cluster.class);
  private HealthChecker healthChecker = mock(HealthChecker.class);
  private WsActionTester underTest = new WsActionTester(new ClusterHealthAction(new ClusterHealthActionSupport(), cluster, healthChecker));

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
  public void returns_501_if_clustering_is_not_enabled() {
    when(cluster.isEnabled()).thenReturn(false);

    TestResponse response = underTest.newRequest().execute();

    assertThat(response.getStatus()).isEqualTo(501);
    assertThat(response.getInput()).isEmpty();
  }

  @Test
  public void returns_200_if_clustering_is_enabled() {
    when(cluster.isEnabled()).thenReturn(true);
    when(healthChecker.checkCluster()).thenReturn(Health.newHealthCheckBuilder()
      .setStatus(Health.Status.GREEN)
      .build());

    TestResponse response = underTest.newRequest().execute();

    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  public void response_contains_status_and_causes_from_HealthChecker_checkCluster() {
    Health.Status randomStatus = Health.Status.values()[random.nextInt(Health.Status.values().length)];
    String[] causes = IntStream.range(0, random.nextInt(33)).mapToObj(i -> randomAlphanumeric(4)).toArray(String[]::new);
    Health.Builder healthBuilder = Health.newHealthCheckBuilder()
      .setStatus(randomStatus);
    Arrays.stream(causes).forEach(healthBuilder::addCause);
    when(cluster.isEnabled()).thenReturn(true);
    when(healthChecker.checkCluster()).thenReturn(healthBuilder
      .build());

    WsSystem.ClusterHealthResponse clusterHealthResponse = underTest.newRequest().executeProtobuf(WsSystem.ClusterHealthResponse.class);
    assertThat(clusterHealthResponse.getHealth().name()).isEqualTo(randomStatus.name());
    assertThat(clusterHealthResponse.getCausesList())
      .extracting(WsSystem.Cause::getMessage)
      .containsOnly(causes);
  }

  @Test
  public void verify_response_example() {
    when(cluster.isEnabled()).thenReturn(true);
    when(healthChecker.checkCluster()).thenReturn(Health.newHealthCheckBuilder()
      .setStatus(Health.Status.RED)
      .addCause("Application node app-1 is RED")
      .build());

    TestResponse response = underTest.newRequest().execute();

    JsonAssert.assertJson(response.getInput())
      .isSimilarTo(underTest.getDef().responseExampleAsString());
  }
}
