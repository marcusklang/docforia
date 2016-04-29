package se.lth.cs.docforia.io;
/*
 * Copyright 2016 Marcus Klang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import se.lth.cs.docforia.Document;
import se.lth.cs.docforia.DocumentStorageLevel;

import java.io.File;
import java.nio.ByteBuffer;

/**
 * Document I/O
 */
public interface DocumentIO {
	/**
	 * Get the document as json
	 * @param doc the document
	 * @return json representation of the document
	 */
	String toJson(Document doc, DocumentStorageLevel opt);

	/**
	 * Convert from json to document
	 * @param json the json string
	 * @return document
	 * @throws java.io.IOError in case of error
	 */
	Document fromJson(String json);

	/**
	 * Convert to bytes
	 * @param doc the document
	 * @return byte array of the document
	 */
	byte[] toBytes(Document doc, DocumentStorageLevel opt);

	/**
	 * Convert from bytes to document
	 * @param bytes all bytes for the document
	 * @return document
	 * @throws java.io.IOError in case of error
	 */
	Document fromBytes(byte[] bytes);

	/**
	 * Convert from bytes from a specific portion of the buffer
	 * @param bytes
	 * @param offset
	 * @param length
	 * @return
	 */
	Document fromBytes(byte[] bytes, int offset, int length);

	/**
	 * Convert from buffer to document
	 * @param buffer the buffer, reads to limit
	 * @return document
	 * @throws java.io.IOError
	 */
	Document fromBuffer(ByteBuffer buffer);

	/**
	 * Create a file reader for documents stored in its native format
	 * @param location the location
	 * @return reader
	 * @throws java.io.IOError in case of IO errors.
	 */
	DocumentReader createReader(File location);

	/**
	 * Create a file writer for documents stored in its native format
	 * @param location the location
	 * @return java.io.IOError in case of IO errors.
	 */
	DocumentWriter createWriter(File location);
}
