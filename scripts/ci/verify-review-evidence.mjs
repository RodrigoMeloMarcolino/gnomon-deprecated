import process from "node:process";

const [owner, repo] = process.env.GITHUB_REPOSITORY.split("/");
const pr = Number(process.env.PR_NUMBER);
const sha = process.env.PR_HEAD_SHA;
const token = process.env.GITHUB_TOKEN;
const api = async (path) => {
  const response = await fetch(`https://api.github.com${path}`, {
    headers: {Accept: "application/vnd.github+json", Authorization: `Bearer ${token}`},
  });
  if (!response.ok) throw new Error(`${path}: ${response.status}`);
  return response.json();
};
const comments = await api(`/repos/${owner}/${repo}/issues/${pr}/comments?per_page=100`);
const marker = /<!--\s*GNOMON_REVIEW_EVIDENCE\s+({[^]*?})\s*-->/g;
let report;
for (const comment of comments) {
  for (const match of comment.body.matchAll(marker)) {
    try {
      const parsed = JSON.parse(match[1]);
      if (parsed.sha === sha) report = {parsed, login: comment.user.login};
    } catch { /* malformed evidence is not evidence */ }
  }
}
if (!report) throw new Error(`no valid GNOMON_REVIEW_EVIDENCE for ${sha}`);
const p = report.parsed;
if (![p.quality, p.security, p.architecture].every((value) => value === "PASS")) throw new Error("review pass failed");
if ((p.findings?.critical ?? 0) !== 0 || (p.findings?.high ?? 0) !== 0) throw new Error("blocking review findings");
if ((p.findings?.medium ?? 0) !== 0) throw new Error("medium findings require a valid risk-register entry; report zero only after recording it");
const acceptance = comments.find((comment) => comment.body.trim() === `/accept-review ${sha}`);
if (!acceptance) throw new Error(`no owner acceptance for ${sha}`);
const permission = await api(`/repos/${owner}/${repo}/collaborators/${acceptance.user.login}/permission`);
if (permission.user?.permissions?.admin !== true) throw new Error("review acceptance author is not a repository owner");
console.log(`review evidence accepted for ${sha}: reviewer=${report.login}, owner=${acceptance.user.login}`);
