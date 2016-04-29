package ar.com.synergian.wagongit;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.maven.scm.log.ScmLogger;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.InputData;
import org.apache.maven.wagon.LazyFileOutputStream;
import org.apache.maven.wagon.OutputData;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.StreamWagon;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.observers.Debug;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.FileUtils;

public class GitWagon extends StreamWagon {

	private final boolean debug = Utils.getBooleanEnvironmentProperty("wagon.git.debug");
	private final boolean safeCheckout = Utils.getBooleanEnvironmentProperty("wagon.git.safe.checkout");
	private final boolean skipEmptyCommit = Utils.getBooleanEnvironmentProperty("wagon.git.skip.empty.commit");

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
	 * {@inheritDoc}
	 */
	public void fillInputData(InputData inputData) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {

		log.debug("Invoked fillInputData()");

		Resource resource = inputData.getResource();

		File file = new File(git.workDir, resource.getName());

		if (!file.exists()) {
			throw new ResourceDoesNotExistException("File: " + file + " does not exist");
		}

		try {
			InputStream in = new BufferedInputStream(new FileInputStream(file));

			inputData.setInputStream(in);

			resource.setContentLength(file.length());

			resource.setLastModified(file.lastModified());
		} catch (FileNotFoundException e) {
			throw new TransferFailedException("Could not read from file: " + file.getAbsolutePath(), e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void fillOutputData(OutputData outputData) throws TransferFailedException {

		log.debug("Invoked fillOutputData()");

		Resource resource = outputData.getResource();

		File file = new File(git.workDir, resource.getName());

		createParentDirectories(file);

		OutputStream outputStream = new BufferedOutputStream(new LazyFileOutputStream(file));

		outputData.setOutputStream(outputStream);
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
	public void closeConnection() throws ConnectionException {

		log.debug("Invoked closeConnection()");

		try {

			git.pushAll(skipEmptyCommit);

			if (safeCheckout)
				FileUtils.cleanDirectory(git.workDir);

		} catch (Exception e) {
			throw new ConnectionException("Unable to push git repostory: " + e.getMessage(), e);
		}
	}
}
