package com.adl.et.telco.dte.mvno.plugin.tmf.application.controller;

import com.adl.et.telco.dte.mvno.plugin.tmf.application.exception.type.FilterException;
import com.adl.et.telco.dte.mvno.plugin.tmf.application.transport.response.transformers.ResponseTransformer;
import com.adl.et.telco.dte.mvno.plugin.tmf.application.validator.RequestEntityValidator;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.BaseResourceDocument;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.Filter;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.FilterOperation;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.Page;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.Pageable;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.service.BaseResourceService;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers how the query endpoint turns request parameters into filters and pagination.
 */
class BaseResourceControllerQueryTest {

    @SuppressWarnings("unchecked")
    private final BaseResourceService<Doc> service = mock(BaseResourceService.class);

    private final HttpServletRequest request = mock(HttpServletRequest.class);

    private TestController controller;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ResponseTransformer<Doc> transformer = mock(ResponseTransformer.class);
        when(transformer.transform(any(Doc.class))).thenReturn(Collections.emptyMap());

        when(service.query(any(), any(), any())).thenReturn(new Page<>(Collections.emptyList(), 0));

        controller = new TestController(service, transformer, mock(RequestEntityValidator.class));
    }

    @Test
    void operatorSuffixesAreTranslatedIntoFilterOperations() {
        controller.query(params("state", "active", "priority.gt", "3", "priority.gte", "4",
                "priority.lt", "9", "priority.lte", "8", "name.regex", "^ab", "code.eq", "X"),
                null, null, null, request);

        assertThat(capturedFilters())
                .extracting(Filter::getField, Filter::getOperation)
                .containsExactlyInAnyOrder(
                        tuple("state", FilterOperation.IS),
                        tuple("priority", FilterOperation.GT),
                        tuple("priority", FilterOperation.GTE),
                        tuple("priority", FilterOperation.LT),
                        tuple("priority", FilterOperation.LTE),
                        tuple("name", FilterOperation.REGEX),
                        tuple("code", FilterOperation.IS));
    }

    @Test
    void paginationAndFieldParametersAreNotTreatedAsFilters() {
        controller.query(params("fields", "id", "offset", "0", "limit", "10", "state", "active",
                "blank", ""), "id", "0", "10", request);

        assertThat(capturedFilters())
                .extracting(Filter::getField)
                .containsExactly("state");
    }

    @Test
    void aLimitOnItsOwnStillPaginates() {
        controller.query(params(), null, null, "10", request);

        Pageable pageable = capturedPageable();
        assertThat(pageable.isPagination()).isTrue();
        assertThat(pageable.getOffset()).isZero();
        assertThat(pageable.getLimit()).isEqualTo(10);
    }

    @Test
    void anOffsetOnItsOwnStillPaginates() {
        controller.query(params(), null, "20", null, request);

        Pageable pageable = capturedPageable();
        assertThat(pageable.isPagination()).isTrue();
        assertThat(pageable.getOffset()).isEqualTo(20);
        assertThat(pageable.getLimit()).isZero();
    }

    @Test
    void withoutPaginationParametersPaginationStaysDisabled() {
        controller.query(params(), null, null, null, request);

        assertThat(capturedPageable().isPagination()).isFalse();
    }

    @Test
    void invalidPaginationParametersAreRejectedAsBadRequests() {
        assertThatThrownBy(() -> controller.query(params(), null, "abc", "10", request))
                .isInstanceOf(FilterException.class)
                .hasMessageContaining("offset");

        assertThatThrownBy(() -> controller.query(params(), null, "0", "-1", request))
                .isInstanceOf(FilterException.class)
                .hasMessageContaining("must not be negative");
    }

    @Test
    void nestedAndBlankFieldSelectionsAreDropped() {
        controller.query(params(), "id , , product.id, name", null, null, request);

        ArgumentCaptor<String> fields = ArgumentCaptor.forClass(String.class);
        verify(service).query(any(), fields.capture(), any());

        assertThat(fields.getValue()).isEqualTo("id,name");
    }

    private List<Filter> capturedFilters() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Filter>> captor = ArgumentCaptor.forClass(List.class);
        verify(service).query(captor.capture(), any(), any());
        return captor.getValue();
    }

    private Pageable capturedPageable() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).query(any(), any(), captor.capture());
        return captor.getValue();
    }

    private static Map<String, String> params(String... keyValues) {
        Map<String, String> params = new LinkedHashMap<>();

        for (int i = 0; i < keyValues.length; i += 2) {
            params.put(keyValues[i], keyValues[i + 1]);
        }

        return params;
    }

    private static class Doc extends BaseResourceDocument {
    }

    private static class TestController extends BaseResourceController<Doc, Doc, Doc> {

        TestController(BaseResourceService<Doc> service, ResponseTransformer<Doc> transformer,
                       RequestEntityValidator validator) {
            super(service, transformer, validator, Doc.class, Doc.class);
        }
    }
}
