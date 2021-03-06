package ejava.examples.ejbwar.inventory;

import java.io.IOException;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import ejava.examples.ejbwar.customer.client.CustomerClient;
import ejava.examples.ejbwar.customer.client.CustomerClientImpl;
import ejava.examples.ejbwar.inventory.client.InventoryClient;
import ejava.examples.ejbwar.inventory.client.InventoryClientImpl;

/**
 * This class implements the details of the test configuration and 
 * objects required to operate the test.
 */
public class InventoryTestConfig {
	private HttpClient httpClient;
	private URI appURI;
	private InventoryClient inventoryClient;
	private CustomerClient customerClient;
	private Properties props = new Properties();
	
	/**
	 * Accept an optional property file resource from the classpath
	 * so that we can override hard-coded defaults with a property file.
	 * @param resource
	 * @throws IOException
	 */
	public InventoryTestConfig(String resource) throws IOException {
		InputStream is = getClass().getResourceAsStream(resource);
		if (is!=null) {
			try {
				props.load(is);
			} finally {
				is.close();
			}
		}
	}
	
	/**
	 * Returns a singleton connection manager for use with HttpClient
	 * HTTP client library.
	 * @return
	 */
	public HttpClient httpClient() {
		if (httpClient==null) {
			httpClient = new DefaultHttpClient();
		}
		return httpClient;
	}

	/**
	 * Return the base URI to the application
	 * @return
	 */
	public URI appURI() {
		if (appURI==null) {
			try {
				String host=props.getProperty("host", "localhost");
				int port=Integer.parseInt(props.getProperty("port", "8080"));
				String path=props.getProperty("servletContext", "/");
				URL url=new URL("http", host, port, path);
				appURI = url.toURI();
			} catch (MalformedURLException ex) {
				throw new RuntimeException("error creating URL:" + ex, ex);
			} catch (URISyntaxException ex) {
				throw new RuntimeException("error creating URI:" + ex, ex);
			} finally {}
		}
		return appURI;
	}
	
	/**
	 * Returns a remote stub (implemented with JAX-RS) to the 
	 * inventory management application.
	 * @return
	 */
	public InventoryClient inventoryClient() {
		if (inventoryClient==null) {
			InventoryClient client = new InventoryClientImpl();
			((InventoryClientImpl)client).setHttpClient(httpClient());
			((InventoryClientImpl)client).setAppURI(appURI());
			inventoryClient = client;
		}
		return inventoryClient;
	}
	
	/**
	 * Returns a remote stub (implemented with JAX-RS) to the integrated
	 * external EJB module.
	 * @return
	 */
	public CustomerClient customerClient() {
		if (customerClient == null) {
			CustomerClient client = new CustomerClientImpl();
			((CustomerClientImpl)client).setHttpClient(httpClient());
			((CustomerClientImpl)client).setAppURI(appURI());
			customerClient = client;
		}
		return customerClient;
	}
}
