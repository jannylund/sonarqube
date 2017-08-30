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
import org.sonar.cluster.health.NodeDetails;
import org.sonar.cluster.health.NodeHealth;
import org.sonar.server.health.ClusterHealth;
import org.sonar.server.health.Health;
import org.sonar.server.health.HealthChecker;
import org.sonar.server.platform.WebServer;
import org.sonar.server.ws.WsUtils;
import org.sonarqube.ws.WsSystem;

import static java.lang.String.valueOf;
import static java.util.Comparator.comparingInt;
import static org.sonar.api.utils.DateUtils.formatDateTime;

public class ClusterHealthAction implements SystemWsAction {
  private final ClusterHealthActionSupport support;
  private final WebServer webServer;
  private final HealthChecker healthChecker;

  public ClusterHealthAction(ClusterHealthActionSupport support, WebServer webServer, HealthChecker healthChecker) {
    this.support = support;
    this.webServer = webServer;
    this.healthChecker = healthChecker;
  }

  @Override
  public void define(WebService.NewController context) {
    support.define(context, this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    if (webServer.isStandalone()) {
      response.stream().setStatus(501);
      return;
    }

    ClusterHealth check = healthChecker.checkCluster();
    WsUtils.writeProtobuf(toResponse(check), request, response);
  }

  private WsSystem.ClusterHealthResponse toResponse(ClusterHealth check) {
    WsSystem.ClusterHealthResponse.Builder responseBuilder = WsSystem.ClusterHealthResponse.newBuilder();
    WsSystem.Node.Builder nodeBuilder = WsSystem.Node.newBuilder();
    WsSystem.Cause.Builder causeBuilder = WsSystem.Cause.newBuilder();

    Health health = check.getHealth();
    responseBuilder.setHealth(WsSystem.Health.valueOf(health.getStatus().name()));
    health.getCauses().forEach(str -> responseBuilder.addCauses(toCause(str, causeBuilder)));

    check.getNodes().stream()
      .sorted(comparingInt(a -> a.getDetails().getType().ordinal()))
      .map(node -> toNode(node, nodeBuilder, causeBuilder))
      .forEach(responseBuilder::addNodes);

    return responseBuilder.build();
  }

  private static WsSystem.Node toNode(NodeHealth nodeHealth, WsSystem.Node.Builder nodeBuilder, WsSystem.Cause.Builder causeBuilder) {
    nodeBuilder.clear();
    if (nodeHealth.getDetails().getType() != NodeDetails.Type.SEARCH) {
      nodeBuilder.setHealth(WsSystem.Health.valueOf(nodeHealth.getStatus().name()));
      nodeHealth.getCauses().forEach(str -> nodeBuilder.addCauses(toCause(str, causeBuilder)));
    }
    NodeDetails details = nodeHealth.getDetails();
    nodeBuilder
      .setType(WsSystem.NodeType.valueOf(details.getType().name()))
      .setName(details.getName())
      .setHost(details.getHost())
      .setPort(valueOf(details.getPort()))
      .setStarted(formatDateTime(details.getStarted()));
    return nodeBuilder.build();
  }

  private static WsSystem.Cause toCause(String str, WsSystem.Cause.Builder causeBuilder) {
    return causeBuilder.clear().setMessage(str).build();
  }
}
