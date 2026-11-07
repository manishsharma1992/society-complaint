package com.mycity.runwal.residency.application.command;

/** The platform owner makes an add-on available to a society, or withdraws it (which also switches it off). */
public record OfferAddOnCommand(long societyId, String addOn, boolean available) {
}
