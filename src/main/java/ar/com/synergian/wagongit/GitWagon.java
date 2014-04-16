package ar.com.synergian.wagongit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

public class GitWagon extends AbstractWagon {

	private final boolean debug = Utils.getBooleanEnvironmentProperty("wagon.git.debug");
	private final boolean safeCheckout = Utils.getBooleanEnvironmentProperty("wagon.git.safe.checkout");

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

				String url = getRepository().getUrl();

				if (url.endsWith("/"))
					url = url.substring(0, url.length() - 1);

				String remote;
				String branch;

				url = url.substring("git:".length());
				int i = url.indexOf(':');
				if (i < 0) {
					remote = url;
					branch = "master";
				} else {
					branch = url.substring(0, i);
					remote = url.substring(i + 3, url.length());
				}

				File workDir = Utils.createCheckoutDirectory(remote);

				if (!workDir.exists() || !workDir.isDirectory() || !workDir.canWrite())
					throw new ConnectionException("Unable to create working directory");

				if (safeCheckout)
					FileUtils.cleanDirectory(workDir);

				git = new GitBackend(workDir, remote, branch, log);
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

			if (safeCheckout)
				FileUtils.cleanDirectory(git.workDir);

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

			File file = new File(git.workDir, destination);
			file.getParentFile().mkdirs();
			transfer(resource, source, new FileOutputStream(file), true);

		} catch (Exception e) {
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

		File remote = new File(git.workDir, resource.getName());
		if (!remote.exists())
			return;

		fireGetInitiated(resource, localFile);
		try {
			fireGetStarted(resource, localFile);

			transfer(resource, new FileInputStream(remote), new FileOutputStream(localFile), TransferEvent.REQUEST_GET);

			fireGetCompleted(resource, localFile);
		} catch (Exception e) {
			fireTransferError(resource, e, TransferEvent.REQUEST_GET);
			throw new TransferFailedException("Unable to get file", e);
		}
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

		return new File(git.workDir, resourceName).exists();
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
