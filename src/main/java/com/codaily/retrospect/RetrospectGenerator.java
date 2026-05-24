package com.codaily.retrospect;

import com.codaily.github.RepositoryActivity;

public interface RetrospectGenerator {

    String generate(RepositoryActivity activity, RetrospectGenerationOptions options);
}
