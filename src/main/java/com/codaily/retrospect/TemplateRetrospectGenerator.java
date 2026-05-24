package com.codaily.retrospect;

import com.codaily.github.CommitSummary;
import com.codaily.github.PullRequestSummary;
import com.codaily.github.RepositoryActivity;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TemplateRetrospectGenerator implements RetrospectGenerator {

    @Override
    public String generate(RepositoryActivity activity, RetrospectGenerationOptions options) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# ").append(activity.repoFullName()).append(" 회고").append("\n\n");
        markdown.append("생성 시각: ").append(activity.fetchedAt()).append("\n\n");

        markdown.append("## 이번 작업에서 보인 흐름").append("\n");
        markdown.append("- 최근 커밋 ").append(activity.commits().size()).append("건과 PR ").append(activity.pullRequests().size()).append("건을 기준으로 정리했다.").append("\n");
        markdown.append("- 백엔드 API와 공유 흐름을 먼저 완성하고, 확장 기능은 뒤에 붙이기 쉬운 구조로 가는 편이 합리적이다.").append("\n\n");

        markdown.append("## 최근 커밋").append("\n");
        appendCommits(markdown, activity.commits());
        markdown.append("\n");

        markdown.append("## 최근 PR").append("\n");
        appendPullRequests(markdown, activity.pullRequests());
        markdown.append("\n");

        markdown.append("## 잘한 점").append("\n");
        markdown.append("- 구현 범위를 `GitHub 수집 -> 회고 생성 -> 공유` 흐름으로 좁혀 MVP 완성 가능성을 높였다.").append("\n");
        markdown.append("- 커밋과 PR 기준으로 회고 근거를 남겨서 산출물 설명력이 좋아졌다.").append("\n\n");

        markdown.append("## 아쉬운 점").append("\n");
        markdown.append("- 실제 AI 호출과 운영 캐시는 아직 연결되지 않아 현재는 템플릿 기반 생성기와 메모리 캐시로 동작한다.").append("\n");
        markdown.append("- GitHub OAuth 미설정 환경에서는 데모 모드 또는 PAT 방식에 의존한다.").append("\n\n");

        markdown.append("## 다음 액션").append("\n");
        markdown.append("- AI 생성기를 실제 모델 API 호출 구현으로 교체한다.").append("\n");
        markdown.append("- PostgreSQL/Redis를 붙여 공유 페이지와 랭킹 조회를 운영형으로 전환한다.").append("\n");
        markdown.append("- 크롬 익스텐션에서 현재 API를 호출하도록 연결한다.").append("\n\n");

        markdown.append("## 이번 회고의 포커스").append("\n");
        if (options.focus() == null || options.focus().isBlank()) {
            markdown.append("- 기본 포커스: 이번 주 작업 흐름, 병목, 다음 액션을 명확히 정리한다.").append("\n");
        } else {
            markdown.append("- 사용자 포커스: ").append(options.focus()).append("\n");
        }

        return markdown.toString();
    }

    private void appendCommits(StringBuilder markdown, List<CommitSummary> commits) {
        if (commits.isEmpty()) {
            markdown.append("- 커밋 데이터가 없다.").append("\n");
            return;
        }

        for (CommitSummary commit : commits) {
            markdown
                .append("- `")
                .append(shortSha(commit.sha()))
                .append("` ")
                .append(commit.message())
                .append(" (")
                .append(commit.authorName())
                .append(")")
                .append("\n");
        }
    }

    private void appendPullRequests(StringBuilder markdown, List<PullRequestSummary> pullRequests) {
        if (pullRequests.isEmpty()) {
            markdown.append("- PR 데이터가 없다.").append("\n");
            return;
        }

        for (PullRequestSummary pullRequest : pullRequests) {
            markdown
                .append("- #")
                .append(pullRequest.number())
                .append(" ")
                .append(pullRequest.title())
                .append(" [")
                .append(pullRequest.state())
                .append("]")
                .append("\n");
        }
    }

    private String shortSha(String sha) {
        if (sha == null || sha.isBlank()) {
            return "unknown";
        }
        return sha.length() <= 7 ? sha : sha.substring(0, 7);
    }
}
