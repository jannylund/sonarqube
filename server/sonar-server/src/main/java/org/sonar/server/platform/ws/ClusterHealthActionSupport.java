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

import com.google.common.io.Resources;
import org.sonar.api.server.ws.WebService;

public class ClusterHealthActionSupport {
  public void define(WebService.NewController controller, SystemWsAction handler) {
    controller.createAction("cluster_health")
      .setDescription("Provide health status of the cluster the current SonarQube is a member of, in addition to information about all members of the cluster." +
          "<p>Require root permission or use of </p>" +
        "<p>status: the health status" +
        " <ul>" +
        " <li>GREEN: SonarQube cluster is fully operational</li>" +
        " <li>YELLOW: SonarQube cluster is operational but something must be fixed to be fully operational</li>" +
        " <li>RED: SonarQube cluster is not operational</li>" +
        " </ul>" +
        "</p>")
      .setSince("6.6")
      .setResponseExample(Resources.getResource(this.getClass(), "example-cluster-health.json"))
      .setHandler(handler);
  }
}
