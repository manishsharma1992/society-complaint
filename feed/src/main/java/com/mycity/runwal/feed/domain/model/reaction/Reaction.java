package com.mycity.runwal.feed.domain.model.reaction;

import com.mycity.runwal.feed.domain.exception.InvalidPostException;
import com.mycity.runwal.feed.domain.model.post.Post;
import com.mycity.runwal.feed.domain.model.post.PostId;
import com.mycity.runwal.shared.ddd.AbstractAggregateRoot;
import com.mycity.runwal.shared.ddd.annotation.AggregateRoot;
import java.time.Instant;
import java.util.Objects;

/**
 * Someone's emoji on a published post. Anyone can react, even without logging in; each reactor has at most one
 * reaction per post (identity: the post and the reactor key), and reacting again replaces it.
 * Its own aggregate, so that reacting never loads or locks the post.
 */
@AggregateRoot
public class Reaction extends AbstractAggregateRoot {

    private final PostId postId;
    private final String reactorKey;
    private final Emoji emoji;
    private final Instant reactedAt;

    private Reaction(PostId postId, String reactorKey, Emoji emoji, Instant reactedAt) {
        this.postId = Objects.requireNonNull(postId, "postId");
        if (reactorKey == null || reactorKey.isBlank() || reactorKey.length() > 100) {
            throw new InvalidPostException("A reaction needs a reactor key of 1 to 100 characters");
        }
        this.reactorKey = reactorKey;
        this.emoji = Objects.requireNonNull(emoji, "emoji");
        this.reactedAt = Objects.requireNonNull(reactedAt, "reactedAt");
    }

    /**
     * Someone reacts to a post; only published posts take reactions.
     *
     * @param reactorKey an opaque, keyed hash that tells reactors apart without identifying them
     */
    public static Reaction to(Post post, String reactorKey, Emoji emoji, Instant at) {
        if (!post.isPublished()) {
            throw new InvalidPostException("Only published posts can be reacted to");
        }
        return new Reaction(post.id(), reactorKey, emoji, at);
    }

    public PostId postId() {
        return postId;
    }

    public String reactorKey() {
        return reactorKey;
    }

    public Emoji emoji() {
        return emoji;
    }

    public Instant reactedAt() {
        return reactedAt;
    }
}
