import { ProjectLayoutClient } from "./ProjectLayoutClient";

export default async function ProjectLayout({
  children,
  params,
}: {
  children: React.ReactNode;
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  return <ProjectLayoutClient projectId={Number(id)}>{children}</ProjectLayoutClient>;
}
