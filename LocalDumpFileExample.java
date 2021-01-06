package examples;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.wikidata.wdtk.dumpfiles.DumpContentType;
import org.wikidata.wdtk.dumpfiles.DumpProcessingController;
import org.wikidata.wdtk.dumpfiles.EntityTimerProcessor;
import org.wikidata.wdtk.dumpfiles.MwLocalDumpFile;
import org.wikidata.wdtk.rdf.PropertyRegister;
import org.wikidata.wdtk.rdf.RdfSerializer;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.wikidata.wdtk.datamodel.interfaces.Sites;

/*
 * #%L
 * Wikidata Toolkit Examples
 * %%
 * Copyright (C) 2014 - 2015 Wikidata Toolkit Developers
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


/**
 * This class illustrates how to process local dumpfiles. It uses
 * {@link EntityTimerProcessor} to process a dump.
 *
 * @author Markus Damm
 */
public class LocalDumpFileExample {

	/**
	 * Path to the dump that should be processed
	 */
	private final static String DUMP_FILE = "/home/ralf/wikidata/molbio-slice.gz";

	public static void main(String[] args) throws IOException {
		ExampleHelpers.configureLogging();
		LocalDumpFileExample.printDocumentation();

		// Prepare a compressed output stream to write the data to
		// (admittedly, this is slightly over-optimized for an example)
		OutputStream bufferedFileOutputStream = new BufferedOutputStream(
				ExampleHelpers
						.openExampleFileOuputStream("test.nt.gz"),
				1024 * 1024 * 5);
		GzipParameters gzipParameters = new GzipParameters();
		gzipParameters.setCompressionLevel(7);
		OutputStream compressorOutputStream = new GzipCompressorOutputStream(
				bufferedFileOutputStream, gzipParameters);
		OutputStream exportOutputStream = asynchronousOutputStream(compressorOutputStream);

		// Create a serializer processor
		RdfSerializer serializer = new RdfSerializer(RDFFormat.NTRIPLES,
				exportOutputStream, null,
				PropertyRegister.getWikidataPropertyRegister());
		// Serialize statements (and nothing else) for all items
		serializer.setTasks(RdfSerializer.TASK_ITEMS
				| RdfSerializer.TASK_STATEMENTS);
		serializer.open();

		DumpProcessingController dumpProcessingController = new DumpProcessingController(
				"wikidatawiki");
		// Note that the project name "wikidatawiki" is only for online access;
		// not relevant here.
		Sites sites = null;
		dumpProcessingController.setOfflineMode(ExampleHelpers.OFFLINE_MODE);

		EntityTimerProcessor entityTimerProcessor = new EntityTimerProcessor(0);
		dumpProcessingController.registerEntityDocumentProcessor(
				entityTimerProcessor, null, true);
		dumpProcessingController.registerEntityDocumentProcessor(
				serializer, null, true);

		// Select local file (meta-data will be guessed):
		System.out.println();
		System.out.println("Processing a local dump file giving only its location");
		System.out.println("(meta-data like the date is guessed from the file name):");
		MwLocalDumpFile mwDumpFile = new MwLocalDumpFile(DUMP_FILE);
		dumpProcessingController.processDump(mwDumpFile);

		entityTimerProcessor.close();
		
		serializer.close();

	}

	/**
	 * Prints some basic documentation about this program.
	 */
	public static void printDocumentation() {
		System.out.println("********************************************************************");
//		System.out.println("*** Wikidata Toolkit: LocalDumpFileExample");
//		System.out.println("*** ");
//		System.out.println("*** This program illustrates how to process local dumps.");
//		System.out.println("*** It uses an EntityTimerProcesses which counts processed items");
//		System.out.println("*** and elapsed time.");
//		System.out.println("*** ");
//		System.out.println("*** See source code for further details.");
//		System.out.println("********************************************************************");
	}

	/**
	 * Creates a separate thread for writing into the given output stream and
	 * returns a pipe output stream that can be used to pass data to this
	 * thread.
	 * <p>
	 * This code is inspired by
	 * http://stackoverflow.com/questions/12532073/gzipoutputstream
	 * -that-does-its-compression-in-a-separate-thread
	 *
	 * @param outputStream the stream to write to in the thread
	 * @return a new stream that data should be written to
	 * @throws IOException if the pipes could not be created for some reason
	 */
	public static OutputStream asynchronousOutputStream(
			final OutputStream outputStream) throws IOException {
		final int SIZE = 1024 * 1024 * 10;
		final PipedOutputStream pos = new PipedOutputStream();
		final PipedInputStream pis = new PipedInputStream(pos, SIZE);
		new Thread(() -> {
			try {
				byte[] bytes = new byte[SIZE];
				for (int len; (len = pis.read(bytes)) > 0; ) {
					outputStream.write(bytes, 0, len);
				}
			} catch (IOException ioException) {
				ioException.printStackTrace();
			} finally {
				close(pis);
				close(outputStream);
			}
		}, "async-output-stream").start();
		return pos;
	}

	/**
	 * Closes a Closeable and swallows any exceptions that might occur in the
	 * process.
	 */
	static void close(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException ignored) {
			}
		}
	}
}
