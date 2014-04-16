package ar.com.synergian.wagongit;

import org.apache.maven.scm.log.ScmLogger;

/**
 * @author mschonaker
 */
public class GitWagonLog implements ScmLogger {

	private boolean debug = false;

	public GitWagonLog() {
		// no op
	}

	public GitWagonLog(boolean debug) {
		this.debug = debug;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isDebugEnabled() {
		return this.debug;
	}

	/**
	 * {@inheritDoc}
	 */
	public void debug(String content) {
		debug(content, null);
	}

	/**
	 * {@inheritDoc}
	 */
	public void debug(String content, Throwable error) {
		if (this.debug) {
			System.out.println("[DEBUG] " + content);
			if (error != null)
				error.printStackTrace(System.out);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void debug(Throwable error) {
		debug("", error);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isInfoEnabled() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public void info(String content) {
		info(content, null);
	}

	/**
	 * {@inheritDoc}
	 */
	public void info(String content, Throwable error) {
		System.out.println("[INFO] " + content);
		if (error != null)
			error.printStackTrace(System.out);
	}

	/**
	 * {@inheritDoc}
	 */
	public void info(Throwable error) {
		info("", error);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isWarnEnabled() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public void warn(String content) {
		warn(content, null);
	}

	/**
	 * {@inheritDoc}
	 */
	public void warn(String content, Throwable error) {
		System.out.println("[WARNING] " + content);
		if (error != null)
			error.printStackTrace(System.out);
	}

	/**
	 * {@inheritDoc}
	 */
	public void warn(Throwable error) {
		warn("", error);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isErrorEnabled() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public void error(String content) {
		error(content, null);
	}

	/**
	 * {@inheritDoc}
	 */
	public void error(String content, Throwable error) {
		System.out.println("[ERROR] " + content);
		if (error != null)
			error.printStackTrace(System.out);
	}

	/**
	 * {@inheritDoc}
	 */
	public void error(Throwable error) {
		error("", error);
	}
}
