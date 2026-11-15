package com.mycity.runwal.feed.application.command;

/** Someone takes back their reaction to a published post. Only a keyed hash of the address is used. */
public record TakeBackReactionCommand(String slug, String reactorAddress) {
}
