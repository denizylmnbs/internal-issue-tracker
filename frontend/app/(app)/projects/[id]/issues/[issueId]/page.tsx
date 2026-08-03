import { IssueDetailClient } from "./IssueDetailClient";

export default async function IssueDetailPage({
  params,
}: {
  params: Promise<{ id: string; issueId: string }>;
}) {
  const { id, issueId } = await params;
  return <IssueDetailClient projectId={Number(id)} issueId={Number(issueId)} />;
}
