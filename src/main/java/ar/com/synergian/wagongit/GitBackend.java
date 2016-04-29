package ar.com.synergian.wagongit;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.log.ScmLogger;
import org.apache.maven.scm.provider.git.gitexe.command.GitCommandLineUtils;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils.StringStreamConsumer;
import org.codehaus.plexus.util.cli.Commandline;

public class GitBackend {

	final File workDir;
	private final String remote;
	private final String branch;

	private final ScmLogger log;

	private final StringStreamConsumer stdout = new StringStreamConsumer() {

		public void consumeLine(String line) {

			log.info("[git] " + line);

		}

	};

	private final StringStreamConsumer stderr = new StringStreamConsumer() {

		public void consumeLine(String line) {

			log.info("[git] " + line);

		}

	};

	public GitBackend(File workDir, String remote, String branch, ScmLogger log) throws GitException {
		this.log = log;
		this.remote = remote;
		this.branch = branch;
		this.workDir = workDir;
		if (!this.workDir.exists())
			throw new GitException("Invalid directory");

	}

	private boolean run(String command) throws GitException {
		return run(command, null);
	}

	private synchronized boolean run(String command, String[] args) throws GitException {

		try {

			Commandline cl = GitCommandLineUtils.getBaseGitCommandLine(workDir, command);

			if (args != null) {
				for (int i = 0; i < args.length; i++)
					cl.createArgument().setValue(args[i]);
			}

			StringBuffer sb = new StringBuffer();
			sb.append("git ").append(command);
			if (args != null) {
				for (int i = 0; i < args.length; i++)
					sb.append(" ").append(args[i]);
			}

			int exitCode = GitCommandLineUtils.execute(cl, stdout, stderr, log);

			log.debug("RAN: " + sb.toString() + " / $? = " + exitCode);

			return exitCode == 0;
		} catch (ScmException e) {
			throw new GitException("Couldn't run command " + command + ": " + e.getMessage(), e);
		}
	}

	private boolean isValidRepo() {

		// Where are assuming that this was checked out by this wagon.
		try {

			return new File(workDir, ".git").exists() && run("status");

		} catch (GitException e) {
			return false;
		}

	}

	public void pullAll() throws GitException {

		if (!workDir.exists())
			workDir.mkdirs();

		// if there a valid ".git" directory?
		if (!isValidRepo()) {

			if (!run("init"))
				throw new GitException("git init failed");

			if (!run("remote", new String[] { "add", "origin", remote })) {
				// Patch contributed by Alex Lin <opoo@users.sf.net>
				// Somehow git remote add failed on windows. Use set-url.
				log.warn("git remote add failed, try git remote set-url");
				if (!run("remote", new String[] { "set-url", "origin", remote })) {
					throw new GitException("git remote failed");
				}
			}

			if (!run("fetch", new String[] { "--progress" }))
				throw new GitException("git fetch failed");
		}

		// if remote branch doesn't exist, create new "headless".
		if (!run("show-ref", new String[] { "refs/remotes/origin/" + branch })) {

			// git symbolic-ref HEAD refs/heads/<branch>
			if (!run("symbolic-ref", new String[] { "HEAD", "refs/heads/" + branch }))
				throw new GitException("Unable to create branch");

			// rm .git/index
			File index = new File(workDir, ".git/index");
			if (index.exists())
				if (!index.delete())
					throw new GitException("Unable to create branch");

			// git clean -fdx

			if (!run("clean", new String[] { "-fdx" }))
				throw new GitException("Unable to create branch");
		} else
		// else if local branch doesn't exist, checkout -b
		if (!run("show-ref", new String[] { "refs/heads/" + branch })) {

			// git checkout -b <branch> origin/<branch>
			if (!run("checkout", new String[] { "-b", branch, "origin/" + branch }))
				throw new GitException("Unable to checkout branch");

		}
		// else checkout local branch.
		else {
			// git checkout <branch>
			if (!run("checkout", new String[] { branch }))
				throw new GitException("Unable to checkout branch");
		}

		// Fix contributed by kaleksandrov <kiril.aleksandrov.89@gmail.com>.
		// Sometimes someone else, using another computer, could be releasing.
		// In that case, our copy is correct, but the head is outdated: pull.
		// git pull origin <branch>
		// if (!run("pull", new String[] { "origin", branch }))
		// throw new GitException("Unable to pull latest changes");

		// Just don't fail if the remote branch doesn't exist.
		run("pull", new String[] { "origin", branch });
	}

	public void pushAll(boolean skipEmptyCommit) throws GitException {

		if (!run("add", new String[] { "." }))
			throw new GitException("Unable to add files");

		String timestamp = new SimpleDateFormat().format(new Date());

		if(skipEmptyCommit) {
			if (!run("commit", new String[] {"-m", "[wagon-git]" + " commit to branch " + branch + " " + timestamp }))
				throw new GitException("Unable to commit files");
		}
		else {
			if (!run("commit", new String[] { "--allow-empty", "-m", "[wagon-git]" + " commit to branch " + branch + " " + timestamp }))
				throw new GitException("Unable to commit files");
		}

		if (!run("push", new String[] { "--progress", "origin", branch }))
			throw new GitException("Unable to push files");

	}

	public void putDirectory(File sourceDirectory, String destinationDirectory) throws IOException {

		FileUtils.copyDirectoryStructure(sourceDirectory, new File(workDir, destinationDirectory));

	}
}
