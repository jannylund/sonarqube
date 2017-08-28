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
package org.sonar.server.health;

import java.util.Arrays;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.sonar.server.health.Health.newHealthCheckBuilder;

/**
 * Implementation of {@link HealthChecker} that executes implementations of {@link NodeHealthCheck} in the container
 * and aggregates their results.
 */
public class HealthCheckerImpl implements HealthChecker {
  private final List<NodeHealthCheck> nodeHealthChecks;
  private final List<ClusterHealthCheck> clusterHealthChecks;

  public HealthCheckerImpl(NodeHealthCheck... nodeHealthChecks) {
    this.nodeHealthChecks = Arrays.asList(nodeHealthChecks);
    this.clusterHealthChecks = emptyList();
  }

  public HealthCheckerImpl(ClusterHealthCheck... clusterHealthChecks) {
    this.clusterHealthChecks = Arrays.asList(clusterHealthChecks);
    this.nodeHealthChecks = emptyList();
  }

  public HealthCheckerImpl(NodeHealthCheck[] nodeHealthChecks, ClusterHealthCheck[] clusterHealthChecks) {
    this.nodeHealthChecks = Arrays.asList(nodeHealthChecks);
    this.clusterHealthChecks = Arrays.asList(clusterHealthChecks);
  }

  @Override
  public Health checkNode() {
    return nodeHealthChecks.stream().map(NodeHealthCheck::check)
      .reduce(Health.GREEN, HealthReducer.INSTANCE);
  }

  @Override
  public Health checkCluster() {
    return clusterHealthChecks.stream().map(ClusterHealthCheck::check)
      .reduce(Health.GREEN, HealthReducer.INSTANCE);
  }

  private enum HealthReducer implements BinaryOperator<Health> {
    INSTANCE;

    /**
     * According to Javadoc, {@link BinaryOperator} used in method
     * {@link java.util.stream.Stream#reduce(Object, BinaryOperator)} is supposed to be stateless.
     *
     * But as we are sure this {@link BinaryOperator} won't be used on a Stream with {@link Stream#parallel()}
     * feature on, we allow ourselves this optimisation.
     */
    private final Health.Builder builder = newHealthCheckBuilder();

    @Override
    public Health apply(Health left, Health right) {
      builder.clear();
      builder.setStatus(worseOf(left.getStatus(), right.getStatus()));
      left.getCauses().forEach(builder::addCause);
      right.getCauses().forEach(builder::addCause);
      return builder.build();
    }

    private static Health.Status worseOf(Health.Status left, Health.Status right) {
      if (left == right) {
        return left;
      }
      if (left.ordinal() > right.ordinal()) {
        return left;
      }
      return right;
    }
  }
}
