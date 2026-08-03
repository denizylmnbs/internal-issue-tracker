import { TeamDetailClient } from "./TeamDetailClient";

export default async function TeamDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <TeamDetailClient teamId={Number(id)} />;
}
