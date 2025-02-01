package com.upokecenter.rdf;
/*
Any copyright to this work is released to the Public Domain.
In case this is not possible, this work is also
licensed under the Unlicense: https://unlicense.org/

 */

  /**
   * <p>Exception thrown for errors that occur while parsing data. </p> <p>This
   * library may throw exceptions of this type in certain cases, notably when
   * errors occur, and may supply messages to those exceptions (the message can
   * be accessed through the {@code Message} property in.NET or the {@code
   * getMessage()} method in Java). These messages are intended to be read by
   * humans to help diagnose the error (or other cause of the exception); they
   * are not intended to be parsed by computer programs, and the exact text of
   * the messages may change at any time between versions of this library.</p>
   */

  public final class ParserException extends RuntimeException {
private static final long serialVersionUID = 1L;
    /**
     * Initializes a new instance of the {@link
     * com.upokecenter.rdf.ParserException} class.
     */
    public ParserException() {
    }

    /**
     * Initializes a new instance of the {@link
     * com.upokecenter.rdf.ParserException} class.
     * @param message The parameter {@code message} is a text string.
     */
    public ParserException(String message) {
 super(message);
    }

    /**
     * Initializes a new instance of the {@link
     * com.upokecenter.rdf.ParserException} class. Uses the specified message and
     * inner exception.
     * @param message The parameter {@code message} is a text string.
     * @param innerException The parameter {@code innerException} is an Exception
     * object.
     */
    public ParserException(String message, Throwable innerException) {
 super(message);
initCause(innerException);;
    }
  }
