/*
 * Copyright 2018, TeamDev. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.server.integration;

import com.google.protobuf.Value;
import io.spine.base.Error;
import io.spine.server.integration.given.ErrorQualifierTestEnv.Attribute;
import io.spine.server.integration.given.ErrorQualifierTestEnv.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.spine.server.integration.ErrorQualifier.withAttribute;
import static io.spine.server.integration.ErrorQualifier.withCode;
import static io.spine.server.integration.ErrorQualifier.withMessage;
import static io.spine.server.integration.ErrorQualifier.withType;
import static io.spine.server.integration.ErrorQualifier.withoutAttribute;
import static io.spine.server.integration.given.ErrorQualifierTestEnv.Code;
import static io.spine.server.integration.given.ErrorQualifierTestEnv.Height;
import static io.spine.server.integration.given.ErrorQualifierTestEnv.Pangram;
import static io.spine.server.integration.given.ErrorQualifierTestEnv.newError;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Mykhailo Drachuk
 */
@DisplayName("Error Qualifier should")
class ErrorQualifierTest {

    private List<Error> errors;

    @BeforeEach
    void setUp() {
        errors = asList(
                newError(Type.FIRST, Code.ZERO, Pangram.FIRST),
                newError(Type.SECOND, Code.ZERO, Pangram.SECOND),
                newError(Type.THIRD, Code.ONE, Pangram.SECOND),
                newError(Type.FOURTH, Code.TWO, Pangram.SECOND),
                newError(Attribute.HEIGHT, Height.BRYANT),
                newError(Attribute.HEIGHT, Height.LEBRON)
        );
    }

    @Test
    void filterWithType() {
        ErrorQualifier type1Qualifier = withType(Type.FIRST.value());
        List<Error> type1Errors = filterErrors(type1Qualifier);
        assertEquals(1, type1Errors.size());

        ErrorQualifier type2Qualifier = withType(Type.SECOND.value());
        List<Error> type2Errors = filterErrors(type2Qualifier);
        assertEquals(1, type2Errors.size());

        ErrorQualifier type11Qualifier = withType(Type.ELEVEN.value());
        List<Error> type11Errors = filterErrors(type11Qualifier);
        assertEquals(0, type11Errors.size());
        assertNotNull(type11Qualifier.description());
    }

    @Test
    void filterWithCode() {
        ErrorQualifier code0Qualifier = withCode(Code.ZERO.value());
        List<Error> code0Errors = filterErrors(code0Qualifier);
        assertEquals(2, code0Errors.size());

        ErrorQualifier code2Qualifier = withCode(Code.TWO.value());
        List<Error> code2Errors = filterErrors(code2Qualifier);
        assertEquals(1, code2Errors.size());

        ErrorQualifier code17Qualifier = withCode(Code.SEVENTEEN.value());
        List<Error> code17Errors = filterErrors(code17Qualifier);
        assertEquals(0, code17Errors.size());
        assertNotNull(code17Qualifier.description());
    }

    @Test
    void filterWithMessage() {
        ErrorQualifier firstMessage = withMessage(Pangram.FIRST.text());
        List<Error> firstMessageErrors = filterErrors(firstMessage);
        assertEquals(1, firstMessageErrors.size());

        ErrorQualifier secondMessage = withMessage(Pangram.SECOND.text());
        List<Error> secondMessageErrors = filterErrors(secondMessage);
        assertEquals(3, secondMessageErrors.size());

        ErrorQualifier missingMessage = withMessage(Pangram.THIRD.text());
        List<Error> missingMessageErrors = filterErrors(missingMessage);
        assertEquals(0, missingMessageErrors.size());
        assertNotNull(missingMessage.description());
    }

    @Test
    void filterWithoutAttribute() {
        ErrorQualifier missingAttribute = withoutAttribute(Attribute.WEIGHT.title());
        List<Error> errors = filterErrors(missingAttribute);

        assertEquals(6, errors.size());
        assertNotNull(missingAttribute.description());
    }

    @Test
    void filterWithAttribute() {
        ErrorQualifier withHeight = withAttribute(Attribute.HEIGHT.title());
        List<Error> errors = filterErrors(withHeight);

        assertEquals(2, errors.size());
        assertNotNull(withHeight.description());
    }

    @Test
    void filterWithAttributeValue() {
        String height = Attribute.HEIGHT.title();
        Value value = Height.BRYANT.value();
        ErrorQualifier withHeightValue = withAttribute(height).value(value);
        List<Error> errors = filterErrors(withHeightValue);

        assertEquals(1, errors.size());
        assertNotNull(withHeightValue.description());
    }

    private List<Error> filterErrors(ErrorQualifier qualifier) {
        return errors.stream()
                     .filter(qualifier::test)
                     .collect(toList());
    }
}
