package net.packsam.geolocatefx.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 * Class for reading and writing the application configuration from disk.
 *
 * @author osterrath
 */
public class ConfigurationIO {

	/**
	 * Reads the users configuration file.
	 *
	 * @return configuration file
	 * @throws IOException
	 * 		could not read XML file
	 */
	public Configuration readConfiguration() throws IOException {
		File cfgFile = getConfigurationFile();
		if (cfgFile.exists()) {
			if (cfgFile.isFile() && cfgFile.canRead()) {
				try {
					// parse file
					JAXBContext ctx = createJAXBContext();
					Unmarshaller unmarshaller = ctx.createUnmarshaller();
					return (Configuration) unmarshaller.unmarshal(cfgFile);
				} catch (JAXBException e) {
					throw new IOException("Could not read configuration file", e);
				}
			} else {
				// unreadable
				throw new IOException("Could not read configuration file " + cfgFile.getAbsolutePath());
			}
		} else {
			// return empty configuration
			return new Configuration();
		}

	}

	/**
	 * Writes the configuration to the users configuration file.
	 *
	 * @param cfg
	 * 		configuration to write
	 * @throws IOException
	 * 		could not write configuration file
	 */
	public void writeConfiguration(Configuration cfg) throws IOException {
		File cfgFile = getConfigurationFile();
		if (cfgFile.exists() && !(cfgFile.isFile() && cfgFile.canWrite())) {
			// unwritable
			throw new IOException("Could not write configuration file " + cfgFile.getAbsolutePath());
		}
		try {
			JAXBContext ctx = createJAXBContext();
			Marshaller marshaller = ctx.createMarshaller();
			marshaller.marshal(cfg, cfgFile);
		} catch (JAXBException e) {
			throw new IOException("Could not write configuration file", e);
		}

		// make file hidden
		try {
			Files.setAttribute(cfgFile.toPath(), "dos:hidden", Boolean.TRUE, LinkOption.NOFOLLOW_LINKS);
		} catch (IOException e) {
			// ignore errors
		}
	}

	/**
	 * Creates the JAXB context for reading and writing configuration files.
	 *
	 * @return JAXB context
	 * @throws JAXBException
	 * 		could not initialize JAXB system
	 */
	private JAXBContext createJAXBContext() throws JAXBException {
		return JAXBContext.newInstance(Configuration.class);
	}

	/**
	 * Returns the configuration file to read from and write to.
	 *
	 * @return configuration file
	 */
	private File getConfigurationFile() {
		String userHome = System.getProperty("user.home");
		return new File(userHome, ".GeolocationFx.config");
	}
}
