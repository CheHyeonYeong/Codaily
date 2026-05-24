package com.codaily.retrospect;

import com.codaily.github.CommitSummary;
import com.codaily.github.PullRequestSummary;
import com.codaily.github.RepositoryActivity;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RetrospectPromptBuilder {

    public String buildInstructions() {
        return """
            You are an engineering retrospective assistant.
            Write only markdown in Korean.
            Do not invent commits or pull requests that are not in the input.
            Use this structure exactly:
            # {repo} 회고
            ## 이번 작업 요약
            ## 잘한 점
            ## 아쉬운 점
            ## 다음 액션
            ## 포커스 반영
            """;
    }

    public String buildInput(RepositoryActivity activity, String focus) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Repository: ").append(activity.repoFullName()).append("\n");
        prompt.append("Focus: ").append(
            StringUtils.hasText(focus)
                ? focus
                : "이번 작업 흐름과 다음 액션을 구체적으로 정리해줘"
        ).append("\n\n");

        prompt.append("Recent commits:\n");
        appendCommits(prompt, activity.commits());
        prompt.append("\n");

        prompt.append("Recent pull requests:\n");
        appendPullRequests(prompt, activity.pullRequests());
        return prompt.toString();
    }

    private void appendCommits(StringBuilder prompt, List<CommitSummary> commits) {
        if (commits.isEmpty()) {
            prompt.append("- none\n");
            return;
        }

        for (CommitSummary commit : commits) {
            prompt
                .append("- ")
                .append(shortSha(commit.sha()))
                .append(" | ")
                .append(commit.message())
                .append(" | author=")
                .append(commit.authorName())
                .append(" | committedAt=")
                .append(commit.committedAt())
                .append("\n");
        }
    }

    private void appendPullRequests(StringBuilder prompt, List<PullRequestSummary> pullRequests) {
        if (pullRequests.isEmpty()) {
            prompt.append("- none\n");
            return;
        }

        for (PullRequestSummary pullRequest : pullRequests) {
            prompt
                .append("- #")
                .append(pullRequest.number())
                .append(" | ")
                .append(pullRequest.title())
                .append(" | state=")
                .append(pullRequest.state())
                .append(" | author=")
                .append(pullRequest.authorName())
                .append(" | updatedAt=")
                .append(pullRequest.updatedAt())
                .append("\n");
        }
    }

    private String shortSha(String sha) {
        if (!StringUtils.hasText(sha)) {
            return "unknown";
        }
        return sha.length() <= 7 ? sha : sha.substring(0, 7);
    }
}
