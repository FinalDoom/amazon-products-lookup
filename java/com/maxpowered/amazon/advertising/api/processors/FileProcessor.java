package com.maxpowered.amazon.advertising.api.processors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.amazon.webservices.awsecommerceservice._2013_08_01.Item;
import com.google.common.io.Files;
import com.maxpowered.amazon.advertising.api.Constants;

public class FileProcessor implements Processor, AutoCloseable {
	private static final Logger LOG = LoggerFactory.getLogger(FileProcessor.class);

	private static Marshaller marshaller;
	static {
		try {
			marshaller = JAXBContext.newInstance(Constants.API_PACKAGE).createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
		} catch (final JAXBException ex) {
			LOG.error("Error creating marshaller", ex);
		}
	}

	private boolean started = false;
	private File outputFile;
	private OutputStream outputStream;

	@Autowired
	FileProcessor(@Value("${app.output}") final String outputFile) throws IOException {
		setOutputFile(outputFile);
	}

	@Override
	public void writeItem(final Item item) throws JAXBException, IOException {
		if (!started) {
			rollOutputFileIntoStream();
			IOUtils.write("<Items>", outputStream, StandardCharsets.UTF_8);
			started = true;
		}
		marshaller.marshal(item, outputStream);
	}

	public void setOutputFile(final String outputFile) throws IOException {
		outputStream = null;
		this.outputFile = new File(outputFile);
	}

	public void setOutputStream(final OutputStream outputStream) {
		outputFile = null;
		this.outputStream = outputStream;
	}

	private void rollOutputFileIntoStream() throws IOException {
		if (outputFile == null) {
			return;
		}
		if (outputFile.exists()) {
			// Roll over files
			rollFile(outputFile, 1);
		}
		outputFile.createNewFile();
		outputStream = new FileOutputStream(outputFile);
	}

	private static void rollFile(final File file, final int index) throws IOException {
		final File newFile = new File(file.getAbsolutePath() + "." + index);
		if (newFile.exists()) {
			rollFile(file, index + 1);
		}
		if (index == 1) {
			// Move non .# file to .1
			Files.move(file, newFile);
			LOG.debug("Moved file {} to {}", file, newFile);
		} else {
			// Move .1 to .2 or .2 to .3, etc.
			final File movedFile = new File(file.getAbsolutePath() + "." + (index - 1));
			Files.move(movedFile, newFile);
			LOG.debug("Moved file {} to {}", movedFile, newFile);
		}
	}

	@Override
	public void close() throws IOException {
		IOUtils.write("</Items>", outputStream, StandardCharsets.UTF_8);
		outputStream.close();
	}

}
