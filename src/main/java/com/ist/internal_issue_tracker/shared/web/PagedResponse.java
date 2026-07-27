package com.ist.internal_issue_tracker.shared.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Stable page contract this application owns, instead of serializing Spring Data's {@code
 * Page}/{@code PageImpl} directly (its JSON shape is not a guaranteed contract across Spring Data
 * versions). Composes with the response envelope as {@code ApiResponse<PagedResponse<T>>}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PagedResponse<T>(List<T> content, PageMeta page) {

  public static <T> PagedResponse<T> from(Page<T> page) {
    return new PagedResponse<>(
        page.getContent(),
        new PageMeta(
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast()));
  }

  /** Maps an entity page to a DTO page in one step. */
  public static <E, T> PagedResponse<T> from(Page<E> page, Function<E, T> mapper) {
    return from(page.map(mapper));
  }

  public record PageMeta(
      int number, int size, long totalElements, int totalPages, boolean first, boolean last) {}
}
