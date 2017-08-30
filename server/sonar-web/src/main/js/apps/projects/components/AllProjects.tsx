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
import * as React from 'react';
import * as PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import PageHeader from './PageHeader';
import ProjectsListContainer from './ProjectsListContainer';
import PageSidebar from './PageSidebar';
import VisualizationsContainer from '../visualizations/VisualizationsContainer';
import { parseUrlQuery } from '../store/utils';
import ListFooter from '../../../components/controls/ListFooter';
import { translate } from '../../../helpers/l10n';
import * as utils from '../utils';
import * as storage from '../../../helpers/storage';
import { RawQuery } from '../../../helpers/query';
import '../styles.css';
import { Project } from '../types';

interface Props {
  isFavorite: boolean;
  location: { pathname: string; query: { [x: string]: string } };
  organization?: { key: string };
}

interface State {
  loading: boolean;
  projects?: Project[];
  query: RawQuery;
  total?: number;
}

export default class AllProjects extends React.PureComponent<Props, State> {
  mounted: boolean;
  state: State = { loading: true, query: {} };

  static contextTypes = {
    currentUser: PropTypes.object.isRequired,
    router: PropTypes.object.isRequired
  };

  componentDidMount() {
    this.mounted = true;
    this.handleQueryChange(true);
    const footer = document.getElementById('footer');
    footer && footer.classList.add('page-footer-with-sidebar');
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.location.query !== this.props.location.query) {
      this.handleQueryChange(false);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    const footer = document.getElementById('footer');
    footer && footer.classList.remove('page-footer-with-sidebar');
  }

  getView = () => this.state.query.view || 'overall';

  getVisualization = () => this.state.query.visualization || 'risk';

  getSort = () => this.state.query.sort || 'name';

  isFiltered = () => Object.keys(this.state.query).some(key => this.state.query[key] != null);

  fetchMoreProjects = () => {
    // FIXME
  };

  getSavedOptions = () => {
    const options: {
      sort?: string;
      view?: string;
      visualization?: string;
    } = {};
    if (storage.getSort()) {
      options.sort = storage.getSort() || undefined;
    }
    if (storage.getView()) {
      options.view = storage.getView() || undefined;
    }
    if (storage.getVisualization()) {
      options.visualization = storage.getVisualization() || undefined;
    }
    return options;
  };

  handlePerspectiveChange = ({ view, visualization }: { view: string; visualization?: string }) => {
    const query: {
      view: string | undefined;
      visualization: string | undefined;
      sort?: string | undefined;
    } = {
      view: view === 'overall' ? undefined : view,
      visualization
    };

    if (this.state.query.view === 'leak' || view === 'leak') {
      if (this.state.query.sort) {
        const sort = utils.parseSorting(this.state.query.sort);
        if (utils.SORTING_SWITCH[sort.sortValue]) {
          query.sort = (sort.sortDesc ? '-' : '') + utils.SORTING_SWITCH[sort.sortValue];
        }
      }
      this.context.router.push({ pathname: this.props.location.pathname, query });
    } else {
      this.updateLocationQuery(query);
    }

    storage.saveSort(query.sort);
    storage.saveView(query.view);
    storage.saveVisualization(visualization);
  };

  handleSortChange = (sort: string, desc: boolean) => {
    const asString = (desc ? '-' : '') + sort;
    this.updateLocationQuery({ sort: asString });
    storage.saveSort(asString);
  };

  handleQueryChange(initialMount: boolean) {
    const query = parseUrlQuery(this.props.location.query);
    const savedOptions = this.getSavedOptions();
    const savedOptionsSet = savedOptions.sort || savedOptions.view || savedOptions.visualization;

    // if there is no filter, but there are saved preferences in the localStorage
    if (initialMount && !this.isFiltered() && savedOptionsSet) {
      this.context.router.replace({ pathname: this.props.location.pathname, query: savedOptions });
    } else {
      this.setState({ loading: true, query });
      utils
        .fetchProjects(
          query,
          this.props.isFavorite,
          this.props.organization && this.props.organization.key
        )
        .then(response => {
          if (this.mounted) {
            this.setState({
              loading: false,
              projects: response.projects,
              total: response.total
            });
          }
        });
    }
  }

  updateLocationQuery = (newQuery: RawQuery) => {
    this.context.router.push({
      pathname: this.props.location.pathname,
      query: {
        ...this.props.location.query,
        ...newQuery
      }
    });
  };

  renderSide = () => (
    <div className="layout-page-side-outer">
      <div
        className="layout-page-side projects-page-side"
        style={{ top: this.props.organization ? 95 : 30 }}>
        <div className="layout-page-side-inner">
          <div className="layout-page-filters">
            <PageSidebar
              isFavorite={this.props.isFavorite}
              organization={this.props.organization}
              query={this.state.query}
              view={this.getView()}
              visualization={this.getVisualization()}
            />
          </div>
        </div>
      </div>
    </div>
  );

  renderHeader = () => (
    <div className="layout-page-header-panel layout-page-main-header">
      <div className="layout-page-header-panel-inner layout-page-main-header-inner">
        <div className="layout-page-main-inner">
          {this.state.projects && (
            <PageHeader
              currentUser={this.context.currentUser}
              isFavorite={this.props.isFavorite}
              loading={this.state.loading}
              onPerspectiveChange={this.handlePerspectiveChange}
              onSortChange={this.handleSortChange}
              organization={this.props.organization}
              projects={this.state.projects}
              query={this.state.query}
              selectedSort={this.getSort()}
              total={this.state.total}
              view={this.getView()}
              visualization={this.getVisualization()}
            />
          )}
        </div>
      </div>
    </div>
  );

  renderMain = () =>
    this.getView() === 'visualizations' ? (
      <div className="layout-page-main-inner">
        <VisualizationsContainer
          sort={this.state.query.sort}
          visualization={this.getVisualization()}
        />
      </div>
    ) : (
      <div className="layout-page-main-inner">
        <ProjectsListContainer
          isFavorite={this.props.isFavorite}
          isFiltered={this.isFiltered()}
          organization={this.props.organization}
          cardType={this.getView()}
        />
        <ListFooter
          count={this.state.projects != undefined ? this.state.projects.length : 0}
          loadMore={this.fetchMoreProjects}
          ready={!this.state.loading}
          total={this.state.total != undefined ? this.state.total : 0}
        />
      </div>
    );

  render() {
    return (
      <div className="layout-page projects-page">
        <Helmet title={translate('projects.page')} />

        {this.renderSide()}

        <div className="layout-page-main projects-page-content">
          {this.renderHeader()}
          {this.renderMain()}
        </div>
      </div>
    );
  }
}
