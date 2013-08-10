package ar.com.synergian.wagongit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.maven.scm.log.ScmLogger;
import org.apache.maven.wagon.AbstractWagon;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.observers.Debug;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.StringUtils;

public class GitWagon extends AbstractWagon {

	private final boolean debug = Utils.getBooleanEnvironmentProperty("wagon.git.debug");

	private final ScmLogger log = new GitWagonLog(debug);

	private GitBackend git = null;

	public GitWagon() {

		if (debug) {
			Debug d = new Debug();
			addSessionListener(d);
			addTransferListener(d);
		}

	}

	/**
	 * Required by plexus. But ignored.
	 */
	public void setSshExecutable(String sshExecutable) {
		// Ignore.
	}

	/**
	 * Required by plexus. But ignored.
	 */
	public void setScpExecutable(String scpExecutable) {
		// Ignore.
	}

	/**
	 * Required by plexus. But ignored.
	 */
	public void setHttpHeaders(Properties httpHeaders) {
		// Ignore.
	}

	/**
	 * {@inheritDoc}
	 */
	protected void openConnectionInternal() throws ConnectionException, AuthenticationException {

		log.debug("Invoked openConnectionInternal()");

		if (git == null) {
			try {

				File workDir = Utils.createCheckoutDirectory();

				if (!workDir.mkdirs())
					throw new ConnectionException("Unable to create working directory");

				String remote = getRepository().getUrl();

				if (remote.endsWith("/"))
					remote = remote.substring(0, remote.length() - 1);

				git = new GitBackend(workDir, remote, log);
				git.pullAll();
			} catch (Exception e) {
				throw new ConnectionException("Unable to pull git repository: " + e.getMessage(), e);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	protected void closeConnection() throws ConnectionException {

		log.debug("Invoked closeConnection()");

		try {
			git.pushAll();
		} catch (Exception e) {
			throw new ConnectionException("Unable to push git repostory: " + e.getMessage(), e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void put(File source, String destination) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {

		log.debug("Invoked put(" + source.getAbsolutePath() + ", " + destination + ")");

		String resourceName = StringUtils.replace(destination, "\\", "/");
		Resource resource = new Resource(resourceName);

		firePutInitiated(resource, source);
		firePutStarted(resource, source);

		try {

			transfer(resource, source, new FileOutputStream(new File(git.workDir, destination)), true);
			git.dirty();

		} catch (IOException e) {
			fireTransferError(resource, e, TransferEvent.REQUEST_PUT);
			throw new TransferFailedException("Unable to put file", e);
		}

		firePutCompleted(resource, source);
	}

	/**
	 * {@inheritDoc}
	 */
	public void get(String resourceName, File localFile) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {

		log.debug("Invoked get(" + resourceName + ", " + localFile.getAbsolutePath() + ")");

		Resource resource = new Resource(resourceName);

		fireGetInitiated(resource, localFile);
		fireGetStarted(resource, localFile);

		try {

			File remote = new File(git.workDir, resource.getName());
			if (!remote.exists())
				throw new IOException("Remote file doesn't exist: " + resourceName);

			transfer(resource, new FileInputStream(remote), new FileOutputStream(localFile), TransferEvent.REQUEST_GET);

		} catch (Exception e) {
			fireTransferError(resource, e, TransferEvent.REQUEST_GET);
			throw new TransferFailedException("Unable to get file", e);
		}

		fireGetCompleted(resource, localFile);
	}

	/**
	 * {@inheritDoc}
	 */
	public List getFileList(String destinationDirectory) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {

		log.debug("Invoked getFileList(" + destinationDirectory + ")");
		log.warn("getFileList not supported");

		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean resourceExists(String resourceName) throws TransferFailedException, AuthorizationException {

		log.debug("Invoked resourceExists(" + resourceName + ")");
		log.warn("resourceExists not supported");

		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean supportsDirectoryCopy() {

		log.debug("Invoked supportsDirectoryCopy()");

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public void putDirectory(File sourceDirectory, String destinationDirectory) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {

		log.debug("Invoked putDirectory(" + sourceDirectory.getAbsolutePath() + ", " + destinationDirectory + ")");

		String resourceName = StringUtils.replace(destinationDirectory, "\\", "/");
		Resource resource = new Resource(resourceName);

		firePutInitiated(resource, sourceDirectory);
		firePutStarted(resource, sourceDirectory);

		try {
			git.putDirectory(sourceDirectory, destinationDirectory);
			git.pushAll();
		} catch (Exception e) {
			fireTransferError(resource, e, TransferEvent.REQUEST_PUT);
			throw new TransferFailedException("Unable to put file", e);
		}

		firePutCompleted(resource, sourceDirectory);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean getIfNewer(String resourceName, File destination, long timestamp) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {

		log.debug("Invoked getIfNewer(" + resourceName + ", " + destination.getAbsolutePath() + ", " + timestamp + ")");
		log.warn("getIfNewer not supported");

		return true;
	}

}
