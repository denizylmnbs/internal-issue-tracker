import { apiData, apiVoid, json, toQuery } from "../client";
import type {
  CreateFieldDefinitionRequest,
  FieldDefinitionResponse,
  FieldKind,
  ReorderFieldDefinitionsRequest,
  UpdateFieldDefinitionRequest,
} from "../types";

/**
 * `projectId === null` targets the two global kinds (`/api/field-definitions`,
 * PROJECT_STATUS/TEAM_FIELD, ADMIN-only to write); otherwise the six
 * project-scoped kinds under `/api/projects/{id}/field-definitions`
 * (EDITOR+ or the project's leader to write). See docs/API.md §4.14.
 */
const basePath = (projectId: number | null) =>
  projectId === null
    ? "/api/field-definitions"
    : `/api/projects/${projectId}/field-definitions`;

export const listFieldDefinitions = (
  projectId: number | null,
  kind?: FieldKind,
) =>
  apiData<FieldDefinitionResponse[]>(
    `${basePath(projectId)}${toQuery({ kind })}`,
  );

export const createFieldDefinition = (
  projectId: number | null,
  body: CreateFieldDefinitionRequest,
) =>
  apiData<FieldDefinitionResponse>(basePath(projectId), json(body, "POST"));

export const updateFieldDefinition = (
  projectId: number | null,
  defId: number,
  body: UpdateFieldDefinitionRequest,
) =>
  apiData<FieldDefinitionResponse>(
    `${basePath(projectId)}/${defId}`,
    json(body, "PUT"),
  );

export const reorderFieldDefinitions = (
  projectId: number | null,
  body: ReorderFieldDefinitionsRequest,
) =>
  apiData<FieldDefinitionResponse[]>(
    `${basePath(projectId)}/reorder`,
    json(body, "PATCH"),
  );

export const deleteFieldDefinition = (
  projectId: number | null,
  defId: number,
) => apiVoid(`${basePath(projectId)}/${defId}`, { method: "DELETE" });
