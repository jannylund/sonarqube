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
package org.sonar.cluster;

import java.util.Properties;

public final class ClusterProperties {
  public static final String CLUSTER_ENABLED = "sonar.cluster.enabled";
  public static final String CLUSTER_CE_DISABLED = "sonar.cluster.ce.disabled";
  public static final String CLUSTER_SEARCH_DISABLED = "sonar.cluster.search.disabled";
  public static final String CLUSTER_SEARCH_HOSTS = "sonar.cluster.search.hosts";
  public static final String CLUSTER_WEB_DISABLED = "sonar.cluster.web.disabled";
  public static final String CLUSTER_HOSTS = "sonar.cluster.hosts";
  public static final String CLUSTER_PORT = "sonar.cluster.port";
  public static final String CLUSTER_NETWORK_INTERFACES = "sonar.cluster.networkInterfaces";
  public static final String CLUSTER_NAME = "sonar.cluster.name";
  public static final String HAZELCAST_LOG_LEVEL = "sonar.log.level.app.hazelcast";
  public static final String CLUSTER_WEB_LEADER = "sonar.cluster.web.startupLeader";
  // Internal property used by sonar-application to share the local endpoint of Hazelcast
  public static final String CLUSTER_LOCALENDPOINT = "sonar.cluster.hazelcast.localEndPoint";
  // Internal property used by sonar-application to share the local UUID of the Hazelcast member
  public static final String CLUSTER_MEMBERUUID = "sonar.cluster.hazelcast.memberUUID";

  private ClusterProperties() {
    // prevents instantiation
  }

  public static void putClusterDefaults(Properties properties) {
    properties.put(CLUSTER_ENABLED, "false");
    properties.put(CLUSTER_CE_DISABLED, "false");
    properties.put(CLUSTER_WEB_DISABLED, "false");
    properties.put(CLUSTER_SEARCH_DISABLED, "false");
    properties.put(CLUSTER_NAME, "sonarqube");
    properties.put(CLUSTER_NETWORK_INTERFACES, "");
    properties.put(CLUSTER_HOSTS, "");
    properties.put(CLUSTER_PORT, "9003");
    properties.put(HAZELCAST_LOG_LEVEL, "WARN");
  }
}
