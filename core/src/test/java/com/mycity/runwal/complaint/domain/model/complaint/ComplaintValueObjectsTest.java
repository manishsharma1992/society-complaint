package com.mycity.runwal.complaint.domain.model.complaint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.complaint.domain.exception.InvalidComplaintException;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ComplaintValueObjectsTest {

    @Test
    void flatComplaintNeedsFlatAndTower() {
        assertThatThrownBy(() -> ComplaintLocation.flat(null, new FlatId(1204)))
                .isInstanceOf(InvalidComplaintException.class);
        assertThatThrownBy(() -> ComplaintLocation.flat(new TowerId(3), null))
                .isInstanceOf(InvalidComplaintException.class);
    }

    @Test
    void commonAreaComplaintHasNoFlatAndAnOptionalTower() {
        assertThat(ComplaintLocation.commonArea(null).towerId()).isNull();
        assertThat(ComplaintLocation.commonArea(new TowerId(3)).towerId()).isEqualTo(new TowerId(3));
        assertThatThrownBy(() -> new ComplaintLocation(ComplaintScope.COMMON_AREA, null, new FlatId(1204)))
                .isInstanceOf(InvalidComplaintException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void titleMustNotBeBlank(String title) {
        assertThatThrownBy(() -> new ComplaintTitle(title)).isInstanceOf(InvalidComplaintException.class);
    }

    @Test
    void titleIsTrimmedAndLimited() {
        assertThat(new ComplaintTitle(" Lift stuck ").value()).isEqualTo("Lift stuck");
        assertThat(new ComplaintTitle("x".repeat(ComplaintTitle.MAX_LENGTH))).isNotNull();
        assertThatThrownBy(() -> new ComplaintTitle("x".repeat(ComplaintTitle.MAX_LENGTH + 1)))
                .isInstanceOf(InvalidComplaintException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "OPEN, ASSIGNED, true", "OPEN, REJECTED, true", "OPEN, IN_PROGRESS, false",
            "ASSIGNED, IN_PROGRESS, true", "ASSIGNED, REJECTED, true", "ASSIGNED, OPEN, false",
            "IN_PROGRESS, RESOLVED, true", "IN_PROGRESS, REJECTED, false",
            "RESOLVED, CLOSED, true", "RESOLVED, OPEN, false",
            "CLOSED, OPEN, false", "REJECTED, OPEN, false"})
    void statusTransitions(ComplaintStatus from, ComplaintStatus to, boolean allowed) {
        assertThat(from.canMoveTo(to)).isEqualTo(allowed);
    }

    @Test
    void unknownStatusOrScopeIsADomainError() {
        assertThat(ComplaintStatus.from("RESOLVED")).isEqualTo(ComplaintStatus.RESOLVED);
        assertThatThrownBy(() -> ComplaintStatus.from("DONE")).isInstanceOf(InvalidComplaintException.class);
        assertThatThrownBy(() -> ComplaintScope.from(null)).isInstanceOf(InvalidComplaintException.class);
    }

    @ParameterizedTest
    @CsvSource({"a.jpg, image/jpeg", "a.png, image/png", "a.webp, image/webp"})
    void photoAcceptsCommonImageTypesUpToTheSizeLimit(String fileName, String contentType) {
        assertThat(photo("complaints/1/7/" + fileName, contentType, ComplaintPhoto.MAX_SIZE_BYTES)).isNotNull();
        assertThat(ComplaintPhoto.contentTypeOf("complaints/1/7/" + fileName)).isEqualTo(contentType);
    }

    @Test
    void photoRejectsOtherTypesSizesAndMismatchedExtensions() {
        assertThatThrownBy(() -> photo("complaints/1/7/a.gif", "image/gif", 1000))
                .isInstanceOf(InvalidComplaintException.class)
                .hasMessage("A photo must be JPEG, PNG or WebP");
        assertThatThrownBy(() -> photo("complaints/1/7/a.jpg", "image/jpeg", ComplaintPhoto.MAX_SIZE_BYTES + 1))
                .isInstanceOf(InvalidComplaintException.class)
                .hasMessage("A photo can be at most 5 MB");
        assertThatThrownBy(() -> photo("complaints/1/7/a.jpg", "image/jpeg", 0))
                .isInstanceOf(InvalidComplaintException.class);
        assertThatThrownBy(() -> photo("complaints/1/7/a.png", "image/jpeg", 1000))
                .isInstanceOf(InvalidComplaintException.class)
                .hasMessage("The photo's file extension does not match its type image/jpeg");
        assertThatThrownBy(() -> ComplaintPhoto.contentTypeOf("complaints/1/7/a.exe"))
                .isInstanceOf(InvalidComplaintException.class)
                .hasMessage("A photo must be JPEG, PNG or WebP");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"complaints/1/7/../8/a.jpg", "complaints/1/7/a b.jpg", "https://evil.example/a.jpg"})
    void photoKeyMustBeAPlainStorageKey(String key) {
        assertThatThrownBy(() -> photo(key, "image/jpeg", 1000))
                .isInstanceOf(InvalidComplaintException.class)
                .hasMessage("Invalid photo storage key");
    }

    private static ComplaintPhoto photo(String key, String contentType, long size) {
        return new ComplaintPhoto(key, contentType, size, new UserId(10), Instant.parse("2026-10-25T08:00:00Z"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void commentNeedsText(String text) {
        assertThatThrownBy(() -> new ComplaintComment(1, new UserId(10), text, Instant.parse("2026-10-25T08:00:00Z")))
                .isInstanceOf(InvalidComplaintException.class)
                .hasMessage("A comment needs some text");
    }

    @Test
    void commentIsTrimmedAndLimited() {
        Instant at = Instant.parse("2026-10-25T08:00:00Z");
        assertThat(new ComplaintComment(1, new UserId(10), " ok ", at).text()).isEqualTo("ok");
        assertThatThrownBy(() -> new ComplaintComment(1, new UserId(10), "x".repeat(ComplaintComment.MAX_LENGTH + 1), at))
                .isInstanceOf(InvalidComplaintException.class);
    }
}
