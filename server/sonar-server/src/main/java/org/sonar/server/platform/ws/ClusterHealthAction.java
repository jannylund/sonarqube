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

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.health.Health;
import org.sonar.server.health.HealthChecker;
import org.sonar.server.platform.cluster.Cluster;
import org.sonar.server.ws.WsUtils;
import org.sonarqube.ws.WsSystem;

public class ClusterHealthAction implements SystemWsAction {
  private final ClusterHealthActionSupport support;
  private final Cluster cluster;
  private final HealthChecker healthChecker;

  public ClusterHealthAction(ClusterHealthActionSupport support, Cluster cluster, HealthChecker healthChecker) {
    this.support = support;
    this.cluster = cluster;
    this.healthChecker = healthChecker;
  }

  @Override
  public void define(WebService.NewController context) {
    support.define(context, this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    if (!cluster.isEnabled()) {
      response.stream().setStatus(501);
      return;
    }

    Health check = healthChecker.checkCluster();
    WsSystem.ClusterHealthResponse.Builder responseBuilder = WsSystem.ClusterHealthResponse.newBuilder()
        .setHealth(WsSystem.Health.valueOf(check.getStatus().name()));
    WsSystem.Cause.Builder causeBuilder = WsSystem.Cause.newBuilder();
    check.getCauses().forEach(str -> responseBuilder.addCauses(causeBuilder.clear().setMessage(str).build()));

    WsUtils.writeProtobuf(responseBuilder.build(), request, response);
  }
}
