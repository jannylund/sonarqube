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

import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;

public class SafeModeClusterHealthActionTest {
  private WsActionTester underTest = new WsActionTester(new SafeModeClusterHealthAction(new ClusterHealthActionSupport()));

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
  public void response_is_always_501() {
    TestResponse response = underTest.newRequest().execute();

    assertThat(response.getStatus()).isEqualTo(501);
    assertThat(response.getInput()).isEmpty();
  }
}
