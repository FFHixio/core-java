/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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
package org.spine3.protobuf;

import com.google.protobuf.*;
import org.spine3.util.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;

/**
 * Utility class for working with {@link Message} objects.
 *
 * @author Mikhail Melnik
 * @author Mikhail Mikhaylov
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("UtilityClass")
public class Messages {

    private static final String GETTER_METHOD_PREFIX = "get";

    private static final String METHOD_PARSE_FROM = "parseFrom";
    private static final String METHOD_GET_DESCRIPTOR = "getDescriptor";

    private static final char UNDERSCORE_PROPERTY_NAME_SEPARATOR = '_';

    private static final String MSG_NO_SUCH_METHOD = "Method %s() is not defined in the class %s.";
    private static final String MSG_UNABLE_TO_ACCESS = "Method %s() is not accessible in the class %s.";
    private static final String MSG_ERROR_INVOKING = "Error invoking %s() of the class %s: %s";

    private Messages() {
        // Prevent instantiation.
    }

    /**
     * Wraps {@link Message} object inside of {@link Any} instance.
     * The protobuf fully qualified name is used as typeUrl param,
     * {@link Message#toByteString()} is used to get the {@link ByteString} message representation.
     *
     * @param message message that should be put inside the {@link Any} instance.
     * @return the instance of {@link Any} object that wraps given message.
     */
    public static Any toAny(Message message) {
        checkNotNull(message);

        Any result = Any.newBuilder()
                .setTypeUrl(message.getDescriptorForType().getFullName())
                .setValue(message.toByteString())
                .build();

        return result;
    }

    /**
     * Unwraps {@link Any} instance object to {@link Message}
     * that was put inside it and returns as the instance of object
     * with described by {@link Any#getTypeUrl()}.
     * <p/>
     * NOTE: This is temporary solution and should be reworked in the future
     * when Protobuf provides means for working with {@link Any}.
     *
     * @param any instance of {@link Any} that should be unwrapped
     * @param <T> descendant of {@link Message} class that is used as the return type for this method
     * @return unwrapped instance of {@link Message} descendant that were put inside of given {@link Any} object
     */
    @SuppressWarnings("ProhibitedExceptionThrown")
    public static <T extends Message> T fromAny(Any any) {
        checkNotNull(any);

        final TypeName typeName = TypeName.of(any.getTypeUrl());
        Class<T> messageClass;
        String messageClassName = StringTypeValue.NULL;
        try {
            messageClass = toMessageClass(typeName);
            messageClassName = messageClass.getName();
            Method method = messageClass.getMethod(METHOD_PARSE_FROM, ByteString.class);

            //noinspection unchecked
            T result = (T) method.invoke(null, any.getValue());
            return result;
        } catch (ClassNotFoundException ignored) {
            throw new UnknownTypeInAnyException(typeName.toString());
        } catch (NoSuchMethodException e) {
            String msg = String.format(MSG_NO_SUCH_METHOD, METHOD_PARSE_FROM, messageClassName);
            throw new Error(msg, e);
        } catch (IllegalAccessException e) {
            String msg = String.format(MSG_UNABLE_TO_ACCESS, METHOD_PARSE_FROM, messageClassName);
            throw new Error(msg, e);
        } catch (InvocationTargetException e) {
            String msg = String.format(MSG_ERROR_INVOKING, METHOD_PARSE_FROM, messageClassName, e.getCause());
            throw new Error(msg, e);
        }
    }

    /**
     * Returns message {@link Class} for the given Protobuf message type.
     * <p/>
     * This method is temporary until full support of {@link Any} is provided.
     *
     * @param messageType full type name defined in the proto files
     * @return message class
     * @throws ClassNotFoundException in case there is no corresponding class for the given Protobuf message type
     * @see #fromAny(Any) that uses the same convention
     */
    public static <T extends Message> Class<T> toMessageClass(TypeName messageType) throws ClassNotFoundException {
        ClassName className = TypeToClassMap.get(messageType);
        @SuppressWarnings("unchecked")
        final Class<T> result = (Class<T>) Class.forName(className.value());
        return result;
    }


    //TODO:2015-07-27:alexander.yevsyukov: Why are we having this AND toJson()?
    /**
     * Prints the passed message into to Json representation.
     *
     * @param message the message object
     * @return string representation of the passed message
     */
    public static String toString(Message message) {
        checkNotNull(message);
        final String result = JsonFormat.printToString(message);
        return result;
    }

    /**
     * Prints the passed message into well formatted text.
     *
     * @param message the message object
     * @return text representation of the passed message
     */
    public static String toText(Message message) {
        checkNotNull(message);
        final String result = TextFormat.printToString(message);
        return result;
    }

    /**
     * Converts passed message into Json representation.
     *
     * @param message the message object
     * @return Json string
     */
    public static String toJson(Message message) {
        checkNotNull(message);
        final String result = JsonFormat.printToString(message);
        return result;
    }

    /**
     * Obtains Protobuf field name for the passed message.
     *
     * @param msg a message to inspect
     * @param index   a zero-based index of the field
     * @return name of the field
     */
    @SuppressWarnings("TypeMayBeWeakened") // Enforce type for API clarity.
    public static String getFieldName(Message msg, int index) {
        final Descriptors.FieldDescriptor fieldDescriptor = getField(msg, index);
        final String fieldName = fieldDescriptor.getName();
        return fieldName;
    }

    static Descriptors.FieldDescriptor getField(MessageOrBuilder msg, int fieldIndex) {
        final Descriptors.FieldDescriptor result = msg.getDescriptorForType().getFields().get(fieldIndex);
        return result;
    }

    /**
     * Converts Protobuf field name into Java accessor method name.
     */
    public static String toAccessorMethodName(CharSequence fieldName) {
        StringBuilder out = new StringBuilder(checkNotNull(fieldName).length() + 3);
        out.append(GETTER_METHOD_PREFIX);
        final char uppercaseFirstChar = Character.toUpperCase(fieldName.charAt(0));
        out.append(uppercaseFirstChar);

        boolean nextUpperCase = false;
        for (int i = 1; i < fieldName.length(); i++) {
            char c = fieldName.charAt(i);
            if (UNDERSCORE_PROPERTY_NAME_SEPARATOR == c) {
                nextUpperCase = true;
                continue;
            }
            out.append(nextUpperCase ? Character.toUpperCase(c) : c);
            nextUpperCase = false;
        }

        return out.toString();
    }

    /**
     * Reads field from the passed message by its index.
     *
     * @param command    a message to inspect
     * @param fieldIndex a zero-based index of the field
     * @return value a value of the field
     */
    @SuppressWarnings("TypeMayBeWeakened") // We are likely to work with already built instances.
    public static Object getFieldValue(Message command, int fieldIndex) {

        //TODO:2015-07-27:alexander.yevsyukov: We need to cache this kind of calculations as they are slow.
        // For this we need a value object FieldAccessor parameterized with a message class and a field index.

        final Descriptors.FieldDescriptor fieldDescriptor = getField(command, fieldIndex);
        final String fieldName = fieldDescriptor.getName();
        final String methodName = toAccessorMethodName(fieldName);

        try {
            Method method = command.getClass().getMethod(methodName);
            Object result = method.invoke(command);
            return result;
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw propagate(e);
        }
    }

    public static Descriptors.Descriptor getClassDescriptor(Class<? extends Message> clazz) {
        try {
            final Method method = clazz.getMethod(METHOD_GET_DESCRIPTOR);
            final Descriptors.Descriptor result = (Descriptors.Descriptor) method.invoke(null);
            return result;
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
            throw new MissingDescriptorException(clazz, e.getCause());
        }
    }

}
