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
import { translate } from '../../helpers/l10n';
import { RawQuery } from '../../helpers/query';
import { convertToFilter, convertToSorting } from './store/utils';
import { RequestData } from '../../helpers/request';
import { searchProjects } from '../../api/components';
import { getMeasuresForProjects } from '../../api/measures';
import { isDiffMetric, getPeriodValue } from '../../helpers/measures';

interface SortingOption {
  class?: string;
  value: string;
}

export const SORTING_METRICS: SortingOption[] = [
  { value: 'name' },
  { value: 'analysis_date' },
  { value: 'reliability' },
  { value: 'security' },
  { value: 'maintainability' },
  { value: 'coverage' },
  { value: 'duplications' },
  { value: 'size' }
];

export const SORTING_LEAK_METRICS: SortingOption[] = [
  { value: 'name' },
  { value: 'analysis_date' },
  { value: 'new_reliability', class: 'projects-leak-sorting-option' },
  { value: 'new_security', class: 'projects-leak-sorting-option' },
  { value: 'new_maintainability', class: 'projects-leak-sorting-option' },
  { value: 'new_coverage', class: 'projects-leak-sorting-option' },
  { value: 'new_duplications', class: 'projects-leak-sorting-option' },
  { value: 'new_lines', class: 'projects-leak-sorting-option' }
];

export const SORTING_SWITCH: { [x: string]: string } = {
  analysis_date: 'analysis_date',
  name: 'name',
  reliability: 'new_reliability',
  security: 'new_security',
  maintainability: 'new_maintainability',
  coverage: 'new_coverage',
  duplications: 'new_duplications',
  size: 'new_lines',
  new_reliability: 'reliability',
  new_security: 'security',
  new_maintainability: 'maintainability',
  new_coverage: 'coverage',
  new_duplications: 'duplications',
  new_lines: 'size'
};

export const VIEWS = ['overall', 'leak'];

export const VISUALIZATIONS = [
  'risk',
  'reliability',
  'security',
  'maintainability',
  'coverage',
  'duplications'
];

export const PAGE_SIZE = 50;
export const PAGE_SIZE_VISUALIZATIONS = 99;

export const METRICS = [
  'alert_status',
  'reliability_rating',
  'security_rating',
  'sqale_rating',
  'duplicated_lines_density',
  'coverage',
  'ncloc',
  'ncloc_language_distribution'
];

export const LEAK_METRICS = [
  'alert_status',
  'new_bugs',
  'new_reliability_rating',
  'new_vulnerabilities',
  'new_security_rating',
  'new_code_smells',
  'new_maintainability_rating',
  'new_coverage',
  'new_duplicated_lines_density',
  'new_lines'
];

export const METRICS_BY_VISUALIZATION: { [x: string]: string[] } = {
  risk: ['reliability_rating', 'security_rating', 'coverage', 'ncloc', 'sqale_index'],
  // x, y, size, color
  reliability: ['ncloc', 'reliability_remediation_effort', 'bugs', 'reliability_rating'],
  security: ['ncloc', 'security_remediation_effort', 'vulnerabilities', 'security_rating'],
  maintainability: ['ncloc', 'sqale_index', 'code_smells', 'sqale_rating'],
  coverage: ['complexity', 'coverage', 'uncovered_lines'],
  duplications: ['ncloc', 'duplicated_lines', 'duplicated_blocks']
};

export const FACETS = [
  'reliability_rating',
  'security_rating',
  'sqale_rating',
  'coverage',
  'duplicated_lines_density',
  'ncloc',
  'alert_status',
  'languages',
  'tags'
];

export const LEAK_FACETS = [
  'new_reliability_rating',
  'new_security_rating',
  'new_maintainability_rating',
  'new_coverage',
  'new_duplicated_lines_density',
  'new_lines',
  'alert_status',
  'languages',
  'tags'
];

export function localizeSorting(sort?: string): string {
  return translate('projects.sort', sort || 'name');
}

export function parseSorting(sort: string): { sortValue: string; sortDesc: boolean } {
  const desc = sort[0] === '-';
  return { sortValue: desc ? sort.substr(1) : sort, sortDesc: desc };
}

export function defineMetrics(query: RawQuery): string[] {
  switch (query.view) {
    case 'visualizations':
      return METRICS_BY_VISUALIZATION[query.visualization || 'risk'];
    case 'leak':
      return LEAK_METRICS;
    default:
      return METRICS;
  }
}

export function defineFacets(query: RawQuery): string[] {
  if (query.view === 'leak') {
    return LEAK_FACETS;
  }
  return FACETS;
}

export const convertToQueryData = (
  query: RawQuery,
  isFavorite: boolean,
  organization?: string,
  defaultData: RawQuery = {}
) => {
  const data: RequestData = { ...defaultData, organization };
  const filter = convertToFilter(query, isFavorite);
  const sort = convertToSorting(query as any);

  if (filter) {
    data.filter = filter;
  }
  if (sort.s) {
    data.s = sort.s;
  }
  if (sort.hasOwnProperty('asc')) {
    data.asc = sort.asc;
  }
  return data;
};

export function fetchProjects(query: RawQuery, isFavorite: boolean, organization?: string) {
  const ps = query.view === 'visualizations' ? PAGE_SIZE_VISUALIZATIONS : PAGE_SIZE;
  const data = convertToQueryData(query, isFavorite, organization, {
    ps,
    facets: defineFacets(query).join(),
    f: 'analysisDate,leakPeriodDate'
  });
  return searchProjects(data).then(({ components, paging }) => {
    return fetchProjectMeasures(components, query).then(measures => {
      return {
        projects: components.map(component => {
          const componentMeasures: { [key: string]: string } = {};
          measures.filter(measure => measure.component === component.key).forEach(measure => {
            const value = isDiffMetric(measure.metric) ? getPeriodValue(measure, 1) : measure.value;
            if (value != undefined) {
              componentMeasures[measure.metric] = value;
            }
          });
          return { ...component, measures: componentMeasures, organization: undefined };
        }),
        total: paging.total
      };
    });
  });
}

export function fetchProjectMeasures(projects: Array<{ key: string }>, query: RawQuery) {
  if (!projects.length) {
    return Promise.resolve([]);
  }

  const projectKeys = projects.map(project => project.key);
  const metrics = defineMetrics(query);
  return getMeasuresForProjects(projectKeys, metrics);
}
