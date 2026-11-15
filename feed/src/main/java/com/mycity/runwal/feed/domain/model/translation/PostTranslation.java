package com.mycity.runwal.feed.domain.model.translation;

import com.mycity.runwal.feed.domain.exception.InvalidPostException;
import com.mycity.runwal.feed.domain.model.post.Post;
import com.mycity.runwal.feed.domain.model.post.PostId;
import com.mycity.runwal.shared.ddd.AbstractAggregateRoot;
import com.mycity.runwal.shared.ddd.annotation.AggregateRoot;
import java.time.Instant;
import java.util.Objects;

/**
 * A published post's title and text machine-translated into one language. Made once per post and language and
 * kept, so readers never pay for the same translation twice. The author's own words stay the original.
 * Its own aggregate, so that translating never loads or locks the post. No length limit: a translation can be
 * longer than the original (Devanagari often is).
 */
@AggregateRoot
public class PostTranslation extends AbstractAggregateRoot {

    private final PostId postId;
    private final Language language;
    private final String title;
    private final String body;
    private final Instant translatedAt;

    private PostTranslation(PostId postId, Language language, String title, String body, Instant translatedAt) {
        this.postId = Objects.requireNonNull(postId, "postId");
        this.language = Objects.requireNonNull(language, "language");
        if (title == null || title.isBlank() || body == null || body.isBlank()) {
            throw new InvalidPostException("A translation needs a title and a text");
        }
        this.title = title;
        this.body = body;
        this.translatedAt = Objects.requireNonNull(translatedAt, "translatedAt");
    }

    /** Only published posts are translated: nobody else can read them. */
    public static PostTranslation of(Post post, Language language, String title, String body, Instant at) {
        if (!post.isPublished()) {
            throw new InvalidPostException("Only published posts can be translated");
        }
        return new PostTranslation(post.id(), language, title, body, at);
    }

    /** A translation as stored earlier. */
    public static PostTranslation reconstitute(PostId postId, Language language, String title, String body,
                                               Instant translatedAt) {
        return new PostTranslation(postId, language, title, body, translatedAt);
    }

    public PostId postId() {
        return postId;
    }

    public Language language() {
        return language;
    }

    public String title() {
        return title;
    }

    public String body() {
        return body;
    }

    public Instant translatedAt() {
        return translatedAt;
    }
}
